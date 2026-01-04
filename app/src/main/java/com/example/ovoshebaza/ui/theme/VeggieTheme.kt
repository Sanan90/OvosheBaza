package com.example.ovoshebaza.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val VeggieColorScheme = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7E1B3),
    onPrimaryContainer = Color(0xFF0A2E12),
    secondary = Color(0xFFFFC857),
    onSecondary = Color(0xFF3E1F00),
    secondaryContainer = Color(0xFFFFE4B8),
    onSecondaryContainer = Color(0xFF5A2F00),
    tertiary = Color(0xFF2BBFAE),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F5F1),
    onBackground = Color(0xFF1B1C18),
    surface = Color(0xFFFFFDFC),
    onSurface = Color(0xFF1B1C18),
    surfaceVariant = Color(0xFFE3ECE4),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFFB0B8B0)
)

private val VeggieShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun VeggieTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VeggieColorScheme,
        typography = Typography(),
        shapes = VeggieShapes,
        content = content
    )
}