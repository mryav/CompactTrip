package com.mryav.compacttrip.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Figma frames are drawn at 480 units wide = the RUIO Joy's physical pixel width.
 * One design unit ("du") maps to [dpPerUnit] dp so a 480-unit frame fills a 480-px-wide
 * screen exactly, whatever the device density. Capped at 1 so larger phones don't balloon.
 */
@Immutable
data class DesignScale(val dpPerUnit: Float)

const val DESIGN_WIDTH_UNITS = 480f

val LocalDesignScale = staticCompositionLocalOf { DesignScale(dpPerUnit = 1f) }

/** Design units → dp. */
val Int.du: Dp
    @Composable @ReadOnlyComposable
    get() = (this * LocalDesignScale.current.dpPerUnit).dp

/** Design units → sp (still honours the user's font scale). */
val Int.dsp: TextUnit
    @Composable @ReadOnlyComposable
    get() = (this * LocalDesignScale.current.dpPerUnit).sp
