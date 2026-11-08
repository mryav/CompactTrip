package com.mryav.compacttrip.ui.components

import android.content.res.ColorStateList
import android.widget.ProgressBar
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView

/** The platform's own indeterminate loading spinner (android.widget.ProgressBar), tinted. */
@Composable
fun LoadingSpinner(size: Dp, color: Color, modifier: Modifier = Modifier) {
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
