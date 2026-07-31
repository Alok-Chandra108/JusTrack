package com.alok.justrack.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alok.justrack.ui.theme.LightShadow

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 8
) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(LightShadow.copy(alpha = alpha))
    )
}

@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    NeuCard(modifier = modifier.clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SkeletonBox(modifier = Modifier.size(80.dp, 120.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBox(modifier = Modifier.fillMaxWidth().height(20.dp))
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
                Spacer(modifier = Modifier.height(16.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp))
            }
        }
    }
}
