package com.example.ovoshebaza.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ovoshebaza.Product
import com.example.ovoshebaza.R
import com.example.ovoshebaza.UnitType
import java.util.Locale

data class QuickStep(val label: String, val delta: Double)

@Composable
fun QuickStepButton(
    step: QuickStep,
    product: Product,
    enabled: Boolean,
    currentQuantity: Double,
    onAddToCart: (Product, Double) -> Unit,
    onUpdateQuantity: (String, Double) -> Unit,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFE7C4),
            Color(0xFFF5B56A),
            Color(0xFFE3903B)
        )
    )
    val highlight = Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.8f),
            Color.Transparent
        ),
        center = Offset(40f, 40f),
        radius = 180f
    )
    val shadowOverlay = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.18f)
        )
    )
    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled) {
                if (step.delta > 0) {
                    onAddToCart(product, step.delta)
                } else {
                    val nextQuantity = (currentQuantity + step.delta).coerceAtLeast(0.0)
                    onUpdateQuantity(product.id, nextQuantity)
                }
            },
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient, CircleShape)
                .border(1.dp, Color(0xFFFFF5E7), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(highlight, CircleShape)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(shadowOverlay, CircleShape)
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                val iconRes = when {
                    step.label.contains("шт") && step.delta == -1.0 -> R.drawable.minus_1st
                    step.label.contains("шт") && step.delta == 1.0 -> R.drawable.plus_1st
                    step.delta == -1.0 -> R.drawable.minus_1
                    step.delta == -0.5 -> R.drawable.minus_05
                    step.delta == 0.5 -> R.drawable.plus_05
                    step.delta == 1.0 -> R.drawable.plus_1
                    else -> null
                }
                if (iconRes != null) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = step.label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    Text(
                        text = step.label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CartButton(
    enabled: Boolean,
    onClick: () -> Unit,
    size: Dp,
    currentQuantity: Double,
    unit: UnitType,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFE3B8),
            Color(0xFFF6B25C),
            Color(0xFFE48C35)
        )
    )
    val highlight = Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.85f),
            Color.Transparent
        ),
        center = Offset(50f, 50f),
        radius = 220f
    )
    val shadowOverlay = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.2f)
        )
    )
    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 16.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient, CircleShape)
                .border(1.dp, Color(0xFFFFF7EB), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(highlight, CircleShape)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(shadowOverlay, CircleShape)
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.order_button),
                    contentDescription = "Корзина",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }
            if (currentQuantity > 0) {
                val quantityText = if (unit == UnitType.KG) {
                    if (currentQuantity % 1.0 == 0.0) {
                        currentQuantity.toInt().toString()
                    } else {
                        String.format(Locale.US, "%.1f", currentQuantity)
                    }
                } else {
                    currentQuantity.toInt().toString()
                }
                val unitLabel = if (unit == UnitType.KG) "кг" else "шт"
                val quantityLabel = "$quantityText$unitLabel"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF6E3B1F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = quantityLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}