package com.example.evaluacion2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BlueSoftDark,          // El azul suave para acentos
    background = BackgroundSoftBlue, // El fondo principal celeste crema
    surface = CardWhite,             // El fondo de las tarjetas
    onBackground = TextDark,
    onSurface = TextDark,
    outline = BorderLight
)

@Composable
fun Evaluacion2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}