package com.mryav.compacttrip.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.model.Journey
import com.mryav.compacttrip.data.model.JourneyLeg
import com.mryav.compacttrip.data.model.Notice
import com.mryav.compacttrip.data.model.RealtimeStatus
import com.mryav.compacttrip.data.model.SavedTrip
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.BackButton
import com.mryav.compacttrip.ui.components.CountdownText
import com.mryav.compacttrip.ui.components.FocusSurface
import com.mryav.compacttrip.ui.components.LineBadgeChip
import com.mryav.compacttrip.ui.components.ModeIcon
import com.mryav.compacttrip.ui.components.clockTime
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.components.statusColor
import com.mryav.compacttrip.ui.components.statusLabel
import com.mryav.compacttrip.ui.theme.LineColors
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/** Walks longer than this get a chip in the summary (Figma 11:2255 annotation). */
private const val WALK_CHIP_MIN = 5

/**
 * Trip Details (Figma 11:2093). Each leg is one focusable step so D-pad DOWN walks the journey;
 * a leg's inline notices are focusable too and open the Service Alert.
 */
@Composable
fun TripDetailsScreen(
    trip: SavedTrip,
    journey: Journey,
    notices: List<Notice>,
    now: State<Long>,
    onBack: () -> Unit,
    onOpenNotice: (Notice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val noticesById = remember(notices) { notices.associateBy { it.id } }
    val initialFocus = rememberInitialFocus()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Page)
            .focusEntry(initialFocus),
    ) {
        AppBar(
            title = "Trip Details",
            navigation = { BackButton(onClick = onBack, modifier = Modifier.focusRequester(initialFocus)) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(vertical = 16.du),
            verticalArrangement = Arrangement.spacedBy(2.du),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Palette.Card)
                    .padding(horizontal = 16.du, vertical = 20.du),
                verticalArrangement = Arrangement.spacedBy(4.du),
            ) {
                BasicText(trip.origin.name, style = Type.title(Palette.TitleText), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText("To ${trip.destination.name}", style = Type.title(Palette.TitleText), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            JourneySummary(journey = journey, now = now)
            BasicText(
                "Directions",
                style = Type.condensed24(Palette.BodyText),
                modifier = Modifier.padding(start = 8.du, end = 8.du, top = 8.du, bottom = 4.du),
            )
            Column {
                journey.legs.forEach { leg ->
                    when (leg) {
                        is JourneyLeg.Ride -> RideLeg(leg, leg.noticeIds.mapNotNull(noticesById::get), onOpenNotice)
                        is JourneyLeg.Walk -> WalkLeg(leg)
                    }
                }
                DestinationLeg(trip.destination.name)
            }
        }
    }
}

/** Countdown box + line/walk chips + "12:48pm —[41min]— 1:29pm". */
@Composable
private fun JourneySummary(journey: Journey, now: State<Long>) {
    val card = journey.card
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Palette.Card)
            .height(IntrinsicSize.Min)
            .padding(16.du),
        horizontalArrangement = Arrangement.spacedBy(10.du),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .border(4.du, Palette.Page, RoundedCornerShape(5.du))
                .padding(horizontal = 12.du, vertical = 16.du),
            contentAlignment = Alignment.Center,
        ) {
            if (card.status == RealtimeStatus.Cancelled) {
                BasicText("Cancelled", style = Type.condensedSmall(Palette.Delayed))
            } else {
                CountdownText(departsAt = card.departsAt, status = card.status, now = now)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.du)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.du), verticalAlignment = Alignment.CenterVertically) {
                journey.legs.forEach { leg ->
                    when (leg) {
                        is JourneyLeg.Ride -> LineBadgeChip(leg.badge, large = true)
                        is JourneyLeg.Walk -> if (leg.minutes > WALK_CHIP_MIN) WalkChip()
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(clockTime(card.departsAt), style = Type.condensedSmall(Palette.CardSecondaryText))
                val arrives = journey.arrivesAt
                if (arrives != null) {
                    Connector()
                    BasicText(
                        durationLabel(arrives - card.departsAt),
                        style = Type.condensedSmall(Palette.CardSecondaryText),
                        modifier = Modifier
                            .border(1.du, Palette.DurationChipBorder, RoundedCornerShape(5.du))
                            .padding(horizontal = 4.du, vertical = 4.du),
                    )
                    Connector()
                    BasicText(clockTime(arrives), style = Type.condensedSmall(Palette.CardSecondaryText))
                }
            }
        }
    }
}

@Composable
private fun Connector() {
    Box(Modifier.padding(horizontal = 4.du).width(16.du).height(1.du).background(Palette.CardSecondaryText))
}

@Composable
private fun WalkChip() {
    Box(
        modifier = Modifier
            .border(1.du, Palette.WalkChipBorder, RoundedCornerShape(4.du))
            .padding(horizontal = 6.du, vertical = 4.du),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_directions_walk),
            contentDescription = "Walk",
            colorFilter = ColorFilter.tint(Palette.BodyText),
            modifier = Modifier.size(20.du),
        )
    }
}

/** "41min", "1h 5min". */
private fun durationLabel(millis: Long): String {
    val minutes = ((millis + 59_999) / 60_000).toInt().coerceAtLeast(0)
    return if (minutes < 60) "${minutes}min" else "${minutes / 60}h ${minutes % 60}min"
}

