package com.letssign.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Identidade visual LetsSign (rosa/magenta + navy, conforme material institucional).
val LetsSignPink = Color(0xFFE6007A)
val LetsSignPinkDark = Color(0xFFB2005F)
val LetsSignNavy = Color(0xFF1C1B3A)
val LetsSignBackground = Color(0xFFF7F7FA)
val LetsSignSurface = Color(0xFFFFFFFF)

private val LightColors = lightColorScheme(
    primary = LetsSignPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9EB),
    onPrimaryContainer = LetsSignPinkDark,
    secondary = LetsSignNavy,
    background = LetsSignBackground,
    surface = LetsSignSurface,
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = LetsSignPink,
    onPrimary = Color.White,
    secondary = Color(0xFFCCC9F0),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

@Composable
fun LetsSignTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography.copy(
            titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        ),
        content = content
    )
}
