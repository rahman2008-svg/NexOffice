package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.OfficeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    viewModel: OfficeViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDocument.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val context = LocalContext.current

    if (activeDoc == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No loaded PDF file.", color = Color.Gray)
        }
        return
    }

    val doc = activeDoc!!
    var docName by remember(doc.id) { mutableStateOf(doc.name) }

    // Structure of PDF content and annotations
    // We can store content as normal lines and annotations as a discrete state mapped to paragraph index
    val rawText = doc.content
    val paragraphs = remember(rawText) {
        rawText.split("\n").filter { it.isNotEmpty() }
    }

    // Annotation memory state (maps paragraph Index to style / notes)
    // E.g., Map of "Index" -> AnnotationState(highlightColor, isUnderlined, stickyNoteText)
    var annotations by remember(doc.id) {
        mutableStateOf(mutableMapOf<Int, AnnotationData>())
    }

    var selectedParagraphIdx by remember { mutableStateOf(-1) }
    var activeTool by remember { mutableStateOf("SELECT") } // "SELECT", "HIGHLIGHT", "UNDERLINE", "NOTE"
    var selectedHighlightColor by remember { mutableStateOf("#FFEB3B") } // Amber Yellow

    var showNoteDialog by remember { mutableStateOf(false) }
    var stickyNoteInput by remember { mutableStateOf("") }
    
    // Dialog to read existing notes
    var selectedNoteForReading by remember { mutableStateOf<String?>(null) }

    val colorsHex = listOf("#FFEB3B", "#8BC34A", "#2196F3", "#E91E63", "#FF9800")

    Scaffold(
        modifier = modifier.testTag("pdf_viewer_root"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = docName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo("DASHBOARD") },
                        modifier = Modifier.testTag("pdf_viewer_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Empty existing local annotations
                            annotations = mutableMapOf()
                            Toast.makeText(context, "All annotations reset!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Annotations", tint = Color.LightGray)
                    }
                    IconButton(
                        onClick = {
                            // Serialize annotations and back up active state in DB content
                            // We use a safe local persistence format for storing annotations
                            val serialized = annotations.entries.joinToString(";") { 
                                "${it.key}|${it.value.color}|${it.value.isUnderlined}|${it.value.note}"
                            }
                            // Save onto DB
                            viewModel.updateActiveDocumentContent(rawText + "\n[ANNOTATIONS]\n" + serialized, isAutoSave = false)
                            Toast.makeText(context, "Annotations fully saved to PDF file offline!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("pdf_save_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save PDF", tint = Color.Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDarkMode) DarkSurface else Color.White)
            )
        },
        bottomBar = {
            // Annotations Tool Control Deck
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = if (isDarkMode) DarkSurface else Color.White,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "NexOffice PDF Annotator Deck",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Select tool
                        IconButton(
                            onClick = { activeTool = "SELECT" },
                            modifier = Modifier
                                .background(
                                    if (activeTool == "SELECT") AccentOrange.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("pdf_tool_select")
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = "Select Mode", tint = if (activeTool == "SELECT") AccentOrange else Color.Gray)
                        }

                        // Highlight tool
                        IconButton(
                            onClick = { activeTool = "HIGHLIGHT" },
                            modifier = Modifier
                                .background(
                                    if (activeTool == "HIGHLIGHT") AccentOrange.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("pdf_tool_highlight")
                        ) {
                            Icon(Icons.Default.BorderColor, contentDescription = "Highlight Mode", tint = if (activeTool == "HIGHLIGHT") AccentOrange else Color.Gray)
                        }

                        // Underline tool
                        IconButton(
                            onClick = { activeTool = "UNDERLINE" },
                            modifier = Modifier
                                .background(
                                    if (activeTool == "UNDERLINE") AccentOrange.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("pdf_tool_underline")
                        ) {
                            Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline Mode", tint = if (activeTool == "UNDERLINE") AccentOrange else Color.Gray)
                        }

                        // Note annotation tool
                        IconButton(
                            onClick = { activeTool = "NOTE" },
                            modifier = Modifier
                                .background(
                                    if (activeTool == "NOTE") AccentOrange.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("pdf_tool_note")
                        ) {
                            Icon(Icons.Default.StickyNote2, contentDescription = "Sticky Note Mode", tint = if (activeTool == "NOTE") AccentOrange else Color.Gray)
                        }
                    }

                    // Dynamic color options if Highlight is chosen
                    AnimatedVisibility(visible = activeTool == "HIGHLIGHT") {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            colorsHex.forEach { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(2.dp)
                                        .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                        .border(
                                            if (selectedHighlightColor == hex) 2.dp else 0.dp,
                                            if (isDarkMode) Color.White else Color.Black,
                                            CircleShape
                                        )
                                        .clickable { selectedHighlightColor = hex }
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = if (isDarkMode) DarkBg else Color(0xFFD3D6DC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PDF Page Simulation card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) DarkSurface else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Header Bar
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "PDF VIEW READER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PdfRed
                        )
                        Text(
                            "PAGE 1 OF 1",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Divider(color = PdfRed, thickness = 2.dp, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))

                    // PDF Interactive Content
                    paragraphs.forEachIndexed { idx, text ->
                        if (text.startsWith("[ANNOTATIONS]")) {
                            // Parse annotation lines if loading the file
                            return@forEachIndexed
                        }

                        val annotation = annotations[idx] ?: AnnotationData()
                        val hasNote = annotation.note.isNotEmpty()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedParagraphIdx = idx
                                    when (activeTool) {
                                        "HIGHLIGHT" -> {
                                            val updated = annotations.toMutableMap()
                                            updated[idx] = annotation.copy(color = selectedHighlightColor)
                                            annotations = updated
                                        }
                                        "UNDERLINE" -> {
                                            val updated = annotations.toMutableMap()
                                            updated[idx] = annotation.copy(isUnderlined = !annotation.isUnderlined)
                                            annotations = updated
                                        }
                                        "NOTE" -> {
                                            stickyNoteInput = annotation.note
                                            showNoteDialog = true
                                        }
                                        else -> {
                                            // SELECT mode, if has note allow reading it
                                            if (hasNote) {
                                                selectedNoteForReading = annotation.note
                                            }
                                        }
                                    }
                                }
                                .background(
                                    if (annotation.color.isNotEmpty()) {
                                        Color(android.graphics.Color.parseColor(annotation.color)).copy(alpha = 0.4f)
                                    } else {
                                        Color.Transparent
                                    },
                                    RoundedCornerShape(2.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Text block with conditional underline styling
                            Text(
                                text = text,
                                style = TextStyle(
                                    textDecoration = if (annotation.isUnderlined) TextDecoration.Underline else TextDecoration.None,
                                    color = if (isDarkMode) Color.LightGray else Color.Black,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            // Sticky note visual overlay indicator
                            if (hasNote) {
                                Icon(
                                    imageVector = Icons.Default.StickyNote2,
                                    contentDescription = "Read Sticky Note",
                                    tint = AccentOrange,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(start = 4.dp)
                                        .clickable {
                                            selectedNoteForReading = annotation.note
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG TO WRITE STICKY NOTE
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Add PDF Annotation Note") },
            text = {
                OutlinedTextField(
                    value = stickyNoteInput,
                    onValueChange = { stickyNoteInput = it },
                    placeholder = { Text("Write your sticky annotation here...") },
                    modifier = Modifier.fillMaxWidth().testTag("pdf_sticky_note_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentAnn = annotations[selectedParagraphIdx] ?: AnnotationData()
                        val updated = annotations.toMutableMap()
                        updated[selectedParagraphIdx] = currentAnn.copy(note = stickyNoteInput)
                        annotations = updated
                        showNoteDialog = false
                        Toast.makeText(context, "Sticky annotation added!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Attach Annotation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = if (isDarkMode) DarkSurface else Color.White
        )
    }

    // READ NOTE DIALOG
    if (selectedNoteForReading != null) {
        AlertDialog(
            onDismissRequest = { selectedNoteForReading = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.StickyNote2, contentDescription = null, tint = AccentOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Attached Annotation Note", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = selectedNoteForReading!!,
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color.LightGray else Color.DarkGray
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedNoteForReading = null }) {
                    Text("Close")
                }
            },
            containerColor = if (isDarkMode) DarkSurface else Color.White
        )
    }
}

data class AnnotationData(
    val color: String = "",
    val isUnderlined: Boolean = false,
    val note: String = ""
)
