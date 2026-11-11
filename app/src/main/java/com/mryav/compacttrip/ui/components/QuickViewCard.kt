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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
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
            .height(quickViewCardHeight(compact))
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
                    style = Type.headsign(text),
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = HEADSIGN_MAX_WIDTH.du),
                )
                val platformLine = listOfNotNull(card.platform, card.boardCars).joinToString(" • ")
                if (platformLine.isNotEmpty()) {
                    BasicText(
                        text = platformLine,
                        style = Type.caption(secondary, bold = card.platformChanged),
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

/** The shorter one-line card (Add trip preview, Figma 25:2749). */
const val COMPACT_CARD_HEIGHT = 155

/** Headsigns wrap (to two lines at most) past this width. */
const val HEADSIGN_MAX_WIDTH = 250

/**
 * Height of a Quick View card: the design's [CARD_HEIGHT] (or [COMPACT_CARD_HEIGHT]), or more if the
 * text needs it. Text is sized in sp while the card's chrome follows the design-unit scale, so on a
 * small screen or with a large font size the content can outgrow the design height; the card then
 * grows to fit rather than cropping. [headsignLines] is 2 when a headsign in the carousel wraps.
 */
@Composable
fun quickViewCardHeight(compact: Boolean, headsignLines: Int = 1): Dp {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 8)
    val badge = Type.badge(Color.Unspecified)
    val headsign = Type.headsign(Color.Unspecified)
    val caption = Type.caption(Color.Unspecified)
    val countdown = Type.countdown(Color.Unspecified)
    val design = (if (compact) COMPACT_CARD_HEIGHT else CARD_HEIGHT).du
    val chrome = 8.du * 2 + 4.du * 2 + 4.du + 4.du + 8.du + 8.du * 2 + 2.du
    return remember(density, measurer, badge, headsign, caption, countdown, design, chrome, headsignLines) {
        fun lineHeight(style: TextStyle) = with(density) { measurer.measure("Ag", style).size.height.toDp() }
        val content = chrome + lineHeight(badge) + lineHeight(headsign) * headsignLines + lineHeight(caption) + lineHeight(countdown)
        max(design, content)
    }
}

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
