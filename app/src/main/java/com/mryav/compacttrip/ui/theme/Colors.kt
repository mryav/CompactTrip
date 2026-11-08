package com.mryav.compacttrip.ui.theme

import androidx.compose.ui.graphics.Color

/** Colours taken from the MobileRail Figma file. */
object Palette {
    val AppBar = Color(0xFF2A2A2A)
    val Page = Color(0xFFEDEDED)
    val Card = Color.White
    val ActionRow = Color(0xFF5D5D5D)
    val LinkRow = Color(0xFF3E7CB2)
    val OnDark = Color.White
    val OnDarkSubtle = Color(0xFFEDEDED)

    val TitleText = Color(0xFF343434)
    val BodyText = Color(0xFF2A2A2A)
    val PlaceholderText = Color(0xFF898989)

    /** High / veryHigh priority alert banner. */
    val AlertHighBackground = Color(0xFFFFCCCC)
    val AlertForeground = Color(0xFF890000)

    /** Connection status banners (API key screen). */
    val SuccessBackground = Color(0xFFCAFFC8)
    val SuccessForeground = Color(0xFF004C0E)
    val NeutralStatusBackground = Color(0xFFD4D4D4)
    val NeutralStatusForeground = Color(0xFF2A2A2A)

    /** Focus state (Figma "Focus Fill", "Focused White", "Focus state" inner shadow). */
    val FocusFill = Color(0xFFFF7B00)
    val FocusedContent = Color.White
    val FocusShadow = Color(0x40000000)
}
