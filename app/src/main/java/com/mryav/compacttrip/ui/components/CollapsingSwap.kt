package com.mryav.compacttrip.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * A container that changes state the way the API key status banner does (Figma 22:1562): it starts
 * collapsed and expands downwards; when the [kind] of [target] changes it collapses, swaps the
 * content, then expands again. A null [target] just collapses (and the next one expands without a
 * collapse first). The content is anchored to the top and cropped by the container, so it's
 * revealed top-to-bottom. Updates within the same kind (e.g. a countdown ticking, or loading →
 * loaded with its own animation) go straight through. Height is animated in layout only.
 */
@Composable
fun <T : Any> CollapsingSwap(
    target: T?,
    kind: (T) -> Any?,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val latest by rememberUpdatedState(target)
    var shown by remember { mutableStateOf(target) }
    val reveal = remember { Animatable(0f) }
    val targetKind = target?.let(kind)

    LaunchedEffect(target == null, targetKind) {
        val current = shown
        if (reveal.value > 0f && current != null && (target == null || kind(current) != targetKind)) {
            reveal.animateTo(0f, tween(Motion.COLLAPSE_MS, easing = Motion.Collapse))
        }
        shown = latest
        if (latest != null) reveal.animateTo(1f, tween(Motion.EXPAND_MS, easing = Motion.Expand))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                layout(placeable.width, (placeable.height * reveal.value).roundToInt()) { placeable.place(0, 0) }
            }
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        val current = shown
        val display = if (target != null && current != null && kind(current) == targetKind) target else current
        if (display != null) content(display)
    }
}
