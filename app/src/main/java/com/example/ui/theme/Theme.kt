package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFFF5722), // AccentOrange
    secondary = Color(0xFF1A73E8), // WordBlue
    tertiary = Color(0xFF0F9D58), // ExcelGreen
    background = Color(0xFF0D0F14), // DarkBg
    surface = Color(0xFF171A23), // DarkSurface
    onPrimary = Color.White,
    onBackground = Color(0xFFE3E2E6),
    onSurface = Color(0xFFE3E2E6)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFFFF5722),
    secondary = Color(0xFF1A73E8),
    tertiary = Color(0xFF0F9D58),
    background = Color(0xFFF5F6F9),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
