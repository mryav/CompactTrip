package com.mryav.compacttrip.ui.components

import androidx.compose.animation.core.CubicBezierEasing

/**
 * The app's motion language (first used by the API key status banner, Figma 22:1562; extended to
 * departure loading in Figma 24:1843): containers expand downwards with a smooth ease-out, collapse
 * with a gentle ease-in, and content is revealed by being cropped in from the top.
 */
object Motion {
    const val EXPAND_MS = 420
    const val COLLAPSE_MS = 240

    /** Content reveal per item, and the stagger between neighbouring items. */
    const val REVEAL_MS = 360
    const val STAGGER_MS = 70

    /**
     * Screen transitions: full-width push. The new screen slides in from the edge and pushes the old
     * one off the other side; both stay opaque, so no fade is needed.
     */
    const val SCREEN_MS = 300

    /** Loading spinners only appear if loading takes longer than this (Figma 24:1563). */
    const val SPINNER_DELAY_MS = 3_000L

    /** Fast start, gentle landing. */
    val Expand = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    /** Gentle start, quick finish. */
    val Collapse = CubicBezierEasing(0.55f, 0f, 0.75f, 0.2f)

    /**
     * Screen push (Material standard easing). Softer start than [Expand]: a full-width move with
     * Expand's fast start would jump most of the way in the first frame or two.
     */
    val Push = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}
