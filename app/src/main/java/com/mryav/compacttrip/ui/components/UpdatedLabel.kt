package com.mryav.compacttrip.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/** "Updated 30s ago" (right-aligned label). Shows "Updating…" before the first data arrives. */
@Composable
fun UpdatedLabel(updatedAt: Long?, refreshing: Boolean, now: State<Long>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.du, end = 8.du, top = 8.du, bottom = 4.du),
        contentAlignment = Alignment.CenterEnd,
    ) {
        val text = when {
            updatedAt == null -> if (refreshing) "Updating…" else ""
            else -> "Updated ${updatedAgo(updatedAt, now.value)}"
        }
        BasicText(text, style = Type.label(Palette.BodyText))
    }
}
