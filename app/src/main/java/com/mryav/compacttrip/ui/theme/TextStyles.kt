package com.mryav.compacttrip.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em

/** Figma text styles, sized in design units. */
object Type {
    /** Roboto Regular 30: app bar titles. */
    @Composable @ReadOnlyComposable
    fun appBar(color: Color) = TextStyle(color = color, fontFamily = Roboto, fontSize = 30.dsp)

    /** Roboto Regular 28: action rows, inputs. */
    @Composable @ReadOnlyComposable
    fun row(color: Color) = TextStyle(color = color, fontFamily = Roboto, fontSize = 28.dsp)

    /** Roboto Medium 28: card titles. */
    @Composable @ReadOnlyComposable
    fun title(color: Color) =
        TextStyle(color = color, fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 28.dsp)

    /** Roboto Condensed 28: banners, body copy, "Done". */
    @Composable @ReadOnlyComposable
    fun condensed(color: Color) = TextStyle(color = color, fontFamily = RobotoCondensed, fontSize = 28.dsp)

    /** Roboto Condensed 20, tracking -0.4: secondary lines. */
    @Composable @ReadOnlyComposable
    fun condensedSmall(color: Color) =
        TextStyle(color = color, fontFamily = RobotoCondensed, fontSize = 20.dsp, letterSpacing = (-0.02).em)
}
