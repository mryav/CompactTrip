package com.mryav.compacttrip.ui.theme

import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** System Roboto: nothing bundled, so no font loading at startup. */
val Roboto: FontFamily = FontFamily.SansSerif

/** System Roboto Condensed ("sans-serif-condensed" on AOSP). */
val RobotoCondensed: FontFamily = FontFamily(
    Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight.Normal),
    Font(DeviceFontFamilyName("sans-serif-condensed-medium"), FontWeight.Medium),
)
