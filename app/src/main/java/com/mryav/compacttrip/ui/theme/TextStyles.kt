package com.mryav.compacttrip.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Type scale. Text is sized in sp like any Android app (so it honours the user's font size and
 * doesn't shrink on small screens); only layout follows the design-unit scale.
 *
 * Roboto Condensed is the app's accent: used at 16 sp and above, where it kerns well, for
 * headsigns, banners and the countdown unit, always at its natural tracking (negative letter
 * spacing on a condensed face at hdpi rounds glyph advances unevenly and reads as bad kerning).
 * Everything small or long-form is plain Roboto.
 *
 * Sizes follow Android's usual ladder: 20 app bar, 18 title and rows, 16 body, 14 secondary/label,
 * 13 caption, 12 fine print.
 */
object Type {
    /** Roboto Regular 20: app bar titles (the standard toolbar size; 22 truncates next to four actions on a 320 dp screen). */
    @Composable @ReadOnlyComposable
    fun appBar(color: Color) = TextStyle(color = color, fontFamily = Roboto, fontSize = 20.sp)

    /** Roboto Medium 18: trip and stop names, card headings. */
    @Composable @ReadOnlyComposable
    fun title(color: Color) =
        TextStyle(color = color, fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 18.sp)

    /** Roboto Regular 18: action rows, inputs, option rows. */
    @Composable @ReadOnlyComposable
    fun row(color: Color) = TextStyle(color = color, fontFamily = Roboto, fontSize = 18.sp)

    /** Roboto Regular / Bold 16: prose (alert bodies, welcome copy, empty states). */
    @Composable @ReadOnlyComposable
    fun body(color: Color, bold: Boolean = false) = TextStyle(
        color = color,
        fontFamily = Roboto,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontSize = 16.sp,
    )

    /** Roboto Medium 14: section labels ("Directions", "Recent"), "Updated 30s ago", localities. */
    @Composable @ReadOnlyComposable
    fun label(color: Color) =
        TextStyle(color = color, fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 14.sp)

    /** Roboto Regular 14: times, platforms, status, subtitles, notice counts. */
    @Composable @ReadOnlyComposable
    fun secondary(color: Color) = TextStyle(color = color, fontFamily = Roboto, fontSize = 14.sp)

    /** Roboto Regular / Bold 13: the platform line on a Quick View card. */
    @Composable @ReadOnlyComposable
    fun caption(color: Color, bold: Boolean = false) = TextStyle(
        color = color,
        fontFamily = Roboto,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontSize = 13.sp,
    )

    /** Roboto Medium 12: departure clock time in a card's countdown box. */
    @Composable @ReadOnlyComposable
    fun cardTime(color: Color) =
        TextStyle(color = color, fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 12.sp)

    /** Roboto Bold 13: line badges. */
    @Composable @ReadOnlyComposable
    fun badge(color: Color) =
        TextStyle(color = color, fontFamily = Roboto, fontWeight = FontWeight.Bold, fontSize = 13.sp)

    /** Roboto Bold 16: large line badges (Trip Details summary). */
    @Composable @ReadOnlyComposable
    fun badgeLarge(color: Color) =
        TextStyle(color = color, fontFamily = Roboto, fontWeight = FontWeight.Bold, fontSize = 16.sp)

    /** Roboto Black 22: countdown number. */
    @Composable @ReadOnlyComposable
    fun countdown(color: Color) = TextStyle(
        color = color, fontFamily = Roboto, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = (-0.02).em,
    )

    /** Roboto Condensed Medium 16: "min" after the countdown. */
    @Composable @ReadOnlyComposable
    fun countdownUnit(color: Color) =
        TextStyle(color = color, fontFamily = RobotoCondensed, fontWeight = FontWeight.Medium, fontSize = 16.sp)

    /** Roboto Condensed 18: banners, chips, service headsigns, leg stop names, app bar actions. */
    @Composable @ReadOnlyComposable
    fun condensed(color: Color) = TextStyle(color = color, fontFamily = RobotoCondensed, fontSize = 18.sp)

    /** Roboto Condensed 16: Quick View card headsigns and notice headlines. */
    @Composable @ReadOnlyComposable
    fun headsign(color: Color) = TextStyle(color = color, fontFamily = RobotoCondensed, fontSize = 16.sp)
}
