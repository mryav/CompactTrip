package com.mryav.compacttrip.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Roboto
import com.mryav.compacttrip.ui.theme.dsp
import com.mryav.compacttrip.ui.theme.du

/** Full-width dark grey action row with a trailing icon (e.g. "Add trip to home page  +"). */
@Composable
fun ActionRow(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = Palette.ActionRow,
    ) {
        // Content is white both normally and focused.
        Row(
            modifier = Modifier.padding(horizontal = 16.du, vertical = 20.du),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.du),
        ) {
            BasicText(
                text = label,
                modifier = Modifier.weight(1f),
                style = TextStyle(color = Palette.OnDark, fontFamily = Roboto, fontSize = 28.dsp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Palette.OnDark),
                modifier = Modifier.size(40.du),
            )
        }
    }
}
