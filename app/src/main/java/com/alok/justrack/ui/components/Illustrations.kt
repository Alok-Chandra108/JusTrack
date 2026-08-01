package com.alok.justrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.alok.justrack.ui.theme.AccentPrimary
import com.alok.justrack.ui.theme.AccentSecondary
import com.alok.justrack.ui.theme.SurfaceColor
import com.alok.justrack.ui.theme.TextSecondary

@Composable
fun MovieIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(120.dp)) {
        drawClapperboard()
    }
}

@Composable
fun TvShowIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(120.dp)) {
        drawTvScreen()
    }
}

private fun DrawScope.drawClapperboard() {
    val w = size.width
    val h = size.height
    val centerX = w / 2

    val boardWidth = w * 0.72f
    val boardHeight = h * 0.42f
    val boardX = centerX - boardWidth / 2
    val boardY = h * 0.38f
    val cornerRadius = 6.dp.toPx()

    // Board body
    drawRoundRect(
        color = AccentPrimary.copy(alpha = 0.12f),
        topLeft = Offset(boardX, boardY),
        size = Size(boardWidth, boardHeight),
        cornerRadius = CornerRadius(cornerRadius)
    )
    drawRoundRect(
        color = AccentPrimary.copy(alpha = 0.35f),
        topLeft = Offset(boardX, boardY),
        size = Size(boardWidth, boardHeight),
        cornerRadius = CornerRadius(cornerRadius),
        style = Stroke(width = 2.5.dp.toPx())
    )

    // Text lines on board
    val lineY1 = boardY + boardHeight * 0.3f
    val lineY2 = boardY + boardHeight * 0.55f
    val lineY3 = boardY + boardHeight * 0.78f
    val linePadding = boardWidth * 0.12f

    drawRoundRect(
        color = AccentPrimary.copy(alpha = 0.2f),
        topLeft = Offset(boardX + linePadding, lineY1),
        size = Size(boardWidth * 0.5f, 4.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
    drawRoundRect(
        color = AccentPrimary.copy(alpha = 0.15f),
        topLeft = Offset(boardX + linePadding, lineY2),
        size = Size(boardWidth * 0.35f, 4.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
    drawRoundRect(
        color = AccentPrimary.copy(alpha = 0.15f),
        topLeft = Offset(boardX + linePadding, lineY3),
        size = Size(boardWidth * 0.42f, 4.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx())
    )

    // Clapper top (hinged lid)
    val lidHeight = h * 0.2f
    val lidY = boardY - lidHeight + 2.dp.toPx()

    val lidPath = Path().apply {
        moveTo(boardX, boardY)
        lineTo(boardX + boardWidth, boardY)
        lineTo(boardX + boardWidth, lidY)
        lineTo(boardX, lidY)
        close()
    }
    drawPath(lidPath, color = AccentPrimary.copy(alpha = 0.18f))
    drawPath(lidPath, color = AccentPrimary.copy(alpha = 0.4f), style = Stroke(width = 2.dp.toPx()))

    // Diagonal stripes on clapper lid
    val stripeCount = 5
    val stripeSpacing = boardWidth / stripeCount
    for (i in 0 until stripeCount) {
        val sx = boardX + i * stripeSpacing
        val stripePath = Path().apply {
            moveTo(sx, lidY)
            lineTo(sx + stripeSpacing * 0.5f, lidY)
            lineTo(sx + stripeSpacing * 0.5f + 6.dp.toPx(), boardY)
            lineTo(sx + 6.dp.toPx(), boardY)
            close()
        }
        drawPath(stripePath, color = AccentPrimary.copy(alpha = 0.25f))
    }

    // Hinge circle
    drawCircle(
        color = AccentPrimary.copy(alpha = 0.4f),
        radius = 5.dp.toPx(),
        center = Offset(boardX + 8.dp.toPx(), boardY)
    )
    drawCircle(
        color = SurfaceColor,
        radius = 2.5.dp.toPx(),
        center = Offset(boardX + 8.dp.toPx(), boardY)
    )

    // Star decoration (top-right)
    val starCx = w * 0.82f
    val starCy = h * 0.18f
    val starOuter = 10.dp.toPx()
    val starInner = 4.dp.toPx()
    val starPoints = 5
    val starPath = Path()
    for (i in 0 until starPoints * 2) {
        val radius = if (i % 2 == 0) starOuter else starInner
        val angle = Math.toRadians((i * 360.0 / (starPoints * 2)) - 90.0).toFloat()
        val px = starCx + radius * kotlin.math.cos(angle)
        val py = starCy + radius * kotlin.math.sin(angle)
        if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
    }
    starPath.close()
    drawPath(starPath, color = AccentPrimary.copy(alpha = 0.2f))
    drawPath(starPath, color = AccentPrimary.copy(alpha = 0.45f), style = Stroke(width = 1.5.dp.toPx()))
}

private fun DrawScope.drawTvScreen() {
    val w = size.width
    val h = size.height
    val centerX = w / 2
    val centerY = h * 0.45f

    // TV body
    val tvWidth = w * 0.75f
    val tvHeight = h * 0.52f
    val tvX = centerX - tvWidth / 2
    val tvY = centerY - tvHeight / 2
    val cornerRadius = 10.dp.toPx()

    drawRoundRect(
        color = AccentSecondary.copy(alpha = 0.12f),
        topLeft = Offset(tvX, tvY),
        size = Size(tvWidth, tvHeight),
        cornerRadius = CornerRadius(cornerRadius)
    )
    drawRoundRect(
        color = AccentSecondary.copy(alpha = 0.4f),
        topLeft = Offset(tvX, tvY),
        size = Size(tvWidth, tvHeight),
        cornerRadius = CornerRadius(cornerRadius),
        style = Stroke(width = 2.5.dp.toPx())
    )

    // Screen inside TV
    val screenPadding = 8.dp.toPx()
    val screenWidth = tvWidth - screenPadding * 2
    val screenHeight = tvHeight - screenPadding * 2
    drawRoundRect(
        color = AccentSecondary.copy(alpha = 0.06f),
        topLeft = Offset(tvX + screenPadding, tvY + screenPadding),
        size = Size(screenWidth, screenHeight),
        cornerRadius = CornerRadius(6.dp.toPx())
    )

    // Play button triangle in screen
    val playSize = 18.dp.toPx()
    val playX = centerX - playSize * 0.4f
    val playY = centerY - playSize * 0.5f
    val playPath = Path().apply {
        moveTo(playX, playY)
        lineTo(playX + playSize, playY + playSize * 0.5f)
        lineTo(playX, playY + playSize)
        close()
    }
    drawPath(playPath, color = AccentSecondary.copy(alpha = 0.5f))

    // TV stand
    val standWidth = tvWidth * 0.3f
    val standHeight = h * 0.06f
    val standX = centerX - standWidth / 2
    val standY = tvY + tvHeight

    drawRoundRect(
        color = AccentSecondary.copy(alpha = 0.25f),
        topLeft = Offset(standX, standY),
        size = Size(standWidth, standHeight),
        cornerRadius = CornerRadius(4.dp.toPx())
    )

    // TV base
    val baseWidth = tvWidth * 0.45f
    val baseHeight = h * 0.03f
    val baseX = centerX - baseWidth / 2
    val baseY = standY + standHeight

    drawRoundRect(
        color = AccentSecondary.copy(alpha = 0.35f),
        topLeft = Offset(baseX, baseY),
        size = Size(baseWidth, baseHeight),
        cornerRadius = CornerRadius(3.dp.toPx())
    )

    // Antenna lines
    val antennaLength = h * 0.18f
    val antennaBaseX = centerX
    val antennaBaseY = tvY

    val leftAntenna = Path().apply {
        moveTo(antennaBaseX, antennaBaseY)
        lineTo(antennaBaseX - w * 0.15f, antennaBaseY - antennaLength)
    }
    val rightAntenna = Path().apply {
        moveTo(antennaBaseX, antennaBaseY)
        lineTo(antennaBaseX + w * 0.15f, antennaBaseY - antennaLength)
    }
    drawPath(leftAntenna, color = AccentSecondary.copy(alpha = 0.35f), style = Stroke(width = 2.dp.toPx()))
    drawPath(rightAntenna, color = AccentSecondary.copy(alpha = 0.35f), style = Stroke(width = 2.dp.toPx()))

    // Antenna tips
    drawCircle(color = AccentSecondary.copy(alpha = 0.5f), radius = 3.dp.toPx(), center = Offset(antennaBaseX - w * 0.15f, antennaBaseY - antennaLength))
    drawCircle(color = AccentSecondary.copy(alpha = 0.5f), radius = 3.dp.toPx(), center = Offset(antennaBaseX + w * 0.15f, antennaBaseY - antennaLength))
}