/**
 * A leg: 12-du left border in [borderColor], content to its right. Focused as one step; being a
 * large card, focus shows only the inner shadow, no orange fill (content stays as is).
 */
@Composable
private fun LegFrame(
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable (focused: Boolean) -> Unit,
) {
    FocusSurface(onClick = {}, fillOnFocus = false, modifier = modifier.fillMaxWidth()) { focused ->
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(12.du).fillMaxHeight().background(borderColor))
            Box(Modifier.weight(1f)) { content(focused) }
        }
    }
}

@Composable
private fun RideLeg(leg: JourneyLeg.Ride, notices: List<Notice>, onOpenNotice: (Notice) -> Unit) {
    val legFocus = remember { FocusRequester() }
    val firstNotice = remember { FocusRequester() }
    LegFrame(
        borderColor = LineColors.of(leg.badge.label, leg.badge.mode),
        modifier = Modifier
            .focusRequester(legFocus)
            .focusProperties { if (notices.isNotEmpty()) down = firstNotice },
    ) { focused ->
        val block = if (focused) Color.Transparent else Palette.Card
        val text = if (focused) Palette.FocusedContent else Palette.BodyText
        Column(verticalArrangement = Arrangement.spacedBy(2.du)) {
            Column(
                modifier = Modifier.fillMaxWidth().background(block).padding(16.du),
                verticalArrangement = Arrangement.spacedBy(10.du),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.du)) {
                    BasicText(leg.from, style = Type.condensed(text), modifier = Modifier.weight(1f))
                    ModeIcon(leg.badge.mode, size = 40.du)
                }
                notices.forEachIndexed { index, notice ->
                    LegNotice(
                        notice = notice,
                        onClick = { onOpenNotice(notice) },
                        modifier = Modifier
                            .then(if (index == 0) Modifier.focusRequester(firstNotice) else Modifier)
                            .then(if (index == 0) Modifier.focusProperties { up = legFocus } else Modifier),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.du)) {
                    LineBadgeChip(leg.badge)
                    BasicText(
                        leg.headsign,
                        style = Type.condensedSmall(if (focused) Palette.FocusedContent else Color.Black),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.du)) {
                    BasicText(clockTime(leg.departsAt), style = Type.condensedSmall(text))
                    BasicText(
                        statusLabel(leg.status),
                        style = Type.condensedSmall(if (focused) Palette.FocusedContent else statusColor(leg.status)),
                    )
                }
            }
            BasicText(
                "Ride ${leg.stops} ${if (leg.stops == 1) "stop" else "stops"}, ${leg.minutes} min",
                style = Type.medium20(if (focused) Palette.FocusedContent else Palette.TitleText),
                modifier = Modifier.fillMaxWidth().background(block).padding(16.du),
            )
            Column(
                modifier = Modifier.fillMaxWidth().background(block).padding(16.du),
                verticalArrangement = Arrangement.spacedBy(10.du),
            ) {
                BasicText(leg.to, style = Type.condensed(text))
                leg.arrivesAt?.let { BasicText(clockTime(it), style = Type.condensedSmall(text)) }
            }
        }
    }
}

/** Grey inline alert inside a ride leg ("Some trains make extra stops…  >"). */
@Composable
private fun LegNotice(notice: Notice, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusSurface(
        onClick = onClick,
        color = Palette.Page,
        shape = RoundedCornerShape(4.du),
        modifier = modifier.fillMaxWidth(),
    ) { focused ->
        val color = when {
            focused -> Palette.FocusedContent
            notice.urgent -> Palette.AlertForeground
            else -> Palette.BodyText
        }
        Row(
            modifier = Modifier.padding(12.du),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.du),
        ) {
            BasicText(
                notice.title,
                style = Type.condensedSmall(color).copy(letterSpacing = 0.em),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Image(
                painter = painterResource(R.drawable.ic_arrow_forward_ios),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color),
                modifier = Modifier.size(24.du),
            )
        }
    }
}

@Composable
private fun WalkLeg(leg: JourneyLeg.Walk) {
    LegFrame(borderColor = Palette.WalkLegBorder) { focused ->
        val text = if (focused) Palette.FocusedContent else Palette.BodyText
        Column(modifier = Modifier.padding(16.du), verticalArrangement = Arrangement.spacedBy(10.du)) {
            Image(
                painter = painterResource(R.drawable.ic_directions_walk),
                contentDescription = null,
                colorFilter = ColorFilter.tint(text),
                modifier = Modifier.size(24.du),
            )
            BasicText("Walk ${leg.minutes} min", style = Type.condensed(text))
        }
    }
}

@Composable
private fun DestinationLeg(name: String) {
    LegFrame(borderColor = Palette.DestinationLegBorder) { focused ->
        val text = if (focused) Palette.FocusedContent else Palette.BodyText
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (focused) Color.Transparent else Palette.Card)
                .padding(16.du),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.du),
        ) {
            BasicText(name, style = Type.condensed(text), modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.ic_location_on),
                contentDescription = null,
                colorFilter = ColorFilter.tint(text),
                modifier = Modifier.size(40.du),
            )
        }
    }
}
