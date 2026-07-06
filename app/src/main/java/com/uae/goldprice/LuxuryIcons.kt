package com.uae.goldprice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun LuxuryGoldIcon(modifier: Modifier = Modifier) {
    val goldPrimary = MaterialTheme.colorScheme.primary
    val goldSecondary = Color(0xFFCFB53B)

    Canvas(modifier = modifier.size(40.dp)) {
        val strokeWidth = 2.dp.toPx()

        // Draw a stylized "G" or a gold bar silhouette
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(goldPrimary, goldSecondary)
            ),
            style = Stroke(width = strokeWidth)
        )

        drawRect(
            color = goldPrimary,
            size = size.copy(width = size.width * 0.4f, height = size.height * 0.2f),
            topLeft = center.copy(x = center.x - size.width * 0.2f, y = center.y - size.height * 0.1f)
        )
    }
}
