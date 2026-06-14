package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Document
import com.example.data.model.DocumentVersion
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Stack

class OfficeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = DocumentRepository(database.documentDao())

    // App Preferences
    private val _isDarkMode = MutableStateFlow(true) // Start with modern Cosmic Dark
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // Navigation and UX State
    private val _selectedFolder = MutableStateFlow("All")
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Combined Flow for filtering documents by folder and search query
    val documentList: StateFlow<List<Document>> = combine(
        repository.allDocuments,
        _selectedFolder,
        _searchQuery
    ) { docs, folder, query ->
        var filtered = docs
        if (folder != "All") {
            filtered = filtered.filter { it.folder.equals(folder, ignoreCase = true) }
        }
        if (query.isNotEmpty()) {
            filtered = filtered.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.content.contains(query, ignoreCase = true) 
            }
        }
        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Recent documents (limit 5)
    val recentDocuments: StateFlow<List<Document>> = repository.allDocuments
        .map { it.take(5) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active screen navigation: "DASHBOARD", "DOC_EDITOR", "SHEET_EDITOR", "PDF_VIEWER", "PPT_VIEWER", "ABOUT"
    private val _activeScreen = MutableStateFlow("DASHBOARD")
    val activeScreen: StateFlow<String> = _activeScreen.asStateFlow()

    private val _activeDocument = MutableStateFlow<Document?>(null)
    val activeDocument: StateFlow<Document?> = _activeDocument.asStateFlow()

    // Undo / Redo Stacks for the active document editing
    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    // Version History
    private val _versionHistory = MutableStateFlow<List<DocumentVersion>>(emptyList())
    val versionHistory: StateFlow<List<DocumentVersion>> = _versionHistory.asStateFlow()

    fun selectFolder(folder: String) {
        _selectedFolder.value = folder
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun navigateTo(screen: String, doc: Document? = null) {
        _activeScreen.value = screen
        _activeDocument.value = doc
        undoStack.clear()
        redoStack.clear()

        if (doc != null) {
            loadVersionHistory(doc.id)
        }
    }

    private fun loadVersionHistory(documentId: Long) {
        viewModelScope.launch {
            repository.getVersionsForDocument(documentId).collect { versions ->
                _versionHistory.value = versions
            }
        }
    }

    // Create a new Document offline
    fun createDocument(name: String, type: String, folder: String = "Documents") {
        viewModelScope.launch {
            val initialContent = when (type) {
                "SHEET" -> "A1:;B1:;C1:;A2:;B2:;C2:;A3:;B3:;C3:" // Default Spreadsheet structure
                "PRESENTATION" -> "Slide 1 Title:Welcome to presentation;Slide 1 Body:Add details here;#FFFFFF|Slide 2 Title:Overview;Slide 2 Body:Points go here;#FFFFFF"
                "PDF" -> "DOCUMENT MOCKUP\n----\nThis is a generated PDF document ready for offline annotations and reviews.\n\nAuthor: Prince AR Abdur Rahman\n\nPage 1 Content:\nNexOffice Studio supports editing, annotation, highlighting, and custom PDF notes fully offline.\n\nPage 2 Content:\nAdd highlights by selecting the brush tool from the toolbar and drawing on any parts of this document."
                else -> "Welcome to NexOffice Word Document.\nStart typing your content here...\n\nYou can highlight, bold, italicize, align, or add tables easily."
            }

            val docName = if (name.endsWith(".$type", true)) name else {
                when (type) {
                    "DOC" -> "$name.docx"
                    "SHEET" -> "$name.xlsx"
                    "PRESENTATION" -> "$name.pptx"
                    "PDF" -> "$name.pdf"
                    else -> name
                }
            }

            val newDoc = Document(
                name = docName,
                type = type,
                content = initialContent,
                folder = folder
            )
            val id = repository.insertDocument(newDoc)
            val savedDoc = repository.getDocumentById(id)
            if (savedDoc != null) {
                navigateTo(
                    screen = when (type) {
                        "DOC" -> "DOC_EDITOR"
                        "SHEET" -> "SHEET_EDITOR"
                        "PRESENTATION" -> "PPT_VIEWER"
                        "PDF" -> "PDF_VIEWER"
                        else -> "DASHBOARD"
                    },
                    doc = savedDoc
                )
            }
        }
    }

    // Auto-save & Manual-save content with local version storage
    fun updateActiveDocumentContent(newContent: String, isAutoSave: Boolean = false) {
        val currentDoc = _activeDocument.value ?: return
        
        // Skip updating and histories if content remains identical
        if (currentDoc.content == newContent) return

        // Push to Undo stack
        undoStack.push(currentDoc.content)
        redoStack.clear()

        val updatedDoc = currentDoc.copy(
            content = newContent,
            lastSaved = System.currentTimeMillis()
        )
        _activeDocument.value = updatedDoc

        viewModelScope.launch {
            repository.updateDocument(updatedDoc)
            
            // Periodically create version states (e.g. on manual saves or major increments)
            if (!isAutoSave) {
                repository.insertVersion(
                    DocumentVersion(
                        documentId = currentDoc.id,
                        content = newContent,
                        description = "Saved Version - " + java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                    )
                )
            }
        }
    }

    // Save annotation point onto active PDF
    fun savePdfAnnotations(annotationsJson: String) {
        val currentDoc = _activeDocument.value ?: return
        if (currentDoc.type != "PDF") return
        viewModelScope.launch {
            val updated = currentDoc.copy(
                content = annotationsJson,
                lastSaved = System.currentTimeMillis()
            )
            _activeDocument.value = updated
            repository.updateDocument(updated)
        }
    }

    // Toggle document favorite status
    fun toggleFavorite(document: Document) {
        viewModelScope.launch {
            repository.updateDocument(document.copy(isFavorite = !document.isFavorite))
        }
    }

    // Move Document to specific category Folder
    fun moveDocumentToFolder(document: Document, newFolder: String) {
        viewModelScope.launch {
            repository.updateDocument(document.copy(folder = newFolder))
        }
    }

    // Delete document completely
    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            repository.deleteDocument(document)
            repository.deleteVersionsForDocument(document.id)
            if (_activeDocument.value?.id == document.id) {
                _activeDocument.value = null
                _activeScreen.value = "DASHBOARD"
            }
        }
    }

    // Undo system
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo() {
        if (!canUndo()) return
        val currentDoc = _activeDocument.value ?: return
        val previousState = undoStack.pop()
        redoStack.push(currentDoc.content)

        val updatedDoc = currentDoc.copy(content = previousState, lastSaved = System.currentTimeMillis())
        _activeDocument.value = updatedDoc
        viewModelScope.launch {
            repository.updateDocument(updatedDoc)
        }
    }

    fun redo() {
        if (!canRedo()) return
        val currentDoc = _activeDocument.value ?: return
        val nextState = redoStack.pop()
        undoStack.push(currentDoc.content)

        val updatedDoc = currentDoc.copy(content = nextState, lastSaved = System.currentTimeMillis())
        _activeDocument.value = updatedDoc
        viewModelScope.launch {
            repository.updateDocument(updatedDoc)
        }
    }

    // Restore from old version history
    fun restoreVersion(version: DocumentVersion) {
        val currentDoc = _activeDocument.value ?: return
        viewModelScope.launch {
            val restored = currentDoc.copy(content = version.content, lastSaved = System.currentTimeMillis())
            _activeDocument.value = restored
            repository.updateDocument(restored)
            undoStack.clear()
            redoStack.clear()
        }
    }
}
