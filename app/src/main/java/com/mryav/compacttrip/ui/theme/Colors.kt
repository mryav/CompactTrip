package com.mryav.compacttrip.ui.theme

import androidx.compose.ui.graphics.Color

/** Colours taken from the MobileRail Figma file (Home, node 4:1750). */
object Palette {
    val AppBar = Color(0xFF2A2A2A)
    val Page = Color(0xFFEDEDED)
    val ActionRow = Color(0xFF5D5D5D)
    val OnDark = Color.White

    /** High / veryHigh priority alert banner. */
    val AlertHighBackground = Color(0xFFFFCCCC)
    val AlertForeground = Color(0xFF890000)

    /** Focus state (Figma "Focus Fill", "Focused White", "Focus state" inner shadow). */
    val FocusFill = Color(0xFFFF7B00)
    val FocusedContent = Color.White
    val FocusShadow = Color(0x40000000)
}
