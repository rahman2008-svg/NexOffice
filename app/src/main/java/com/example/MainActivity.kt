package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.OfficeViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: OfficeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val activeScreen by viewModel.activeScreen.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BoxWithNavigationRouter(
                        viewModel = viewModel,
                        activeScreen = activeScreen,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun BoxWithNavigationRouter(
    viewModel: OfficeViewModel,
    activeScreen: String,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = activeScreen,
        label = "ScreenCrossNavigator"
    ) { screen ->
        when (screen) {
            "DASHBOARD" -> DashboardScreen(viewModel = viewModel, modifier = modifier)
            "DOC_EDITOR" -> DocEditorScreen(viewModel = viewModel, modifier = modifier)
            "SHEET_EDITOR" -> SheetEditorScreen(viewModel = viewModel, modifier = modifier)
            "PDF_VIEWER" -> PdfViewerScreen(viewModel = viewModel, modifier = modifier)
            "PPT_VIEWER" -> PresentationScreen(viewModel = viewModel, modifier = modifier)
            else -> DashboardScreen(viewModel = viewModel, modifier = modifier)
        }
    }
}
