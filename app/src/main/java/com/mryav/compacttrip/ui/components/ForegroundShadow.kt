package com.mryav.compacttrip.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.du

/** Height of the shadow cast upwards by a pinned bottom panel. */
private const val SHADOW_DU = 8

/**
 * A soft shadow along the top edge of a panel pinned over scrolling content, cast upwards onto the
 * content. Fades in (0.4 s, like the focus shadow) while [visible], i.e. while content sits behind
 * the panel. The alpha is read in the draw phase only.
 */
@Composable
fun Modifier.foregroundShadow(visible: Boolean): Modifier {
    val alpha = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "foregroundShadow",
    )
    val height = SHADOW_DU.du
    return drawBehind {
        val a = alpha.value
        if (a <= 0f) return@drawBehind
        val h = height.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Palette.FocusShadow),
                startY = -h,
                endY = 0f,
            ),
            topLeft = Offset(0f, -h),
            size = Size(size.width, h),
            alpha = a,
        )
    }
}
