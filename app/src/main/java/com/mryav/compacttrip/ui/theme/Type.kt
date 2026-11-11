package com.mryav.compacttrip.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mryav.compacttrip.R

/** System Roboto: nothing bundled, so no font loading at startup. */
val Roboto: FontFamily = FontFamily.SansSerif

/**
 * Bundled Roboto Condensed (res/font). The system "sans-serif-condensed" differs between devices
 * (static files up to Android 11, an instance of the variable Roboto from 12), and the variable
 * instance spaces glyphs unevenly at small sizes on low-density screens like the RUIO Joy's.
 * Bundling the static, hinted files makes the accent face render the same everywhere.
 */
val RobotoCondensed: FontFamily = FontFamily(
    Font(R.font.roboto_condensed_regular, FontWeight.Normal),
    Font(R.font.roboto_condensed_medium, FontWeight.Medium),
    Font(R.font.roboto_condensed_bold, FontWeight.Bold),
)
