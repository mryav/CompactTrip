package com.mryav.compacttrip.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun CompactTripsTheme(content: @Composable () -> Unit) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val scale = remember(widthDp) {
        DesignScale(dpPerUnit = (widthDp / DESIGN_WIDTH_UNITS).coerceAtMost(1f))
    }
    CompositionLocalProvider(LocalDesignScale provides scale, content = content)
}
