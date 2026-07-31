package com.alok.justrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alok.justrack.ui.theme.Background
import com.alok.justrack.ui.theme.DarkShadow
import com.alok.justrack.ui.theme.LightShadow

fun Modifier.neumorphicShadow(
    offset: Dp = 4.dp,
    blurRadius: Dp = 8.dp,
    lightShadowColor: Color = LightShadow,
    darkShadowColor: Color = DarkShadow,
    cornerRadius: Dp = 12.dp,
    isInset: Boolean = false
): Modifier = this.drawBehind {
    val shadowPaint = Paint().asFrameworkPaint().apply {
        color = Color.Transparent.toArgb()
    }
    
    val shadowPath = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                0f, 0f, size.width, size.height,
                androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx())
            )
        )
    }

    drawIntoCanvas { canvas ->
        if (!isInset) {
            // Light shadow (top-left)
            shadowPaint.setShadowLayer(
                blurRadius.toPx(),
                -offset.toPx(),
                -offset.toPx(),
                lightShadowColor.toArgb()
            )
            canvas.nativeCanvas.drawPath(shadowPath.asAndroidPath(), shadowPaint)

            // Dark shadow (bottom-right)
            shadowPaint.setShadowLayer(
                blurRadius.toPx(),
                offset.toPx(),
                offset.toPx(),
                darkShadowColor.toArgb()
            )
            canvas.nativeCanvas.drawPath(shadowPath.asAndroidPath(), shadowPaint)
        } else {
            // Inset is more complex, for now we will skip or implement simple version
            // For the skeleton, extruded is the most common
        }
    }
}

@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .neumorphicShadow(cornerRadius = cornerRadius)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Background)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    // Simplified for skeleton
    Box(
        modifier = modifier
            .neumorphicShadow(cornerRadius = cornerRadius)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Background)
            .padding(vertical = 12.dp, horizontal = 24.dp)
    ) {
        content()
    }
}
