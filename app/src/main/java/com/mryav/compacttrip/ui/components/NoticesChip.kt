package com.mryav.compacttrip.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.model.Notice
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/**
 * Notices chip under a trip's title in View Services (Figma 48:1864). With a high/veryHigh notice
 * it's red: "Service disruption   2 more notices  >" over the top notice's headline. Otherwise
 * it's blue and just says "3 notices  >".
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
        Column(
            modifier = Modifier.padding(horizontal = 16.du, vertical = 12.du),
            verticalArrangement = Arrangement.spacedBy(8.du),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.du)) {
                val n = notices.size
                if (top.urgent) {
                    BasicText("Service disruption", style = Type.condensed(color), maxLines = 1, modifier = Modifier.weight(1f))
                    if (n > 1) {
                        BasicText(if (n == 2) "1 more notice" else "${n - 1} more notices", style = Type.secondary(color), maxLines = 1)
                    }
                } else {
                    BasicText(if (n == 1) "1 notice" else "$n notices", style = Type.condensed(color), maxLines = 1, modifier = Modifier.weight(1f))
                }
                Image(
                    painter = painterResource(R.drawable.ic_arrow_forward_ios),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color),
                    modifier = Modifier.size(24.du),
                )
            }
            if (top.urgent) {
                BasicText(top.title, style = Type.secondary(color), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
