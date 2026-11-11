package com.mryav.compacttrip.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/** Search result: name (wraps for long names, Figma 4:2521), locality, mode icons. */
@Composable
fun StopRow(stop: Stop, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusSurface(onClick = onClick, color = Palette.Card, modifier = modifier.fillMaxWidth()) { focused ->
        val text = if (focused) Palette.FocusedContent else Palette.TitleText
        Row(
            modifier = Modifier.padding(horizontal = 16.du, vertical = 20.du),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.du),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.du)) {
                BasicText(stop.name, style = Type.title(text))
                if (stop.locality != null) {
                    BasicText(stop.locality, style = Type.label(text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.du)) {
                stop.modes.iconModes().forEach { ModeIcon(it, size = 35.du) }
            }
        }
    }
}
