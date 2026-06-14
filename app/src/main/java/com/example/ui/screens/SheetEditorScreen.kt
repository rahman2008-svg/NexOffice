package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.OfficeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetEditorScreen(
    viewModel: OfficeViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDocument.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val context = LocalContext.current

    if (activeDoc == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No spreadsheet loaded.", color = Color.Gray)
        }
        return
    }

    val doc = activeDoc!!
    var docName by remember(doc.id) { mutableStateOf(doc.name) }
    
    // Parse cells from doc.content string formatted as "A1:val;B1:val;C1:val"
    var cellMap by remember(doc.id) {
        mutableStateOf(parseCells(doc.content))
    }

    // Grid Dimensions: Columns A to J (10 columns), Rows 1 to 15
    val columns = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
    val rowsCount = 15

    var selectedCell by remember { mutableStateOf("A1") }
    var formulaInput by remember { mutableStateOf("") }
    
    // Maintain focus edit target
    LaunchedEffect(selectedCell) {
        val currentCellData = cellMap[selectedCell] ?: " "
        formulaInput = currentCellData
    }

    Scaffold(
        modifier = modifier.testTag("sheet_editor_root"),
        topBar = {
            TopAppBar(
                title = {
                    BasicTextField(
                        value = docName,
                        onValueChange = {
                            docName = it
                            viewModel.moveDocumentToFolder(doc.copy(name = it), doc.folder)
                        },
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            color = if (isDarkMode) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sheet_editor_title_field")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo("DASHBOARD") },
                        modifier = Modifier.testTag("sheet_editor_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = viewModel.canUndo()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (viewModel.canUndo()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = viewModel.canRedo()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (viewModel.canRedo()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = {
                            // Manual Export Sheet to Simulated XLS/HTML conversion
                            viewModel.createDocument(docName.replace(".xlsx", "_print.docx"), "DOC", doc.folder)
                            Toast.makeText(context, "Exported sheet layout to Word DOC successfully!", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Icon(Icons.Default.FileCopy, contentDescription = "Export to doc", tint = ExcelGreen)
                    }
                    IconButton(
                        onClick = {
                            val payload = serializeCells(cellMap)
                            viewModel.updateActiveDocumentContent(payload, isAutoSave = false)
                            Toast.makeText(context, "Spreadsheet manually saved!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("sheet_save_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Sheet", tint = Color.Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDarkMode) DarkSurface else Color.White)
            )
        },
        containerColor = if (isDarkMode) DarkBg else LightBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Excel-style active Formula Bar at the top
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) DarkSurface else Color.White
                ),
                border = BorderStroke(1.dp, if (isDarkMode) Color.DarkGray else Color.LightGray)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(ExcelGreen, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = selectedCell,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("fx", fontWeight = FontWeight.Bold, color = ExcelGreen, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    BasicTextField(
                        value = formulaInput,
                        onValueChange = { input ->
                            formulaInput = input
                            val updatedMap = cellMap.toMutableMap()
                            updatedMap[selectedCell] = input
                            cellMap = updatedMap
                            
                            // Passive auto-save state
                            viewModel.updateActiveDocumentContent(serializeCells(updatedMap), isAutoSave = true)
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = if (isDarkMode) Color.White else Color.Black,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_formula_input_field")
                    )
                }
            }

            // Quick Formula assist bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(if (isDarkMode) DarkCard else Color(0xFFF1F5F9))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = {
                    val targetForm = "=SUM(${selectedCell.first()}1:${selectedCell.first()}5)"
                    formulaInput = targetForm
                    val updatedMap = cellMap.toMutableMap()
                    updatedMap[selectedCell] = targetForm
                    cellMap = updatedMap
                    viewModel.updateActiveDocumentContent(serializeCells(updatedMap), isAutoSave = true)
                }) {
                    Text("SUM", color = ExcelGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                TextButton(onClick = {
                    val targetForm = "=AVG(${selectedCell.first()}1:${selectedCell.first()}5)"
                    formulaInput = targetForm
                    val updatedMap = cellMap.toMutableMap()
                    updatedMap[selectedCell] = targetForm
                    cellMap = updatedMap
                    viewModel.updateActiveDocumentContent(serializeCells(updatedMap), isAutoSave = true)
                }) {
                    Text("AVERAGE", color = ExcelGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                TextButton(onClick = {
                    val col = selectedCell.first().toString()
                    val targetForm = "=${col}1+${col}2"
                    formulaInput = targetForm
                    val updatedMap = cellMap.toMutableMap()
                    updatedMap[selectedCell] = targetForm
                    cellMap = updatedMap
                    viewModel.updateActiveDocumentContent(serializeCells(updatedMap), isAutoSave = true)
                }) {
                    Text("A1+B1 style", color = ExcelGreen, fontSize = 12.sp)
                }
                TextButton(onClick = {
                    val updatedMap = cellMap.toMutableMap()
                    updatedMap[selectedCell] = ""
                    cellMap = updatedMap
                    formulaInput = ""
                    viewModel.updateActiveDocumentContent(serializeCells(updatedMap), isAutoSave = true)
                }) {
                    Text("CLEAR CELL", color = Color.Red, fontSize = 12.sp)
                }
            }

            // Outer scroll for columns layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
            ) {
                // Spreadsheet body: Column guides + Cells lazy vertical grid
                Column(
                    modifier = Modifier.width(680.dp) // wide horizontal width fits columns safely
                ) {
                    // Columns headers row (including spacer cell on left top corner)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDarkMode) DarkCard else Color(0xFFE2E8F0))
                    ) {
                        // Corner block spacer
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(32.dp)
                                .border(0.5.dp, if (isDarkMode) Color.DarkGray else Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Apps, contentDescription = null, tint = ExcelGreen, modifier = Modifier.size(16.dp))
                        }

                        columns.forEach { col ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .border(0.5.dp, if (isDarkMode) Color.DarkGray else Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(col, fontWeight = FontWeight.Bold, color = ExcelGreen, fontSize = 12.sp)
                            }
                        }
                    }

                    // Rows layout
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        for (r in 1..rowsCount) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Left Side Row number Indicator cell
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(36.dp)
                                        .background(if (isDarkMode) DarkCard else Color(0xFFE2E8F0))
                                        .border(0.5.dp, if (isDarkMode) Color.DarkGray else Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        r.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isDarkMode) Color.LightGray else Color.DarkGray
                                    )
                                }

                                // Interactive Cells for columns
                                columns.forEach { col ->
                                    val cellCoord = "$col$r"
                                    val cellFormula = cellMap[cellCoord] ?: ""
                                    // Evaluate formulas if starts with '='
                                    val computedDisplay = if (cellFormula.startsWith("=")) {
                                        evaluateFormula(cellFormula, cellMap)
                                    } else {
                                        cellFormula
                                    }

                                    val isSelected = selectedCell == cellCoord

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .background(
                                                if (isSelected) {
                                                    ExcelGreen.copy(alpha = 0.2f)
                                                } else if (isDarkMode) {
                                                    DarkCard
                                                } else {
                                                    Color.White
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 0.5.dp,
                                                color = if (isSelected) ExcelGreen else if (isDarkMode) Color.DarkGray else Color.LightGray
                                            )
                                            .clickable {
                                                selectedCell = cellCoord
                                            }
                                            .testTag("sheet_cell_$cellCoord"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = computedDisplay,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isDarkMode) Color.White else Color.Black,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 4.dp)
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
}

// Convert serialized state string back into coordinate map
fun parseCells(content: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    if (content.isEmpty()) return map
    val pairs = content.split(";")
    for (pair in pairs) {
        val splitLoc = pair.indexOf(":")
        if (splitLoc != -1) {
            val cellCoord = pair.substring(0, splitLoc).trim()
            val cellVal = pair.substring(splitLoc + 1)
            map[cellCoord] = cellVal
        }
    }
    return map
}

// Save map state into serialized database string
fun serializeCells(map: Map<String, String>): String {
    return map.entries.joinToString(";") { "${it.key}:${it.value}" }
}

// Complex arithmetic and aggregation offline calculation parser
fun evaluateFormula(formula: String, cellMap: Map<String, String>): String {
    val cleanFormula = formula.removePrefix("=").trim().uppercase()
    try {
        if (cleanFormula.startsWith("SUM(")) {
            val range = cleanFormula.removePrefix("SUM(").removeSuffix(")")
            val cellValues = getCellRangeValues(range, cellMap)
            val sum = cellValues.sum()
            // Format to drop decimals if whole number
            return if (sum % 1 == 0f) sum.toInt().toString() else sum.toString()
        } else if (cleanFormula.startsWith("AVG(")) {
            val range = cleanFormula.removePrefix("AVG(").removeSuffix(")")
            val cellValues = getCellRangeValues(range, cellMap)
            if (cellValues.isEmpty()) return "0"
            val avg = cellValues.average()
            return String.format(java.util.Locale.US, "%.2f", avg)
        } else {
            // Evaluates single arithmetic addition: E.g., A1+B1 or A1-B1 or similar coordinates
            if (cleanFormula.contains("+")) {
                val parts = cleanFormula.split("+").map { it.trim() }
                if (parts.size == 2) {
                    val v1 = getSingleCellValue(parts[0], cellMap)
                    val v2 = getSingleCellValue(parts[1], cellMap)
                    val sum = v1 + v2
                    return if (sum % 1 == 0f) sum.toInt().toString() else sum.toString()
                }
            } else if (cleanFormula.contains("-")) {
                val parts = cleanFormula.split("-").map { it.trim() }
                if (parts.size == 2) {
                    val v1 = getSingleCellValue(parts[0], cellMap)
                    val v2 = getSingleCellValue(parts[1], cellMap)
                    val diff = v1 - v2
                    return if (diff % 1 == 0f) diff.toInt().toString() else diff.toString()
                }
            }
        }
    } catch (e: Exception) {
        return "#VALUE!"
    }
    return formula
}

fun getSingleCellValue(coord: String, cellMap: Map<String, String>): Float {
    val textVal = cellMap[coord] ?: return 0f
    if (textVal.startsWith("=")) {
        val evaluated = evaluateFormula(textVal, cellMap)
        return evaluated.toFloatOrNull() ?: 0f
    }
    return textVal.toFloatOrNull() ?: 0f
}

// Extracts cell ranges like SUM(A1:A5)
fun getCellRangeValues(range: String, cellMap: Map<String, String>): List<Float> {
    val parts = range.split(":")
    if (parts.size != 2) return emptyList()
    val start = parts[0]
    val end = parts[1]

    val startCol = start[0]
    val startRow = start.substring(1).toInt()
    val endCol = end[0]
    val endRow = end.substring(1).toInt()

    val values = mutableListOf<Float>()

    for (colChar in startCol..endCol) {
        for (rowNum in startRow..endRow) {
            val coord = "$colChar$rowNum"
            val textVal = cellMap[coord] ?: continue
            val numericVal = if (textVal.startsWith("=")) {
                evaluateFormula(textVal, cellMap).toFloatOrNull()
            } else {
                textVal.toFloatOrNull()
            }
            if (numericVal != null) {
                values.add(numericVal)
            }
        }
    }
    return values
}
