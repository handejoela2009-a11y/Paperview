package com.paperview.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta inspirada en papel/tinta, coherente con la identidad visual de la app
// (independiente del filtro overlay, que solo afecta a lo que hay DEBAJO).
val PaperCream = Color(0xFFF4ECD8)
val PaperCreamDark = Color(0xFF2A2420)
val InkBrown = Color(0xFF3E3226)
val AccentOchre = Color(0xFF9C6B30)

private val LightColors = lightColorScheme(
    primary = AccentOchre,
    onPrimary = Color.White,
    background = PaperCream,
    onBackground = InkBrown,
    surface = PaperCream,
    onSurface = InkBrown,
)

private val DarkColors = darkColorScheme(
    primary = AccentOchre,
    onPrimary = Color.White,
    background = PaperCreamDark,
    onBackground = PaperCream,
    surface = PaperCreamDark,
    onSurface = PaperCream,
)

@Composable
fun PaperViewTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
