package com.mryav.compacttrip.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * Content that expands downwards when [expanded] turns true and collapses when it turns false, the
 * way the app's banners and previews do (Figma 22:1562): the content stays anchored to the top and
 * is cropped by the animated height, so it's revealed top-to-bottom. Anything that should move
 * with it (a chevron) can animate on [expanded] with [expandTween].
 */
@Composable
fun Expandable(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reveal = remember { Animatable(if (expanded) 1f else 0f) }
    LaunchedEffect(expanded) {
        if (expanded) {
            reveal.animateTo(1f, tween(Motion.EXPAND_MS, easing = Motion.Expand))
        } else {
            reveal.animateTo(0f, tween(Motion.COLLAPSE_MS, easing = Motion.Collapse))
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                layout(placeable.width, (placeable.height * reveal.value).roundToInt()) { placeable.place(0, 0) }
            },
    ) {
        if (reveal.value > 0f || expanded) content()
    }
}

/** The tween [Expandable] uses for the given direction, for things animating alongside it. */
fun expandTween(expanded: Boolean) = if (expanded) {
    tween<Float>(Motion.EXPAND_MS, easing = Motion.Expand)
} else {
    tween(Motion.COLLAPSE_MS, easing = Motion.Collapse)
}
