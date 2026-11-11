package com.mryav.compacttrip.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.model.Journey
import com.mryav.compacttrip.data.model.JourneyLeg
import com.mryav.compacttrip.data.model.Notice
import com.mryav.compacttrip.data.model.RealtimeStatus
import com.mryav.compacttrip.data.model.SavedTrip
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.BackButton
import com.mryav.compacttrip.ui.components.CountdownText
import com.mryav.compacttrip.ui.components.Expandable
import com.mryav.compacttrip.ui.components.FocusSurface
import com.mryav.compacttrip.ui.components.LineBadgeChip
import com.mryav.compacttrip.ui.components.ModeIcon
import com.mryav.compacttrip.ui.components.clockTime
import com.mryav.compacttrip.ui.components.expandTween
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.label
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.components.statusColor
import com.mryav.compacttrip.ui.components.statusLabel
import com.mryav.compacttrip.ui.theme.LineColors
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du
import kotlinx.coroutines.launch

/** Walks longer than this get a chip in the summary (Figma 11:2255 annotation). */
private const val WALK_CHIP_MIN = 5

/**
 * Trip Details (Figma 11:2093, cards revised in 48:1301). Each leg is one focusable step so D-pad
 * DOWN walks the journey; a leg's inline notices and its stop list are focusable too.
 *
 * The journey reads as a story: a "Start at" card (or the first ride's heading) with the time,
 * each ride with how long to wait for it, which service to board and how late it is, the stops it
 * passes (expandable), each walk, and an "Arrive at" card with the arrival time.
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
        val scroll = rememberScrollState()
        val scope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .navigationBarsPadding()
                .padding(vertical = 16.du),
            verticalArrangement = Arrangement.spacedBy(2.du),
        ) {
            // Not clickable, but focusable so UP from the first leg scrolls back to the top (shadow-only
            // focus). Scrolls right to the start, so the margin above it shows again too.
            FocusSurface(
                onClick = null,
                fillOnFocus = false,
                color = Palette.Card,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) scope.launch { scroll.animateScrollTo(0) } },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.du, vertical = 20.du),
                    verticalArrangement = Arrangement.spacedBy(4.du),
                ) {
                    BasicText(trip.origin.name, style = Type.title(Palette.TitleText), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicText("To ${trip.destination.name}", style = Type.title(Palette.TitleText), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            JourneySummary(journey = journey, now = now)
            BasicText(
                "Directions",
                style = Type.label(Palette.BodyText),
                modifier = Modifier.padding(start = 8.du, end = 8.du, top = 8.du, bottom = 4.du),
            )
            Column {
                val legs = journey.legs
                // A journey that starts on foot gets its own Start card; one that starts on a service
                // says "Start at" on that service's card instead (Figma 48:1301, incl. the wrong example).
                val first = legs.firstOrNull()
                if (first is JourneyLeg.Walk) StartLeg(name = trip.origin.name, departsAt = first.departsAt)
                legs.forEachIndexed { index, leg ->
                    when (leg) {
                        is JourneyLeg.Ride -> RideLeg(
                            leg = leg,
                            notices = leg.noticeIds.mapNotNull(noticesById::get),
                            startHere = index == 0,
                            waitMinutes = waitMinutes(legs.getOrNull(index - 1), leg),
                            onOpenNotice = onOpenNotice,
                        )
                        is JourneyLeg.Walk -> WalkLeg(leg)
                    }
                }
                DestinationLeg(name = trip.destination.name, arrivesAt = journey.arrivesAt)
            }
        }
    }
}

/** Minutes standing between the previous leg's arrival and this ride's departure (null when unknown or none). */
private fun waitMinutes(previous: JourneyLeg?, ride: JourneyLeg.Ride): Int? {
    val arrived = previous?.arrivesAt ?: return null
    val minutes = ((ride.departsAt - arrived) / 60_000).toInt()
    return minutes.takeIf { it >= 1 }
}

/**
 * Countdown box + line/walk chips + "12:48pm —[41min]— 1:29pm". When the journey is running late
 * the duration chip and the arrival time turn red (Figma 48:1958), since both have moved.
 */
