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

    /** Quick View departure cards. */
    val CardSurface = Color(0xFFF7F7F7)
    val CardBorder = Color(0xFFDFDFDF)
    val CardCancelled = Color(0xFFDBDBDB)
    val CardSecondaryText = Color(0xFF475056)
    val CountdownUnit = Color(0xFF585858)
    val PlatformChanged = Color(0xFFB15900)
    val CancelledChip = Color(0xFFFFDC41)
    val CancelledChipBorder = Color(0xFFAFA300)
    val CancelledChipText = Color(0xFF1D2CB4)
    val NoticeText = Color(0xFF00477C)
    val NoticeBackground = Color(0xFFCDEEFF)
    val PlatformText = Color(0xFF596267)
    /** Trip Details leg borders: walking, and the final destination. */
    val WalkLegBorder = Color(0xFFC0C0C0)
    val DestinationLegBorder = Color(0xFF2A2A2A)
    val WalkChipBorder = Color(0xFF586065)
    val DurationChipBorder = Color(0xFFA1A1A1)
    val MutedText = Color(0xFF7F7F7F)

    /** Progress indicators (Figma variables). */
    val OnTime = Color(0xFF008D00)
    val Delayed = Color(0xFFC90032)
    val LiveUnknown = Color(0xFF2A2A2A)

    /** Focus state (Figma "Focus Fill", "Focused White", "Focus state" inner shadow). */
    val FocusFill = Color(0xFFFF7B00)
    val FocusedContent = Color.White
    val FocusShadow = Color(0x40000000)
}
