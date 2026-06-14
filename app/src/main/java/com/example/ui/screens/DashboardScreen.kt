package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Document
import com.example.ui.theme.*
import com.example.ui.viewmodel.OfficeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: OfficeViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.documentList.collectAsState()
    val recents by viewModel.recentDocuments.collectAsState()
    val currentFolder by viewModel.selectedFolder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var createFileType by remember { mutableStateOf("DOC") }
    var showAboutSheet by remember { mutableStateOf(false) }
    var selectedDocForActions by remember { mutableStateOf<Document?>(null) }
    var showMoveFolderDialog by remember { mutableStateOf(false) }

    // Categories
    val folders = listOf("All", "Documents", "Personal", "Work", "Drafts", "Finance")

    Scaffold(
        modifier = modifier.testTag("dashboard_root"),
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "V1.0.0",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = if (isDarkMode) Color.Gray else Color(0xFF94A3B8)
                                )
                                // Offline Storage Badge matching Tailwind px-2 py-0.5 rounded-full border border-green-200
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(
                                            color = if (isDarkMode) Color(0xFF1B3D2B) else Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isDarkMode) Color(0xFF2E6F4A) else Color(0xFFC8E6C9),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(Color(0xFF4CAF50), CircleShape)
                                    )
                                    Text(
                                        "OFFLINE STORAGE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = buildAnnotatedString {
                                    append("NexOffice ")
                                    withStyle(
                                        SpanStyle(color = Color(0xFF0061A4))
                                    ) {
                                        append("Studio")
                                    }
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (isDarkMode) Color.White else Color(0xFF1C1B1F)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = if (isDarkMode) Color(0xFFFFCC00) else Color(0xFF555555)
                        )
                    }
                    // Profile initials avatar 'PA' from Tailwind template
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD1E4FF))
                            .border(1.dp, Color(0xFF0061A4).copy(alpha = 0.15f), CircleShape)
                            .clickable { showAboutSheet = true }
                            .testTag("about_info_btn_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "PA",
                            color = Color(0xFF001D36),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = if (isDarkMode) DarkBg else LightBg
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    createFileType = "DOC"
                    showCreateDialog = true 
                },
                containerColor = AccentOrange,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_doc_fab").padding(bottom = 8.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Document", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = if (isDarkMode) DarkBg else LightBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_bar_input"),
                placeholder = { Text("Search documents by name or content...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isDarkMode) DarkSurface else Color.White,
                    unfocusedContainerColor = if (isDarkMode) DarkSurface else Color.White,
                    focusedBorderColor = Color(0xFF0061A4),
                    unfocusedBorderColor = if (isDarkMode) Color(0xFF2C3140) else Color(0xFFCBD2E1)
                )
            )

            // Bento Grid Action Cards (From Tailwind HTML Spec)
            if (searchQuery.isEmpty()) {
                Text(
                    text = "Quick Creation Studio",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp),
                    color = if (isDarkMode) Color.LightGray else Color.DarkGray
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoActionCard(
                            title = "Word Doc",
                            icon = Icons.Default.Description,
                            badgeColor = Color(0xFF2196F3),
                            bgColor = if (isDarkMode) Color(0xFF132235) else Color(0xFFE3F2FD),
                            borderColor = if (isDarkMode) Color(0xFF1A3353) else Color(0xFFBBDEFB),
                            onClick = {
                                createFileType = "DOC"
                                showCreateDialog = true
                            },
                            modifier = Modifier.weight(1f).testTag("bento_create_doc")
                        )
                        BentoActionCard(
                            title = "Spreadsheet",
                            icon = Icons.Default.GridView,
                            badgeColor = Color(0xFF4CAF50),
                            bgColor = if (isDarkMode) Color(0xFF102816) else Color(0xFFE8F5E9),
                            borderColor = if (isDarkMode) Color(0xFF183D21) else Color(0xFFC8E6C9),
                            onClick = {
                                createFileType = "SHEET"
                                showCreateDialog = true
                            },
                            modifier = Modifier.weight(1f).testTag("bento_create_sheet")
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoActionCard(
                            title = "Slides",
                            icon = Icons.Default.CoPresent,
                            badgeColor = Color(0xFFFF9800),
                            bgColor = if (isDarkMode) Color(0xFF2E200C) else Color(0xFFFFF3E0),
                            borderColor = if (isDarkMode) Color(0xFF452E0F) else Color(0xFFFFE0B2),
                            onClick = {
                                createFileType = "PRESENTATION"
                                showCreateDialog = true
                            },
                            modifier = Modifier.weight(1f).testTag("bento_create_presentation")
                        )
                        BentoActionCard(
                            title = "PDF Tools",
                            icon = Icons.Default.PictureAsPdf,
                            badgeColor = Color(0xFFF44336),
                            bgColor = if (isDarkMode) Color(0xFF331317) else Color(0xFFFFEBEE),
                            borderColor = if (isDarkMode) Color(0xFF4D1C22) else Color(0xFFFFCDD2),
                            onClick = {
                                createFileType = "PDF"
                                showCreateDialog = true
                            },
                            modifier = Modifier.weight(1f).testTag("bento_create_pdf")
                        )
                    }
                }
            }

            // Dynamic Folder Selection Layout
            Text(
                text = "Document Folders",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                color = if (isDarkMode) Color.LightGray else Color.DarkGray
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders) { folder ->
                    val isSelected = currentFolder == folder
                    InputChip(
                        selected = isSelected,
                        onClick = { viewModel.selectFolder(folder) },
                        label = { Text(folder) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (folder == "All") Icons.AutoMirrored.Filled.List else Icons.Default.Folder,
                                contentDescription = folder,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = if (isDarkMode) DarkSurface else Color.White,
                            selectedContainerColor = AccentOrange,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            labelColor = if (isDarkMode) Color.LightGray else Color.DarkGray,
                            leadingIconColor = if (isDarkMode) Color.Gray else Color.DarkGray
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) AccentOrange else if (isDarkMode) Color(0xFF2C3140) else Color(0xFFCBD2E1)
                        ),
                        modifier = Modifier.testTag("folder_chip_$folder")
                    )
                }
            }

            // Recent Files Carousel (Only show if all files are selected and query is empty)
            if (currentFolder == "All" && searchQuery.isEmpty()) {
                AnimatedVisibility(
                    visible = recents.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Text(
                            text = "Recent Handled Files",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
                            color = if (isDarkMode) Color.LightGray else Color.DarkGray
                        )
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(recents) { doc ->
                                RecentFileCard(
                                    doc = doc,
                                    isDarkMode = isDarkMode,
                                    onClick = { viewModel.navigateTo(getScreenIdForType(doc.type), doc) }
                                )
                            }
                        }
                    }
                }
            }

            // Documents Lists
            Text(
                text = if (searchQuery.isNotEmpty()) "Search Results" else "All Offline Documents (${documents.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp),
                color = if (isDarkMode) Color.LightGray else Color.DarkGray
            )

            if (documents.isEmpty()) {
                // Empty view element
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = "Empty list icon",
                        tint = if (isDarkMode) Color(0xFF2C3140) else Color(0xFFCBD2E1),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No searching matched files" else "No documents created yet",
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color.Gray else Color.DarkGray
                    )
                    Text(
                        text = "Tap the '+' floating button below to create and edit documents, sheets, slides, or annotation PDFs offline.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
                    )
                }
            } else {
                NonScrollableDocumentList(
                    items = documents,
                    isDarkMode = isDarkMode,
                    onDocClick = { viewModel.navigateTo(getScreenIdForType(it.type), it) },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onActionMenuClick = { selectedDocForActions = it }
                )
            }

            // Disclaimer & Dev footer details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "NexOffice Studio v1.0.0",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.DarkGray else Color.Gray
                    )
                    Text(
                        "© 2026 NexVora Lab's Ofc. All Rights Reserved.",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    // CREATE DOCUMENT DIALOG
    if (showCreateDialog) {
        CreateFileDialog(
            isDarkMode = isDarkMode,
            initialType = createFileType,
            onDismiss = { showCreateDialog = false },
            onCreate = { filename, filetype, category ->
                viewModel.createDocument(filename, filetype, category)
                showCreateDialog = false
            }
        )
    }

    // ACTIONS SHEET (BOTTOM SHEET OR ACCESSIBLE DIALOG FOR LOW-END DEVICES)
    if (selectedDocForActions != null) {
        val doc = selectedDocForActions!!
        AlertDialog(
            onDismissRequest = { selectedDocForActions = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getIconForType(doc.type),
                        contentDescription = doc.type,
                        tint = getColorForType(doc.type),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(doc.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Type: ${doc.type} File", fontSize = 12.sp, color = Color.Gray)
                    Text("Saved: ${SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(doc.lastSaved))}", fontSize = 12.sp, color = Color.Gray)
                    Text("Folder: ${doc.folder}", fontSize = 13.sp, color = AccentOrange, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Divider(color = if (isDarkMode) Color.DarkGray else Color.LightGray)
                    
                    TextButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, doc.name)
                                putExtra(Intent.EXTRA_TEXT, "Shared via NexOffice Studio: \n\nTitle: ${doc.name}\n\nContent:\n${doc.content}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Document via:"))
                            selectedDocForActions = null
                        },
                        modifier = Modifier.fillMaxWidth().align(Alignment.Start)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Share via Android Intent", color = if (isDarkMode) Color.White else Color.Black)
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    TextButton(
                        onClick = {
                            showMoveFolderDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().align(Alignment.Start)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Move Folder", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Change Category Folder", color = if (isDarkMode) Color.White else Color.Black)
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    TextButton(
                        onClick = {
                            viewModel.deleteDocument(doc)
                            selectedDocForActions = null
                        },
                        modifier = Modifier.fillMaxWidth().align(Alignment.Start)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Delete Document", color = Color.Red)
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDocForActions = null }) {
                    Text("Close")
                }
            },
            containerColor = if (isDarkMode) DarkSurface else Color.White
        )
    }

    // MOVE FOLDER DIALOG
    if (showMoveFolderDialog && selectedDocForActions != null) {
        val doc = selectedDocForActions!!
        val folderOptions = listOf("Documents", "Personal", "Work", "Drafts", "Finance")

        AlertDialog(
            onDismissRequest = { showMoveFolderDialog = false },
            title = { Text("Move '${doc.name}' to:") },
            text = {
                Column {
                    folderOptions.forEach { folderOpt ->
                        TextButton(
                            onClick = {
                                viewModel.moveDocumentToFolder(doc, folderOpt)
                                showMoveFolderDialog = false
                                selectedDocForActions = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, contentDescription = folderOpt, tint = AccentOrange)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(folderOpt, color = if (isDarkMode) Color.White else Color.Black)
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveFolderDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = if (isDarkMode) DarkSurface else Color.White
        )
    }

    // ABOUT DEVELOPER & PRODUCTIVITY INFORMATION PANEL
    if (showAboutSheet) {
        AlertDialog(
            onDismissRequest = { showAboutSheet = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(AccentOrange, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Workspaces, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("NexOffice Studio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("Version 1.0.0", fontSize = 12.sp, color = Color.Gray)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Divider(color = if (isDarkMode) Color.DarkGray else Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("SUITE SPECIFICATIONS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AccentOrange)
                    Text(
                        "NexOffice Studio is a high-performance, lightweight office productivity platform built from the ground up for completely offline execution. It delivers complete offline word document processing, structured calculation worksheets, multi-slide design layouts, and robust PDF annotation mechanics directly inside a local Room sandbox.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("DEVELOPER PROFILE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AccentOrange)
                    Text("Prince AR Abdur Rahman", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "Independent Android Architect creating fast, highly secure, lightweight digital tools and modern enterprise solutions.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("PUBLISHING LAB", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AccentOrange)
                    Text("NexVora Lab's Ofc", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "Mission: Building beautifully tailored and lighting-fast localized core utilities for absolute productivity, ensuring zero data leakage.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = if (isDarkMode) Color.DarkGray else Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "© 2026 NexVora Lab's Ofc. All Rights Reserved. Fully certified for low-end device hardware.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Close Panel")
                }
            },
            containerColor = if (isDarkMode) DarkSurface else Color.White
        )
    }
}

@Composable
fun RecentFileCard(
    doc: Document,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(150.dp)
            .height(110.dp)
            .testTag("recent_file_card_${doc.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) DarkSurface else Color.White
        ),
        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2C3140) else Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(getColorForType(doc.type).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForType(doc.type),
                        contentDescription = doc.type,
                        tint = getColorForType(doc.type),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                if (doc.isFavorite) {
                    Icon(Icons.Default.Star, contentDescription = "Favorite", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                }
            }
            
            Column {
                Text(
                    text = doc.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Text(
                    text = SimpleDateFormat("HH:mm, MMM dd", Locale.getDefault()).format(Date(doc.lastSaved)),
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun NonScrollableDocumentList(
    items: List<Document>,
    isDarkMode: Boolean,
    onDocClick: (Document) -> Unit,
    onFavoriteToggle: (Document) -> Unit,
    onActionMenuClick: (Document) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { doc ->
            Card(
                onClick = { onDocClick(doc) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("document_list_card_${doc.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) DarkSurface else Color.White
                ),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2C3140) else Color(0xFFECEFF4)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(getColorForType(doc.type).copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForType(doc.type),
                            contentDescription = doc.type,
                            tint = getColorForType(doc.type),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isDarkMode) Color.White else Color(0xFF1C1B1F)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = doc.folder,
                                color = AccentOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "•",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(doc.lastSaved)),
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { onFavoriteToggle(doc) },
                        modifier = Modifier.testTag("doc_favorite_btn_${doc.id}")
                    ) {
                        Icon(
                            imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (doc.isFavorite) Color(0xFFFFB300) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onActionMenuClick(doc) },
                        modifier = Modifier.testTag("doc_more_actions_btn_${doc.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Actions",
                            tint = if (isDarkMode) Color.LightGray else Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

// Dialog to create a file
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFileDialog(
    isDarkMode: Boolean,
    initialType: String = "DOC",
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var filename by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedFolder by remember { mutableStateOf("Documents") }

    val types = listOf(
        Triple("DOC", "Word Document", WordBlue),
        Triple("SHEET", "Spreadsheet", ExcelGreen),
        Triple("PRESENTATION", "Presentation", PresentationOrange),
        Triple("PDF", "PDF Document", PdfRed)
    )

    val foldersList = listOf("Documents", "Personal", "Work", "Drafts", "Finance")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Document", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text("File Name") },
                    placeholder = { Text("e.g. Project Proposal") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = if (isDarkMode) Color.DarkGray else Color.LightGray
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_file_name_field")
                )

                Text("File Type", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentOrange)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    types.forEach { (typeKey, typeLabel, color) ->
                        val isSel = selectedType == typeKey
                        Card(
                            onClick = { selectedType = typeKey },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("type_selection_$typeKey"),
                            border = BorderStroke(2.dp, if (isSel) color else Color.Transparent),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) color.copy(alpha = 0.1f) else if (isDarkMode) DarkCard else Color(0xFFF1F5F9)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(getIconForType(typeKey), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = typeLabel,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isDarkMode) Color.White else Color.Black
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (isSel) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = color)
                                }
                            }
                        }
                    }
                }

                Text("Folder Location", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentOrange)
                var expandedFolders by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { expandedFolders = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) DarkCard else Color(0xFFECEFF4),
                            contentColor = if (isDarkMode) Color.White else Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = AccentOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedFolder)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expandedFolders,
                        onDismissRequest = { expandedFolders = false },
                        modifier = Modifier.background(if (isDarkMode) DarkSurface else Color.White)
                    ) {
                        foldersList.forEach { fol ->
                            DropdownMenuItem(
                                text = { Text(fol) },
                                onClick = {
                                    selectedFolder = fol
                                    expandedFolders = false
                                },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = AccentOrange) }
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (filename.isNotBlank()) {
                        onCreate(filename, selectedType, selectedFolder)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                enabled = filename.isNotBlank(),
                modifier = Modifier.testTag("add_file_submit_btn")
            ) {
                Text("Create file", color = Color.White)
            }
        },
        containerColor = if (isDarkMode) DarkSurface else Color.White
    )
}

// Helpers
fun getIconForType(type: String): ImageVector {
    return when (type) {
        "DOC" -> Icons.Default.Description
        "SHEET" -> Icons.Default.GridView
        "PRESENTATION" -> Icons.Default.CoPresent
        "PDF" -> Icons.Default.PictureAsPdf
        else -> Icons.Default.InsertDriveFile
    }
}

fun getColorForType(type: String): Color {
    return when (type) {
        "DOC" -> WordBlue
        "SHEET" -> ExcelGreen
        "PRESENTATION" -> PresentationOrange
        "PDF" -> PdfRed
        else -> Color.Gray
    }
}

fun getScreenIdForType(type: String): String {
    return when (type) {
        "DOC" -> "DOC_EDITOR"
        "SHEET" -> "SHEET_EDITOR"
        "PRESENTATION" -> "PPT_VIEWER"
        "PDF" -> "PDF_VIEWER"
        else -> "DASHBOARD"
    }
}

@Composable
fun BentoActionCard(
    title: String,
    icon: ImageVector,
    badgeColor: Color,
    bgColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(115.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.2.dp, borderColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Rounded icon badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(badgeColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}
