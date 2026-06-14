package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LightBg
import com.example.ui.viewmodel.OfficeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocEditorScreen(
    viewModel: OfficeViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDocument.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val versions by viewModel.versionHistory.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (activeDoc == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No document details loaded.", color = Color.Gray)
        }
        return
    }

    val doc = activeDoc!!
    var docName by remember(doc.id) { mutableStateOf(doc.name) }
    var contentText by remember(doc.id) { mutableStateOf(doc.content) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Edit, 1: Live Page Render

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showTableInsertDialog by remember { mutableStateOf(false) }
    var fontSizeSelected by remember { mutableStateOf(16) }
    var fontStyleSelected by remember { mutableStateOf(FontFamily.Default) }

    // Table parameters
    var tableRows by remember { mutableStateOf("3") }
    var tableCols by remember { mutableStateOf("3") }

    // Formatting Toolbar helpers
    val insertStylingTag: (String, String) -> Unit = { prefix, suffix ->
        val textVal = contentText
        // Insert formatting markings at current text body
        contentText = "$textVal$prefix Text $suffix"
        viewModel.updateActiveDocumentContent(contentText, isAutoSave = true)
    }

    Scaffold(
        modifier = modifier.testTag("doc_editor_root"),
        topBar = {
            TopAppBar(
                title = {
                    BasicTextField(
                        value = docName,
                        onValueChange = {
                            docName = it
                            // Debounce or immediate rename
                            scope.launch {
                                viewModel.moveDocumentToFolder(doc.copy(name = it), doc.folder)
                            }
                        },
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            color = if (isDarkMode) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("doc_editor_title_field")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo("DASHBOARD") },
                        modifier = Modifier.testTag("doc_editor_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = viewModel.canUndo(),
                        modifier = Modifier.testTag("doc_undo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (viewModel.canUndo()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = viewModel.canRedo(),
                        modifier = Modifier.testTag("doc_redo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (viewModel.canRedo()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("doc_history_btn")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Version History", tint = AccentOrange)
                    }
                    IconButton(
                        onClick = {
                            // Manual Export Doc to simulated PDF file
                            val pdfName = if (docName.endsWith(".docx")) {
                                docName.replace(".docx", ".pdf")
                            } else {
                                "$docName.pdf"
                            }
                            viewModel.createDocument(pdfName, "PDF", doc.folder)
                            Toast.makeText(context, "Exported Doc successfully to PDF: $pdfName", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.testTag("doc_pdf_export_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export to PDF", tint = Color.Red)
                    }
                    IconButton(
                        onClick = {
                            viewModel.updateActiveDocumentContent(contentText, isAutoSave = false)
                            Toast.makeText(context, "Document manual point saved!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("doc_save_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Manual Save", tint = Color.Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) DarkSurface else Color.White
                )
            )
        },
        containerColor = if (isDarkMode) DarkBg else LightBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Document View Mode Tabs (Edit View vs Print Preview)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = if (isDarkMode) DarkSurface else Color.White,
                contentColor = AccentOrange
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Drafting Board") },
                    modifier = Modifier.testTag("tab_edit_view")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Live Print Preview") },
                    modifier = Modifier.testTag("tab_print_preview")
                )
            }

            if (selectedTab == 0) {
                // Formatting helper bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDarkMode) DarkCard else Color(0xFFECEFF4))
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { insertStylingTag("**", "**") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { insertStylingTag("*", "*") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { insertStylingTag("_", "_") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline", modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { showTableInsertDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = "Add Table", modifier = Modifier.size(20.dp))
                    }

                    VerticalDivider(color = Color.Gray, modifier = Modifier.height(20.dp))

                    Text("Font Size: ${fontSizeSelected}sp", fontSize = 12.sp)
                    IconButton(onClick = { if (fontSizeSelected > 12) fontSizeSelected -= 2 }) {
                        Icon(Icons.Default.Remove, contentDescription = "Reduce text size", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { if (fontSizeSelected < 32) fontSizeSelected += 2 }) {
                        Icon(Icons.Default.Add, contentDescription = "Raise text size", modifier = Modifier.size(16.dp))
                    }

                    VerticalDivider(color = Color.Gray, modifier = Modifier.height(20.dp))

                    Text("Family:", fontSize = 12.sp)
                    TextButton(onClick = {
                        fontStyleSelected = when (fontStyleSelected) {
                            FontFamily.Default -> FontFamily.Serif
                            FontFamily.Serif -> FontFamily.Monospace
                            else -> FontFamily.Default
                        }
                    }) {
                        Text(
                            text = when (fontStyleSelected) {
                                FontFamily.Serif -> "Serif"
                                FontFamily.Monospace -> "Mono"
                                else -> "Default"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Keyboard entry area
                OutlinedTextField(
                    value = contentText,
                    onValueChange = {
                        contentText = it
                        // Auto-save silently
                        viewModel.updateActiveDocumentContent(it, isAutoSave = true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .testTag("doc_body_editor_field"),
                    textStyle = TextStyle(
                        fontFamily = fontStyleSelected,
                        fontSize = fontSizeSelected.sp,
                        color = if (isDarkMode) Color.White else Color.Black
                    ),
                    placeholder = { Text("Draft your text here... Tip: use # for H1 headers, ## for H2, and ** for bold words.") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = if (isDarkMode) DarkSurface else Color.White,
                        unfocusedContainerColor = if (isDarkMode) DarkSurface else Color.White
                    )
                )
            } else {
                // PAGE SIMULATOR RENDER
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDarkMode) DarkBg else Color(0xFFDCDFE4))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(bottom = 32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) DarkSurface else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(2.dp) // Professional sheets edge Look
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            // Title Header
                            Text(
                                text = docName.removeSuffix(".docx"),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color.Black,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Divider(color = AccentOrange, thickness = 2.dp, modifier = Modifier.padding(bottom = 20.dp))

                            // Custom Parsed Content UI Blocks
                            val blocks = parseDocContent(contentText)
                            blocks.forEach { block ->
                                when (block) {
                                    is DocBlock.Heading1 -> {
                                        Text(
                                            text = block.text,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentOrange,
                                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                        )
                                    }
                                    is DocBlock.Heading2 -> {
                                        Text(
                                            text = block.text,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color.LightGray else Color.DarkGray,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                                        )
                                    }
                                    is DocBlock.Table -> {
                                        RenderVisualTable(block.rows, isDarkMode)
                                    }
                                    is DocBlock.Paragraph -> {
                                        Text(
                                            text = renderFormattedSpans(block.text),
                                            fontSize = fontSizeSelected.sp,
                                            fontFamily = fontStyleSelected,
                                            lineHeight = (fontSizeSelected + 6).sp,
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            color = if (isDarkMode) Color.LightGray else Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // TABLE CREATION BUILD DIALOG
    if (showTableInsertDialog) {
        AlertDialog(
            onDismissRequest = { showTableInsertDialog = false },
            title = { Text("Configure Custom Table") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Insert offline layout cells into the text flow.", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = tableRows,
                        onValueChange = { tableRows = it },
                        label = { Text("Number of Rows") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange)
                    )
                    OutlinedTextField(
                        value = tableCols,
                        onValueChange = { tableCols = it },
                        label = { Text("Number of Columns") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rs = tableRows.toIntOrNull() ?: 3
                        val cs = tableCols.toIntOrNull() ?: 3
                        // Format table pattern: | Cell 1 | Cell 2 | \n
                        val tableMarkdown = StringBuilder("\n")
                        for (r in 0 until rs) {
                            tableMarkdown.append("|")
                            for (c in 0 until cs) {
                                tableMarkdown.append(" Cell ${r+1},${c+1} |")
                            }
                            tableMarkdown.append("\n")
                        }
                        contentText = "$contentText\n$tableMarkdown\n"
                        viewModel.updateActiveDocumentContent(contentText, isAutoSave = true)
                        showTableInsertDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Insert Table")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTableInsertDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // VERSION HISTORY ROLLBACK ACTION INFO
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Local Version Checkpoints", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Text("Select any previous auto-save point to restore instantly.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (versions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No checkpoints logged yet.\nMake substantial additions to auto-log versions.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(versions) { ver ->
                                Card(
                                    onClick = {
                                        viewModel.restoreVersion(ver)
                                        contentText = ver.content
                                        showHistoryDialog = false
                                        Toast.makeText(context, "Restored to point saved at: ${ver.description}", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDarkMode) DarkCard else Color(0xFFF1F5F9)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Backup, contentDescription = "Rollback", tint = AccentOrange)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(ver.description, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text("Characters size: ${ver.content.length}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.Restore, contentDescription = "Restore", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close")
                }
            },
            containerColor = if (isDarkMode) DarkSurface else Color.White
        )
    }
}

// Custom parser entities of NexOffice DOC format
sealed class DocBlock {
    data class Heading1(val text: String) : DocBlock()
    data class Heading2(val text: String) : DocBlock()
    data class Table(val rows: List<List<String>>) : DocBlock()
    data class Paragraph(val text: String) : DocBlock()
}

fun parseDocContent(content: String): List<DocBlock> {
    val lines = content.split("\n")
    val blocks = mutableListOf<DocBlock>()

    var activeTableRows = mutableListOf<List<String>>()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            // Table row parse
            val cells = trimmed.split("|")
                .map { it.trim() }
                .filterIndexed { idx, _ -> idx > 0 } // drop leading empty split
            // Filter last empty element if ends with |
            val filteredCells = if (cells.isNotEmpty() && cells.last().isEmpty()) cells.dropLast(1) else cells
            if (filteredCells.isNotEmpty()) {
                activeTableRows.add(filteredCells)
            }
        } else {
            // If we were processing a table and it ended, flush it
            if (activeTableRows.isNotEmpty()) {
                blocks.add(DocBlock.Table(activeTableRows.toList()))
                activeTableRows = mutableListOf()
            }

            if (trimmed.startsWith("# ")) {
                blocks.add(DocBlock.Heading1(trimmed.removePrefix("# ").trim()))
            } else if (trimmed.startsWith("## ")) {
                blocks.add(DocBlock.Heading2(trimmed.removePrefix("## ").trim()))
            } else if (trimmed.isNotEmpty()) {
                blocks.add(DocBlock.Paragraph(line)) // Keep indentation spaces
            }
        }
    }

    // Flush final table if open
    if (activeTableRows.isNotEmpty()) {
        blocks.add(DocBlock.Table(activeTableRows.toList()))
    }

    return blocks
}

// Substring bold/italic formatter
@Composable
fun renderFormattedSpans(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val len = text.length

        while (cursor < len) {
            val boldIdx = text.indexOf("**", cursor)
            val italicIdx = text.indexOf("*", cursor)
            val underIdx = text.indexOf("_", cursor)

            // Find closest token
            val nextTokenIdx = listOf(
                if (boldIdx != -1) boldIdx else Int.MAX_VALUE,
                if (italicIdx != -1 && italicIdx != boldIdx) italicIdx else Int.MAX_VALUE, // * vs ** check
                if (underIdx != -1) underIdx else Int.MAX_VALUE
            ).minOrNull() ?: Int.MAX_VALUE

            if (nextTokenIdx == Int.MAX_VALUE) {
                // No more spans matching, append remaining
                append(text.substring(cursor))
                break
            }

            // Append leading text
            append(text.substring(cursor, nextTokenIdx))
            cursor = nextTokenIdx

            if (cursor == boldIdx) {
                val endBold = text.indexOf("**", cursor + 2)
                if (endBold != -1) {
                    val boldText = text.substring(cursor + 2, endBold)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(boldText)
                    pop()
                    cursor = endBold + 2
                } else {
                    append("**")
                    cursor += 2
                }
            } else if (cursor == italicIdx) {
                val endItalic = text.indexOf("*", cursor + 1)
                if (endItalic != -1) {
                    val italicText = text.substring(cursor + 1, endItalic)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(italicText)
                    pop()
                    cursor = endItalic + 1
                } else {
                    append("*")
                    cursor += 1
                }
            } else if (cursor == underIdx) {
                val endUnder = text.indexOf("_", cursor + 1)
                if (endUnder != -1) {
                    val underText = text.substring(cursor + 1, endUnder)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    append(underText)
                    pop()
                    cursor = endUnder + 1
                } else {
                    append("_")
                    cursor += 1
                }
            }
        }
    }
}

// Aesthetic table rendering inside printed page document simulation card
@Composable
fun RenderVisualTable(rows: List<List<String>>, isDarkMode: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(
                1.dp,
                if (isDarkMode) Color.DarkGray else Color.LightGray,
                RoundedCornerShape(4.dp)
            )
    ) {
        rows.forEachIndexed { rIdx, cols ->
            val isHeader = rIdx == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isHeader) {
                            if (isDarkMode) Color(0xFF222634) else Color(0xFFECEFF4)
                        } else {
                            Color.Transparent
                        }
                    )
            ) {
                cols.forEachIndexed { cIdx, cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                0.5.dp,
                                if (isDarkMode) Color.DarkGray else Color.LightGray
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cell,
                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            color = if (isDarkMode) Color.LightGray else Color.Black,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
