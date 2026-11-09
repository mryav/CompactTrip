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
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/** White "Origin ○" / "Destination 📍" picker row (Figma 4:1614). Grey placeholder until chosen. */
@Composable
fun PlaceRow(
    placeholder: String,
    value: String?,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusSurface(onClick = onClick, color = Palette.Card, modifier = modifier.fillMaxWidth()) { focused ->
        val textColor = when {
            focused -> Palette.FocusedContent
            value == null -> Palette.PlaceholderText
            else -> Palette.BodyText
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.du, vertical = 20.du),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.du),
        ) {
            BasicText(
                text = value ?: placeholder,
                style = Type.row(textColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(if (focused) Palette.FocusedContent else if (value == null) Palette.PlaceholderText else Palette.BodyText),
                modifier = Modifier.size(40.du),
            )
        }
    }
}
