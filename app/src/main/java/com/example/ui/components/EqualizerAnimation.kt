package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun EqualizerAnimation(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    maxHeight: Dp = 24.dp,
    barWidth: Dp = 4.dp,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "equalizer")

    val heights = (0 until barCount).map { i ->
        val duration = 400 + (i * 120)
        val target = if (isPlaying) 1f else 0.2f
        val animatedValue by transition.animateFloat(
            initialValue = 0.15f,
            targetValue = target,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$i"
        )
        if (isPlaying) animatedValue else 0.2f
    }

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(fraction.coerceIn(0.15f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}
