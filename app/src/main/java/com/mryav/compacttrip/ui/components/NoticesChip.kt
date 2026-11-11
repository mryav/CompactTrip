package com.mryav.compacttrip.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.model.Notice
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/**
 * "Trackwork · 2 notices  >" chip under a trip's title (Figma 6:600): the top notice's headline
 * (ellipsised) and the count. Red for high/veryHigh priority, blue otherwise.
 */
@Composable
fun NoticesChip(notices: List<Notice>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val top = notices.firstOrNull() ?: return
    FocusSurface(
        onClick = onClick,
        color = if (top.urgent) Palette.AlertHighBackground else Palette.NoticeBackground,
        shape = RoundedCornerShape(6.du),
        modifier = modifier.fillMaxWidth(),
    ) { focused ->
        val color = when {
            focused -> Palette.FocusedContent
            top.urgent -> Palette.AlertForeground
            else -> Palette.NoticeText
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.du, vertical = 12.du),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.du),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.du),
            ) {
                BasicText(
                    top.title,
                    style = Type.condensed(color),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Box(Modifier.size(4.du).clip(CircleShape).background(color))
                BasicText(
                    if (notices.size == 1) "1 notice" else "${notices.size} notices",
                    style = Type.secondary(color),
                    maxLines = 1,
                )
            }
            Image(
                painter = painterResource(R.drawable.ic_arrow_forward_ios),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color),
                modifier = Modifier.size(24.du),
            )
        }
    }
}
