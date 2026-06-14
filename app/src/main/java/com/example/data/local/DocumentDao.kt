package com.example.data.local

import androidx.room.*
import com.example.data.model.Document
import com.example.data.model.DocumentVersion
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY lastSaved DESC")
    fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE folder = :folder ORDER BY lastSaved DESC")
    fun getDocumentsByFolder(folder: String): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): Document?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document): Long

    @Update
    suspend fun updateDocument(document: Document)

    @Delete
    suspend fun deleteDocument(document: Document)

    @Query("SELECT * FROM documents WHERE name LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY lastSaved DESC")
    fun searchDocuments(query: String): Flow<List<Document>>

    // Version History support
    @Query("SELECT * FROM document_versions WHERE documentId = :documentId ORDER BY savedAt DESC")
    fun getVersionsForDocument(documentId: Long): Flow<List<DocumentVersion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: DocumentVersion)

    @Query("DELETE FROM document_versions WHERE documentId = :documentId")
    suspend fun deleteVersionsForDocument(documentId: Long)
}
