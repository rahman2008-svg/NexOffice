package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.OfficeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationScreen(
    viewModel: OfficeViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDocument.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val context = LocalContext.current

    if (activeDoc == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No presentation loaded.", color = Color.Gray)
        }
        return
    }

    val doc = activeDoc!!
    var docName by remember(doc.id) { mutableStateOf(doc.name) }
    
    // Parse slides: list of slides split by '|'
    // Slide structure is: "Title;Body;HexBg"
    var slides by remember(doc.id) {
        mutableStateOf(parseSlides(doc.content))
    }

    var selectedSlideIdx by remember { mutableStateOf(0) }
    var slideTitleInput by remember(selectedSlideIdx, doc.id, slides) {
        mutableStateOf(slides.getOrNull(selectedSlideIdx)?.title ?: "")
    }
    var slideBodyInput by remember(selectedSlideIdx, doc.id, slides) {
        mutableStateOf(slides.getOrNull(selectedSlideIdx)?.body ?: "")
    }

    var isPresentationPlaying by remember { mutableStateOf(false) }
    var activePlaySlideIdx by remember { mutableStateOf(0) }

    val pptColors = listOf("#0D47A1", "#004D40", "#E65100", "#1A237E", "#311B92", "#111111", "#EEEEEE")

    if (isPresentationPlaying) {
        // FULLSCREEN PLAY SLIDESHOW MODE
        val currentPlaySlide = slides.getOrNull(activePlaySlideIdx) ?: SlideData()
        val playBg = try { Color(android.graphics.Color.parseColor(currentPlaySlide.bg)) } catch (e: Exception) { AccentOrange }
        val slideTextColor = if (currentPlaySlide.bg == "#EEEEEE") Color.Black else Color.White

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(playBg)
                .testTag("ppt_play_root")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Headline Title
                Text(
                    text = currentPlaySlide.title,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = slideTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp,
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Detail body
                Text(
                    text = currentPlaySlide.body,
                    fontSize = 18.sp,
                    color = slideTextColor.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Top exit slideshow float button
            IconButton(
                onClick = { isPresentationPlaying = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Exit Presentation", tint = Color.White)
            }

            // Bottom navigator bar controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Slide ${activePlaySlideIdx + 1} of ${slides.size}",
                    color = slideTextColor.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (activePlaySlideIdx > 0) {
                        IconButton(
                            onClick = { activePlaySlideIdx-- },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBackIos, contentDescription = "Prev", tint = Color.White)
                        }
                    }
                    if (activePlaySlideIdx < slides.size - 1) {
                        IconButton(
                            onClick = { activePlaySlideIdx++ },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next", tint = Color.White)
                        }
                    }
                }
            }
        }
        return
    }

    Scaffold(
        modifier = modifier.testTag("ppt_editor_root"),
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
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo("DASHBOARD") },
                        modifier = Modifier.testTag("ppt_editor_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Play slides float action
                    IconButton(
                        onClick = {
                            activePlaySlideIdx = selectedSlideIdx
                            isPresentationPlaying = true
                        },
                        modifier = Modifier.testTag("ppt_play_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play presentation", tint = PresentationOrange)
                    }
                    IconButton(
                        onClick = {
                            val payload = serializeSlides(slides)
                            viewModel.updateActiveDocumentContent(payload, isAutoSave = false)
                            Toast.makeText(context, "Slides manually saved!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save presentation", tint = Color.Green)
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
            // Horizontal Slide Previews Strip
            Text(
                "Presentation Slide Deck (${slides.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = PresentationOrange,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            )
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(slides) { idx, slide ->
                    val isSel = selectedSlideIdx == idx
                    val cardBg = try { Color(android.graphics.Color.parseColor(slide.bg)) } catch (e: Exception) { Color.DarkGray }
                    val slideColor = if (slide.bg == "#EEEEEE") Color.Black else Color.White

                    Card(
                        onClick = { selectedSlideIdx = idx },
                        modifier = Modifier
                            .width(130.dp)
                            .height(84.dp)
                            .testTag("ppt_slide_pill_$idx"),
                        border = BorderStroke(2.dp, if (isSel) PresentationOrange else Color.Transparent),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = slide.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = slideColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Slide ${idx + 1}",
                                    fontSize = 9.sp,
                                    color = slideColor.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                item {
                    // Add New Slide Button
                    Card(
                        onClick = {
                            val updated = slides.toMutableList()
                            updated.add(SlideData(title = "New Slide", body = "Click details to add content", bg = "#0D47A1"))
                            slides = updated
                            selectedSlideIdx = updated.size - 1
                            viewModel.updateActiveDocumentContent(serializeSlides(updated), isAutoSave = true)
                        },
                        modifier = Modifier
                            .width(120.dp)
                            .height(84.dp)
                            .testTag("ppt_add_slide_btn"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) DarkSurface else Color.White
                        ),
                        border = BorderStroke(1.dp, if (isDarkMode) Color.DarkGray else Color.LightGray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        CenterColumn {
                            Icon(Icons.Default.PostAdd, contentDescription = "", tint = PresentationOrange)
                            Text("New Slide", fontSize = 11.sp, color = if (isDarkMode) Color.LightGray else Color.DarkGray)
                        }
                    }
                }
            }

            // Edit Slide canvas and settings
            Spacer(modifier = Modifier.height(8.dp))

            val currentSlide = slides.getOrNull(selectedSlideIdx) ?: SlideData()
            val canvasColor = try { Color(android.graphics.Color.parseColor(currentSlide.bg)) } catch (e: Exception) { Color.DarkGray }
            val slideText = if (currentSlide.bg == "#EEEEEE") Color.Black else Color.White

            // Interactive Editor Card (Representing PPT Canvas slide)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = canvasColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Slide Title Editor Form
                    BasicTextField(
                        value = slideTitleInput,
                        onValueChange = { input ->
                            slideTitleInput = input
                            val updated = slides.toMutableList()
                            updated[selectedSlideIdx] = currentSlide.copy(title = input)
                            slides = updated
                            viewModel.updateActiveDocumentContent(serializeSlides(updated), isAutoSave = true)
                        },
                        textStyle = TextStyle(
                            color = slideText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slide_title_editor"),
                        decorationBox = { innerTextField ->
                            if (slideTitleInput.isEmpty()) {
                                Text("Click to Title", color = slideText.copy(alpha = 0.4f), fontSize = 24.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Slide Detail Body Editor Form
                    BasicTextField(
                        value = slideBodyInput,
                        onValueChange = { input ->
                            slideBodyInput = input
                            val updated = slides.toMutableList()
                            updated[selectedSlideIdx] = currentSlide.copy(body = input)
                            slides = updated
                            viewModel.updateActiveDocumentContent(serializeSlides(updated), isAutoSave = true)
                        },
                        textStyle = TextStyle(
                            color = slideText.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("slide_body_editor"),
                        decorationBox = { innerTextField ->
                            if (slideBodyInput.isEmpty()) {
                                Text("Click to enter body details here", color = slideText.copy(alpha = 0.4f), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // Design Customizing options (Slides Background Paint Palette & Delete slide button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDarkMode) DarkSurface else Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Canvas Palette Theme", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PresentationOrange)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        pptColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                    .border(
                                        if (currentSlide.bg == hex) 1.5.dp else 0.dp,
                                        if (isDarkMode) Color.White else Color.Black,
                                        CircleShape
                                    )
                                    .clickable {
                                        val updated = slides.toMutableList()
                                        updated[selectedSlideIdx] = currentSlide.copy(bg = hex)
                                        slides = updated
                                        viewModel.updateActiveDocumentContent(serializeSlides(updated), isAutoSave = true)
                                    }
                            )
                        }
                    }
                }

                if (slides.size > 1) {
                    IconButton(
                        onClick = {
                            val updated = slides.toMutableList()
                            updated.removeAt(selectedSlideIdx)
                            slides = updated
                            selectedSlideIdx = 0
                            viewModel.updateActiveDocumentContent(serializeSlides(updated), isAutoSave = true)
                            Toast.makeText(context, "Slide deleted", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete slide", tint = Color.Red)
                    }
                }
            }
        }
    }
}

data class SlideData(
    val title: String = "Slide Title",
    val body: String = "Slide Details Content",
    val bg: String = "#0D47A1"
)

fun parseSlides(content: String): List<SlideData> {
    if (content.isEmpty()) return listOf(SlideData())
    val rawSlides = content.split("|")
    return rawSlides.map { raw ->
        val parts = raw.split(";")
        SlideData(
            title = parts.getOrNull(0) ?: "Slide Title",
            body = parts.getOrNull(1) ?: "Slide body details",
            bg = parts.getOrNull(2) ?: "#0D47A1"
        )
    }
}

fun serializeSlides(slides: List<SlideData>): String {
    return slides.joinToString("|") { "${it.title};${it.body};${it.bg}" }
}

@Composable
fun CenterColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}
