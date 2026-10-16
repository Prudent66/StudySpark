package com.studyspark.app.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay

/**
 * A simple fade-in + slide-up animation for any composable.
 *
 * Usage:
 *   Column(modifier = Modifier.fadeInSlideUp(delayMillis = 100)) { ... }
 */
@Composable
fun Modifier.fadeInSlideUp(
    delayMillis: Int = 0,
    durationMillis: Int = 500
): Modifier {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis),
        label = "alpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 40f,
        animationSpec = tween(durationMillis = durationMillis),
        label = "translationY"
    )

    return this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
    }
}