@Composable
private fun JourneySummary(journey: Journey, now: State<Long>) {
    val card = journey.card
    val delayed = card.status == RealtimeStatus.Delayed
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
                BasicText("Cancelled", style = Type.secondary(Palette.Delayed))
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
                BasicText(clockTime(card.departsAt), style = Type.secondary(Palette.CardSecondaryText))
                val arrives = journey.arrivesAt
                if (arrives != null) {
                    val late = if (delayed) Type.secondary(Palette.Delayed).copy(fontWeight = FontWeight.Bold) else null
                    Connector()
                    BasicText(
                        durationLabel(arrives - card.departsAt),
                        style = late ?: Type.secondary(Palette.CardSecondaryText),
                        modifier = Modifier
                            .border(1.du, if (delayed) Palette.Delayed else Palette.DurationChipBorder, RoundedCornerShape(5.du))
                            .padding(horizontal = 4.du, vertical = 4.du),
                    )
                    Connector()
                    BasicText(clockTime(arrives), style = late ?: Type.secondary(Palette.CardSecondaryText))
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
    // Not clickable: focusable only so the D-pad can scroll through the legs.
    FocusSurface(onClick = null, fillOnFocus = false, modifier = modifier.fillMaxWidth()) { focused ->
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(12.du).fillMaxHeight().background(borderColor))
            Box(Modifier.weight(1f)) { content(focused) }
        }
    }
}

@Composable
private fun RideLeg(
    leg: JourneyLeg.Ride,
    notices: List<Notice>,
    /** First leg of the journey: the heading reads "Start at …". */
    startHere: Boolean,
    waitMinutes: Int?,
    onOpenNotice: (Notice) -> Unit,
) {
    val legFocus = remember { FocusRequester() }
    val firstNotice = remember { FocusRequester() }
    val lastNotice = remember { FocusRequester() }
    val stopsFocus = remember { FocusRequester() }
    val expandable = leg.stopNames.isNotEmpty()
    LegFrame(
        borderColor = LineColors.of(leg.badge.label, leg.badge.mode),
        modifier = Modifier
            .focusRequester(legFocus)
            .focusProperties {
                down = when {
                    notices.isNotEmpty() -> firstNotice
                    expandable -> stopsFocus
                    else -> FocusRequester.Default
                }
            },
    ) { focused ->
        val block = if (focused) Color.Transparent else Palette.Card
        val text = if (focused) Palette.FocusedContent else Palette.BodyText
        Column(verticalArrangement = Arrangement.spacedBy(2.du)) {
            Column(
                modifier = Modifier.fillMaxWidth().background(block).padding(16.du),
                verticalArrangement = Arrangement.spacedBy(10.du),
            ) {
                if (waitMinutes != null) BasicText("Wait $waitMinutes min", style = Type.secondary(text))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.du)) {
                    BasicText(
                        if (startHere) "Start at ${leg.from}" else leg.from,
                        style = Type.condensed(text),
                        modifier = Modifier.weight(1f),
                    )
                    ModeIcon(leg.badge.mode, size = 40.du)
                }
                notices.forEachIndexed { index, notice ->
                    LegNotice(
                        notice = notice,
                        onClick = { onOpenNotice(notice) },
                        modifier = Modifier
                            .then(if (index == 0) Modifier.focusRequester(firstNotice).focusProperties { up = legFocus } else Modifier)
                            .then(if (index == notices.lastIndex) Modifier.focusRequester(lastNotice) else Modifier),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.du)) {
                    LineBadgeChip(leg.badge)
                    BasicText(
                        "Board ${leg.badge.mode.label().lowercase()}, ${leg.headsign}",
                        style = Type.secondary(if (focused) Palette.FocusedContent else Color.Black),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DepartureStatus(leg, focused)
            }
            StopsRow(
                leg = leg,
                block = block,
                legFocused = focused,
                modifier = Modifier
                    .focusRequester(stopsFocus)
                    .focusProperties { up = if (notices.isNotEmpty()) lastNotice else legFocus },
            )
            Column(
                modifier = Modifier.fillMaxWidth().background(block).padding(16.du),
                verticalArrangement = Arrangement.spacedBy(10.du),
            ) {
                BasicText(leg.to, style = Type.condensed(text))
                leg.arrivesAt?.let { BasicText(clockTime(it), style = Type.secondary(text)) }
            }
        }
    }
}

