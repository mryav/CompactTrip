package com.mryav.compacttrip.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mryav.compacttrip.data.model.LineBadge
import com.mryav.compacttrip.ui.theme.LineColors
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/** Line chip, e.g. "T1" on orange (Roboto Bold 13 sp, padding 8/4, radius 4; 16 sp and 10/6 when [large]). */
@Composable
fun LineBadgeChip(badge: LineBadge, modifier: Modifier = Modifier, large: Boolean = false) {
    Chip(
        text = badge.label,
        background = LineColors.of(badge.label, badge.mode),
        textColor = Color.White,
        large = large,
        modifier = modifier,
    )
}

/** Yellow "CANCELLED" chip. */
@Composable
fun CancelledChip(modifier: Modifier = Modifier) {
    Chip(
        text = "CANCELLED",
        background = Palette.CancelledChip,
        textColor = Palette.CancelledChipText,
        modifier = modifier.border(1.du, Palette.CancelledChipBorder, RoundedCornerShape(4.du)),
    )
}

@Composable
private fun Chip(text: String, background: Color, textColor: Color, modifier: Modifier = Modifier, large: Boolean = false) {
    BasicText(
        text = text,
        style = if (large) Type.badgeLarge(textColor) else Type.badge(textColor),
        maxLines = 1,
        modifier = modifier
            .background(background, RoundedCornerShape(4.du))
            .padding(horizontal = if (large) 10.du else 8.du, vertical = if (large) 6.du else 4.du),
    )
}
