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

    /** Roboto Condensed 24 (tracking -0.48): card headsigns, "Updated", section labels. */
    @Composable @ReadOnlyComposable
    fun condensed24(color: Color) =
        TextStyle(color = color, fontFamily = RobotoCondensed, fontSize = 24.dsp, letterSpacing = (-0.02).em)

    /** Roboto Condensed 18: card platform line. */
    @Composable @ReadOnlyComposable
    fun condensed18(color: Color, bold: Boolean = false) = TextStyle(
        color = color,
        fontFamily = RobotoCondensed,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontSize = 18.dsp,
    )

    /** Roboto Medium 20: stop locality under the name. */
    @Composable @ReadOnlyComposable
    fun medium20(color: Color) =
        TextStyle(color = color, fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 20.dsp)

    /** Roboto Regular / Bold 24: service alert body. */
    @Composable @ReadOnlyComposable
    fun body24(color: Color, bold: Boolean = false) = TextStyle(
        color = color,
        fontFamily = Roboto,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontSize = 24.dsp,
    )

    /** Roboto Bold 20: large line badges (Trip Details summary). */
    @Composable @ReadOnlyComposable
    fun badgeLarge(color: Color) =
        TextStyle(color = color, fontFamily = Roboto, fontWeight = FontWeight.Bold, fontSize = 20.dsp)

    /** Roboto Bold 16: line badges. */
    @Composable @ReadOnlyComposable
    fun badge(color: Color) =
        TextStyle(color = color, fontFamily = Roboto, fontWeight = FontWeight.Bold, fontSize = 16.dsp)

    /** Roboto Black 30: countdown number. */
    @Composable @ReadOnlyComposable
    fun countdown(color: Color) = TextStyle(
        color = color, fontFamily = Roboto, fontWeight = FontWeight.Black, fontSize = 30.dsp, letterSpacing = (-0.02).em,
    )

    /** Roboto Condensed Medium 23: "min" after the countdown. */
    @Composable @ReadOnlyComposable
    fun countdownUnit(color: Color) =
        TextStyle(color = color, fontFamily = RobotoCondensed, fontWeight = FontWeight.Medium, fontSize = 23.dsp)

    /** Roboto Condensed Medium 14: departure clock time on cards. */
    @Composable @ReadOnlyComposable
    fun cardTime(color: Color) =
        TextStyle(color = color, fontFamily = RobotoCondensed, fontWeight = FontWeight.Medium, fontSize = 14.dsp)
}
