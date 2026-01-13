package com.example.ovoshebaza.ui.root

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

@Composable
fun SupportIcon(
    iconRes: Int,
    isVisible: Boolean,
    iconOffsetX: Dp,
    iconAlpha: Float,
    bounceProgress: Float,
    bottomPadding: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(
                end = 20.dp,
                bottom = bottomPadding
            )
            .size(75.dp)
            .offset(x = iconOffsetX)
            .graphicsLayer(alpha = iconAlpha)
            .offset(y = lerp(0.dp, (-6).dp, bounceProgress))
            .clickable {
                if (isVisible) {
                    onClick()
                }
            },
        contentAlignment = Alignment.Center

    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "Связь с поддержкой",
            modifier = Modifier.size(125.dp)
        )
    }
}