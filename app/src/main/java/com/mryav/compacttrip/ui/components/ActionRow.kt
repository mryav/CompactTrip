package com.mryav.compacttrip.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/**
 * Full-width action row with a trailing icon, e.g. "Add trip to home page  +".
 * Default is the dark grey row; [color] = [Palette.LinkRow] gives the blue "Open user guide ↗" row.
 * Optional [subtitle] (Settings: "Your API key is functional…").
 */
@Composable
fun ActionRow(
    label: String,
    @DrawableRes icon: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    color: Color = Palette.ActionRow,
    maxLines: Int = 1,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = color,
    ) {
        // Content is white both normally and focused.
        Row(
            modifier = Modifier.padding(horizontal = 16.du, vertical = 20.du),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.du),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.du),
            ) {
                BasicText(
                    text = label,
                    style = Type.row(Palette.OnDark),
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    BasicText(text = subtitle, style = Type.condensedSmall(Palette.OnDarkSubtle))
                }
            }
            if (icon != null) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Palette.OnDark),
                    modifier = Modifier.size(40.du),
                )
            }
        }
    }
}
