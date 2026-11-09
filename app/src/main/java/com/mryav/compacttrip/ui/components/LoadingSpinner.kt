package com.mryav.compacttrip.ui.components

import android.content.res.ColorStateList
import android.widget.ProgressBar
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Android's standard spinner size (Widget.Material.ProgressBar): 48 dp, i.e. 72 px on the RUIO Joy.
 * Deliberately in dp, not design units, so it matches the platform on every screen.
 */
val SpinnerSize: Dp = 48.dp

/** The platform's own indeterminate loading spinner (android.widget.ProgressBar, Material style), tinted. */
@Composable
fun LoadingSpinner(color: Color, modifier: Modifier = Modifier, size: Dp = SpinnerSize) {
    AndroidView(
        factory = { context ->
            ProgressBar(context).apply {
                isIndeterminate = true
                indeterminateTintList = ColorStateList.valueOf(color.toArgb())
            }
        },
        modifier = modifier.size(size),
    )
}
