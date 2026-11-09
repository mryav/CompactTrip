package com.mryav.compacttrip.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import com.mryav.compacttrip.data.model.Journey
import com.mryav.compacttrip.data.model.RealtimeStatus
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/**
 * One journey in View Services (Figma 6:600): badges, headsign, "dep — arr", platform + status,
 * countdown box. Focused: orange, white text; badges and the countdown box keep their colours.
 */
@Composable
fun ServiceRow(journey: Journey, now: State<Long>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val card = journey.card
    FocusSurface(onClick = onClick, color = Palette.Card, modifier = modifier.fillMaxWidth()) { focused ->
        val primary = if (focused) Palette.FocusedContent else Palette.BodyText
        val secondary = if (focused) Palette.FocusedContent else Palette.CardSecondaryText
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(16.du),
            horizontalArrangement = Arrangement.spacedBy(12.du),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.du)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.du)) {
                    card.badges.forEach { LineBadgeChip(it) }
                    if (card.status == RealtimeStatus.Cancelled) CancelledChip()
                }
                BasicText(
                    card.headsign,
                    style = Type.condensed(primary).copy(letterSpacing = (-0.02).em),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(vertical = 4.du),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.du),
                ) {
                    BasicText(clockTime(card.departsAt), style = Type.condensedSmall(secondary))
                    if (journey.arrivesAt != null) {
                        Box(Modifier.width(16.du).height(1.du).background(secondary))
                        BasicText(clockTime(journey.arrivesAt), style = Type.condensedSmall(secondary))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.du)) {
                    card.platform?.let {
                        BasicText(
                            it,
                            style = Type.condensedSmall(
                                when {
                                    focused -> Palette.FocusedContent
                                    card.platformChanged -> Palette.PlatformChanged
                                    else -> Palette.PlatformText
                                },
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    BasicText(
                        statusLabel(card.status),
                        style = Type.condensedSmall(if (focused) Palette.FocusedContent else statusColor(card.status)),
                        maxLines = 1,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(104.du)
                    .fillMaxHeight()
                    .background(Palette.CardSurface, RoundedCornerShape(5.du))
                    .border(1.du, Palette.CardBorder, RoundedCornerShape(5.du))
                    .padding(8.du),
                contentAlignment = Alignment.Center,
            ) {
                if (card.status != RealtimeStatus.Cancelled) {
                    CountdownText(departsAt = card.departsAt, status = card.status, now = now)
                }
            }
        }
    }
}