/** "12:48pm  On-time", or when late "1:07pm · 5 minutes late", both in red (Figma 48:1002). */
@Composable
private fun DepartureStatus(leg: JourneyLeg.Ride, focused: Boolean) {
    val text = if (focused) Palette.FocusedContent else Palette.BodyText
    val status = if (focused) Palette.FocusedContent else statusColor(leg.status)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.du)) {
        if (leg.status == RealtimeStatus.Delayed && leg.delayMinutes >= 1) {
            BasicText(clockTime(leg.departsAt), style = Type.secondary(status))
            Box(Modifier.size(3.du).clip(CircleShape).background(status))
            BasicText(
                "${leg.delayMinutes} ${if (leg.delayMinutes == 1) "minute" else "minutes"} late",
                style = Type.secondary(status),
            )
        } else {
            BasicText(clockTime(leg.departsAt), style = Type.secondary(text))
            BasicText(statusLabel(leg.status), style = Type.secondary(status))
        }
    }
}

/**
 * "Ride 3 stops, 10 min" with, when the planner listed them, a chevron that expands the stops
 * passed (Figma 48:857). Its own focus step inside the leg, so the D-pad can open it.
 */
@Composable
private fun StopsRow(leg: JourneyLeg.Ride, block: Color, legFocused: Boolean, modifier: Modifier = Modifier) {
    val label = "Ride ${leg.stops} ${if (leg.stops == 1) "stop" else "stops"}, ${leg.minutes} min"
    if (leg.stopNames.isEmpty()) {
        BasicText(
            label,
            style = Type.label(if (legFocused) Palette.FocusedContent else Palette.TitleText),
            modifier = Modifier.fillMaxWidth().background(block).padding(16.du),
        )
        return
    }
    var expanded by remember { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (expanded) 0f else 180f, expandTween(expanded), label = "chevron")
    FocusSurface(
        onClick = { expanded = !expanded },
        color = block,
        onClickLabel = if (expanded) "Hide stops" else "Show stops",
        modifier = modifier.fillMaxWidth(),
    ) { focused ->
        val text = if (focused || legFocused) Palette.FocusedContent else Palette.TitleText
        Column(modifier = Modifier.padding(horizontal = 16.du, vertical = 8.du)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.du)) {
                BasicText(label, style = Type.label(text), modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(R.drawable.ic_keyboard_arrow_up),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(text),
                    modifier = Modifier.size(40.du).rotate(chevron),
                )
            }
            Expandable(expanded = expanded) {
                Column(
                    modifier = Modifier.padding(vertical = 12.du),
                    verticalArrangement = Arrangement.spacedBy(12.du),
                ) {
                    leg.stopNames.forEach { BasicText(it, style = Type.secondary(text)) }
                }
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
                style = Type.secondary(color),
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

/** "Walk 3 min" with the walking icon on the right, in line with the ride cards' mode icons. */
@Composable
private fun WalkLeg(leg: JourneyLeg.Walk) {
    LegFrame(borderColor = Palette.WalkLegBorder) { focused ->
        val text = if (focused) Palette.FocusedContent else Palette.BodyText
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.du),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.du),
        ) {
            BasicText("Walk ${leg.minutes} min", style = Type.condensed(text), modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.ic_directions_walk),
                contentDescription = null,
                colorFilter = ColorFilter.tint(text),
                modifier = Modifier.size(40.du),
            )
        }
    }
}

/** "Start at Bondi Beach  1:52pm ○": where the journey begins, when it begins on foot (Figma 48:1155). */
@Composable
private fun StartLeg(name: String, departsAt: Long?) {
    EndpointLeg(title = "Start at $name", time = departsAt, icon = R.drawable.ic_trip_origin)
}

/** "Arrive at Newington  1:52pm ⌾" (Figma 48:1019). */
@Composable
private fun DestinationLeg(name: String, arrivesAt: Long?) {
    EndpointLeg(title = "Arrive at $name", time = arrivesAt, icon = R.drawable.ic_location_on)
}

@Composable
private fun EndpointLeg(title: String, time: Long?, icon: Int) {
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.du)) {
                BasicText(title, style = Type.condensed(text))
                if (time != null) BasicText(clockTime(time), style = Type.secondary(text))
            }
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(text),
                modifier = Modifier.size(40.du),
            )
        }
    }
}
