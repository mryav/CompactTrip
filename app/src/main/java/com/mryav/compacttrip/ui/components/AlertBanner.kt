package com.mryav.compacttrip.ui.components

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
import com.mryav.compacttrip.R
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/** Service alert banner: red for high/veryHigh priority, blue otherwise (Figma rule). */
@Composable
fun AlertBanner(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    urgent: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = if (urgent) Palette.AlertHighBackground else Palette.NoticeBackground,
    ) { focused ->
        val content = when {
            focused -> Palette.FocusedContent
            urgent -> Palette.AlertForeground
            else -> Palette.NoticeText
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.du, vertical = 20.du),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.du),
        ) {
            BasicText(
                text = text,
                modifier = Modifier.weight(1f),
                style = Type.condensed(content),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
            Image(
                painter = painterResource(R.drawable.ic_arrow_forward_ios),
                contentDescription = null,
                colorFilter = ColorFilter.tint(content),
                modifier = Modifier.size(40.du),
            )
        }
    }
}
