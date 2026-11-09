package com.mryav.compacttrip.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.data.model.DepartureCard
import com.mryav.compacttrip.data.model.RealtimeStatus
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/**
 * Quick View card (Figma 4:558 component, used on Home and Add trip). Focused: orange fill, white
 * text; line badges and the white countdown box keep their colours (Figma 22:759).
 * [now] is read only inside the countdown, so the 15 s ticker recomposes just that part.
 * With [onClick] = null (Add trip preview) the card isn't clickable: it's still focusable, so the
 * carousel can be scrolled with the D-pad, but focus shows only the inner shadow, no orange fill.
 * [compact] keeps the headsign to one line, for the shorter preview card (Figma 25:2749).
 */
@Composable
fun QuickViewCard(
    card: DepartureCard,
    now: State<Long>,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    /** 0..1: how much of the content is uncovered, top to bottom (entry animation). Read at draw time. */
    contentReveal: () -> Float = { 1f },
) {
    val cancelled = card.status == RealtimeStatus.Cancelled
    val shape = RoundedCornerShape(4.du)
    FocusSurface(
        onClick = onClick,
        fillOnFocus = onClick != null,
        color = if (cancelled) Palette.CardCancelled else Palette.CardSurface,
        shape = shape,
        modifier = modifier
            .height(CARD_HEIGHT.du)
            .widthIn(min = 180.du, max = 268.du)
            .border(1.du, Palette.CardBorder, shape),
    ) { focused ->
        val text = if (focused) Palette.FocusedContent else Palette.BodyText
        val secondary = when {
            focused -> Palette.FocusedContent
            card.platformChanged -> Palette.PlatformChanged
            else -> Palette.CardSecondaryText
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .drawWithContent {
                    val f = contentReveal()
                    if (f >= 1f) {
                        drawContent()
                    } else if (f > 0f) {
                        clipRect(bottom = size.height * f) { this@drawWithContent.drawContent() }
                    }
                }
                .padding(8.du),
            verticalArrangement = Arrangement.spacedBy(8.du),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.du, Alignment.CenterVertically),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.du)) {
                    card.badges.forEach { LineBadgeChip(it) }
                    if (cancelled) CancelledChip()
                }
                BasicText(
                    text = card.headsign,
                    style = Type.condensed24(text),
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 250.du),
                )
                val platformLine = listOfNotNull(card.platform, card.boardCars).joinToString(" • ")
                if (platformLine.isNotEmpty()) {
                    BasicText(
                        text = platformLine,
                        style = Type.condensed18(secondary, bold = card.platformChanged),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 250.du),
                    )
                }
            }
            CountdownBox(card = card, now = now, cancelled = cancelled)
        }
    }
}

@Composable
private fun CountdownBox(card: DepartureCard, now: State<Long>, cancelled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (cancelled) Palette.CardCancelled else Palette.Card, RoundedCornerShape(2.du))
            .padding(8.du),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!cancelled) CountdownText(departsAt = card.departsAt, status = card.status, now = now)
        Spacer(Modifier.weight(1f).width(10.du))
        BasicText(clockTime(card.departsAt), style = Type.cardTime(Palette.CardSecondaryText))
    }
}

const val CARD_HEIGHT = 175

/** "12 min" / "1 hr" / "Now", number coloured by realtime status. Reads [now] (15 s ticker). */
@Composable
fun CountdownText(departsAt: Long, status: RealtimeStatus, now: State<Long>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        val (number, unit) = countdown(minutesUntil(departsAt, now.value))
        BasicText(number, style = Type.countdown(statusColor(status)))
        if (unit != null) {
            Spacer(Modifier.width(2.du))
            BasicText(unit, style = Type.countdownUnit(Palette.CountdownUnit))
        }
    }
}

fun statusColor(status: RealtimeStatus) = when (status) {
    RealtimeStatus.OnTime -> Palette.OnTime
    RealtimeStatus.Delayed, RealtimeStatus.Cancelled -> Palette.Delayed
    RealtimeStatus.Unknown -> Palette.LiveUnknown
}

fun statusLabel(status: RealtimeStatus) = when (status) {
    RealtimeStatus.OnTime -> "On-time"
    RealtimeStatus.Delayed -> "Delayed"
    RealtimeStatus.Unknown -> "Real-time unknown"
    RealtimeStatus.Cancelled -> "Cancelled"
}
