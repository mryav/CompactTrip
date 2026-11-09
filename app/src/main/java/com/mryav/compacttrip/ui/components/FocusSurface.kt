package com.mryav.compacttrip.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.createRippleModifierNode
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.du
import kotlin.math.roundToInt

const val FOCUS_SHADOW_FADE_MS = 400

/**
 * The one focusable building block for the whole app (Figma "Focused states examples", node 22:993).
 *
 * Focused: background becomes [Palette.FocusFill] with the "Focus state" inner shadow
 * (#00000040, y 6, blur 4), which fades in over [FOCUS_SHADOW_FADE_MS] each time an item gains focus,
 * and [content] is told `focused = true` so it switches its text and icons to [Palette.FocusedContent].
 * Nested chips (line badges, countdown boxes) keep their colours. The fill follows [shape].
 * Pressed (touch or OK): the Android ripple, dark on light surfaces and light on dark ones ([color]),
 * or [rippleColor] when the surface is transparent over something dark (e.g. the app bar).
 * When [enabled] is false it is neither clickable nor focusable.
 * With [onClick] = null it's focusable but not clickable (e.g. preview cards, focused only to scroll).
 */
@Composable
fun FocusSurface(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    shape: Shape = RectangleShape,
    enabled: Boolean = true,
    /** false: focus shows only the inner shadow (over the content), no orange fill. For large cards. */
    fillOnFocus: Boolean = true,
    onClickLabel: String? = null,
    rippleColor: Color = Color.Unspecified,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .focusHighlight(focused = focused, color = color, shape = shape, fill = fillOnFocus)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = when {
                            rippleColor.isSpecified -> if (rippleColor.luminance() > 0.5f) LightRipple else DarkRipple
                            color.alpha > 0f && color.luminance() < 0.5f -> LightRipple
                            else -> DarkRipple
                        },
                        enabled = enabled,
                        onClickLabel = onClickLabel,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier.focusable(enabled = enabled, interactionSource = interactionSource)
                },
            ),
        contentAlignment = contentAlignment,
    ) {
        content(focused && fillOnFocus)
    }
}

/**
 * Background + focus highlight for anything focusable, including text fields that can't use
 * [FocusSurface]: [color] normally, focus fill + fading inner shadow when [focused].
 * With [fill] = false, focus keeps [color] and only the shadow fades in, drawn over the content
 * (large cards whose own blocks would otherwise hide it).
 */
@Composable
fun Modifier.focusHighlight(focused: Boolean, color: Color, shape: Shape = RectangleShape, fill: Boolean = true): Modifier {
    // Fill switches instantly; the shadow fades in. Read only in the draw phase (no recomposition per frame).
    val shadowAlpha = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = if (focused) tween(FOCUS_SHADOW_FADE_MS) else snap(),
        label = "focusShadow",
    )
    val shadowOffset = 6.du
    val shadowBlur = 4.du
    return this
        .clip(shape)
        .background(if (focused && fill) Palette.FocusFill else color)
        .focusInnerShadow(shape, shadowOffset, shadowBlur, overContent = !fill) { shadowAlpha.value }
}

/**
 * Figma inner shadow: #00000040, offset (0, [offset]), blur [blur], spread 0, following [shape].
 * Built the way Figma renders it: the area outside the shape, shifted down by [offset], blurred,
 * then clipped to the shape. This gives the dark top band plus the faint side shading and correct
 * rounded corners. Drawn over the fill and under the content, or over the content when
 * [overContent]. [alpha] (0..1) is read at draw time.
 */
private fun Modifier.focusInnerShadow(
    shape: Shape,
    offset: Dp,
    blur: Dp,
    overContent: Boolean,
    alpha: () -> Float,
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val clip = Path().apply { addOutline(outline) }
    val dy = offset.toPx()
    val blurPx = blur.toPx()
    val margin = blurPx * 4 + dy
    val hole = Path().apply {
        addOutline(outline)
        translate(Offset(0f, dy))
    }
    val frame = Path().apply {
        addRect(Rect(-margin, -margin, size.width + margin, size.height + margin))
    }
    val shadow = Path().apply { op(frame, hole, PathOperation.Difference) }
    // Figma blur is a CSS-style radius (sigma = blur / 2). Android: sigma = 0.57735 * radius + 0.5.
    val sigma = blurPx / 2f
    val androidRadius = ((sigma - 0.5f) / 0.57735f).coerceAtLeast(0.5f)
    val baseColor = Palette.FocusShadow.toArgb()
    val baseAlpha = android.graphics.Color.alpha(baseColor)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = baseColor
        maskFilter = BlurMaskFilter(androidRadius, BlurMaskFilter.Blur.NORMAL)
    }
    val androidShadow = shadow.asAndroidPath()

    onDrawWithContent {
        if (overContent) drawContent()
        val a = alpha()
        if (a > 0f) {
            paint.alpha = (baseAlpha * a).roundToInt()
            clipPath(clip) {
                drawIntoCanvas { it.nativeCanvas.drawPath(androidShadow, paint) }
            }
        }
        if (!overContent) drawContent()
    }
}

/** Touch / OK press feedback only; focus is shown by [focusHighlight]. No ripple (cheap on low-end GPUs). */
/**
 * Touch feedback: the standard Android ripple, spreading from the touch point (or the centre for a
 * keypad OK press). Only the press is drawn; focus has its own orange/shadow highlight, so the
 * ripple's focus and hover layers are off.
 */
private data class RippleIndication(private val color: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        createRippleModifierNode(
            interactionSource = interactionSource,
            bounded = true,
            radius = Dp.Unspecified,
            color = { color },
            rippleAlpha = { if (color.luminance() > 0.5f) LightRippleAlpha else DarkRippleAlpha },
        )

    private companion object {
        // Material values: a light ripple on dark surfaces needs more opacity to read.
        val DarkRippleAlpha = RippleAlpha(draggedAlpha = 0.16f, focusedAlpha = 0f, hoveredAlpha = 0f, pressedAlpha = 0.12f)
        val LightRippleAlpha = RippleAlpha(draggedAlpha = 0.24f, focusedAlpha = 0f, hoveredAlpha = 0f, pressedAlpha = 0.24f)
    }
}

private val DarkRipple = RippleIndication(Color.Black)
private val LightRipple = RippleIndication(Color.White)
