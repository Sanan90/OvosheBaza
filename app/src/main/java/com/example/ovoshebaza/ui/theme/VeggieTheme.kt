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
    primary = Color(0xFF5C8E1E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3F2C6),
    onPrimaryContainer = Color(0xFF1B3206),
    secondary = Color(0xFFFFB14D),
    onSecondary = Color(0xFF4A2500),
    secondaryContainer = Color(0xFFFFE4C1),
    onSecondaryContainer = Color(0xFF5A2F00),
    tertiary = Color(0xFF8BC34A),
    onTertiary = Color(0xFF1B3307),
    background = Color(0xFFF7F1E8),
    onBackground = Color(0xFF2B251A),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF2B251A),
    surfaceVariant = Color(0xFFF0E6D9),
    onSurfaceVariant = Color(0xFF5A5247),
    outline = Color(0xFFD6C9B9)
)

private val VeggieShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
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