package com.mryav.compacttrip.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.model.RouteFor
import com.mryav.compacttrip.data.model.Timing
import com.mryav.compacttrip.data.model.TravelMode
import com.mryav.compacttrip.data.model.TripPreferences
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.BackButton
import com.mryav.compacttrip.ui.components.DoneAction
import com.mryav.compacttrip.ui.components.FocusSurface
import com.mryav.compacttrip.ui.components.ModeIcon
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/** Days offered for a planned time: today plus the next six. */
private const val DAYS_AHEAD = 7
private const val MINUTE_STEP = 5

/** Which of the time boxes a pick list is open for. */
private enum class TimePart { Day, Hour, Minute }

/**
 * Trip preferences (Figma 27:3020). Changes apply on Done; Back discards them.
 * Timing: now, leave at or arrive by a day/hour/minute (each box opens a pick list). Accessibility,
 * allowed modes (at least one) and route type. [showTiming] = false for a trip on Home, which always
 * shows what's leaving now.
 */
@Composable
fun TripPreferencesScreen(
    initial: TripPreferences,
    showTiming: Boolean,
    now: () -> LocalDateTime,
    onBack: () -> Unit,
    onDone: (TripPreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf(initial) }
    var picking by remember { mutableStateOf<TimePart?>(null) }
    val today = remember { now().toLocalDate() }
    val initialFocus = rememberInitialFocus()
    val pickFocus = remember { FocusRequester() }
    // Each time box, so focus returns to the one that opened the list when it closes.
    val boxFocus = remember { TimePart.entries.associateWith { FocusRequester() } }
    var reopenedFrom by remember { mutableStateOf<TimePart?>(null) }
    // The time boxes sit inside their timing row, so D-pad DOWN from the row would skip past them:
    // wire row → Day box and box → row explicitly.
    val timingRowFocus = remember { mapOf(Timing.LeaveAt to FocusRequester(), Timing.ArriveBy to FocusRequester()) }
    LaunchedEffect(picking) {
        val from = reopenedFrom
        if (picking != null || from == null) return@LaunchedEffect
        repeat(10) {
            withFrameNanos { }
            if (runCatching { boxFocus.getValue(from).requestFocus(FocusDirection.Enter) }.getOrDefault(false)) return@LaunchedEffect
        }
    }

    fun chooseTiming(timing: Timing) {
        draft = draft.copy(timing = timing, at = if (timing == Timing.Now) null else draft.at ?: nextSlot(now()))
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Palette.Page)
                // While a pick list is open, focus belongs in it. After a tap, Android leaves touch mode
                // on the first key press and focus re-enters from the top: send it to the list, not Back.
                .focusProperties { onEnter = { (if (picking != null) pickFocus else initialFocus).requestFocus() } }
                .focusGroup()
                // And if focus is somehow still back here, the next key moves it into the list.
                .onPreviewKeyEvent { event ->
                    if (picking == null) return@onPreviewKeyEvent false
                    // Key down and up: the first press after a touch only leaves touch mode on key down.
                    runCatching { pickFocus.requestFocus() }
                    true
                },
        ) {
            AppBar(
                title = "Trip preferences",
                navigation = { BackButton(onClick = onBack, modifier = Modifier.focusRequester(initialFocus)) },
            ) {
                DoneAction(enabled = draft.modes.isNotEmpty(), onClick = { onDone(draft) })
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(vertical = 16.du),
                verticalArrangement = Arrangement.spacedBy(2.du),
            ) {
                if (showTiming) {
                    SectionLabel("Timing")
                    OptionRow(
                        selected = draft.timing == Timing.Now,
                        icon = radioIcon(draft.timing == Timing.Now),
                        title = "Now",
                        subtitle = "Show trips leaving right now",
                        onClick = { chooseTiming(Timing.Now) },
                    )
                    for (timing in listOf(Timing.LeaveAt, Timing.ArriveBy)) {
                        val selected = draft.timing == timing
                        val at = draft.at
                        OptionRow(
                            selected = selected,
                            icon = radioIcon(selected),
                            title = if (timing == Timing.LeaveAt) "Leave at…" else "Arrive by…",
                            subtitle = when {
                                selected && at != null && timing == Timing.LeaveAt -> "Show services that depart ${dayPhrase(at, today)}at ${TripPreferences.shortTime(at)}"
                                selected && at != null -> "Show services that arrive ${dayPhrase(at, today)}by ${TripPreferences.shortTime(at)}"
                                timing == Timing.LeaveAt -> "Show services that depart at a specific time."
                                else -> "Show services that arrive at destination by a specific time."
                            },
                            onClick = { if (!selected) chooseTiming(timing) },
                            modifier = Modifier
                                .focusRequester(timingRowFocus.getValue(timing))
                                .focusProperties { if (selected) down = boxFocus.getValue(TimePart.Day) },
                        ) {
                            if (selected && at != null) {
                                TimeBoxes(
                                    at = at,
                                    today = today,
                                    focus = boxFocus,
                                    up = timingRowFocus.getValue(timing),
                                    onPick = {
                                        reopenedFrom = it
                                        picking = it
                                    },
                                )
                            }
                        }
                    }

                }

                SectionLabel("Accessibility")
                OptionRow(
                    selected = draft.accessibleOnly,
                    icon = checkIcon(draft.accessibleOnly),
                    title = "Accessible services only",
                    subtitle = "Only show wheelchair accessible services and stops",
                    onClick = { draft = draft.copy(accessibleOnly = !draft.accessibleOnly) },
                )

                SectionLabel(if (draft.modes.isEmpty()) "Allowed modes: choose at least one" else "Allowed modes")
                TravelMode.entries.forEach { mode ->
                    val on = mode in draft.modes
                    OptionRow(
                        selected = on,
                        icon = checkIcon(on),
                        title = mode.label,
                        leading = { ModeIcon(mode.icon, size = 31.du) },
                        onClick = { draft = draft.copy(modes = if (on) draft.modes - mode else draft.modes + mode) },
                    )
                }

                SectionLabel("Route for")
                RouteFor.entries.forEach { route ->
                    val on = draft.routeFor == route
                    OptionRow(
                        selected = on,
                        icon = radioIcon(on),
                        title = route.label,
                        onClick = { draft = draft.copy(routeFor = route) },
                    )
                }
            }
        }

        val at = draft.at
        val part = picking
        if (part != null && at != null) {
            val choices = timeChoices(part, at, today)
            PickList(
                focus = pickFocus,
                title = part.name,
                options = choices.map { it.label },
                selected = choices.indexOfFirst { it.selected }.coerceAtLeast(0),
                onPick = { i ->
                    draft = draft.copy(at = choices[i].value)
                    picking = null
                },
                onDismiss = { picking = null },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    BasicText(
        text,
        style = Type.condensed24(Palette.BodyText),
        modifier = Modifier.padding(start = 8.du, end = 8.du, top = 8.du, bottom = 4.du),
    )
}

/**
 * Dark option row (Figma 27:3020): radio/check icon, optional mode icon, title, subtitle. Unselected
 * rows are dimmed to 80%. [below] sits under the text (the time boxes).
 */
@Composable
private fun OptionRow(
    selected: Boolean,
    @DrawableRes icon: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
) {
    FocusSurface(
        onClick = onClick,
        color = if (selected) Palette.ActionRow else Palette.ActionRow.copy(alpha = 0.8f),
        modifier = modifier.fillMaxWidth(),
    ) { focused ->
        Row(
            modifier = Modifier
                .padding(horizontal = 16.du, vertical = 20.du)
                .alpha(if (selected || focused) 1f else 0.8f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.du),
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Palette.OnDark),
                modifier = Modifier.size(40.du),
            )
            leading?.invoke()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.du)) {
                BasicText(title, style = Type.row(Palette.OnDark), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) BasicText(subtitle, style = Type.condensedSmall(Palette.OnDarkSubtle))
                below?.invoke()
            }
        }
    }
}

/** Day / Hour / Minute boxes (Figma 27:3192). Each opens a pick list. */
@Composable
private fun TimeBoxes(
    at: LocalDateTime,
    today: LocalDate,
    focus: Map<TimePart, FocusRequester>,
    up: FocusRequester,
    onPick: (TimePart) -> Unit,
) {
    fun Modifier.box(part: TimePart) = focusRequester(focus.getValue(part)).focusProperties { this.up = up }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.du)
            .clip(RoundedCornerShape(4.du)),
        horizontalArrangement = Arrangement.spacedBy(2.du),
    ) {
        TimeBox("Day", dayName(at.toLocalDate(), today), Modifier.weight(1f).box(TimePart.Day)) { onPick(TimePart.Day) }
        TimeBox("Hour", at.hour.toString(), Modifier.weight(1f).box(TimePart.Hour)) { onPick(TimePart.Hour) }
        TimeBox("Minute", at.minute.toString(), Modifier.weight(1f).box(TimePart.Minute)) { onPick(TimePart.Minute) }
    }
}

@Composable
private fun TimeBox(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    FocusSurface(onClick = onClick, color = Palette.TimeBox, modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 16.du, vertical = 12.du),
            verticalArrangement = Arrangement.spacedBy(4.du),
        ) {
            BasicText(label, style = Type.condensedSmall(Palette.OnDarkSubtle), maxLines = 1)
            BasicText(value, style = Type.row(Palette.OnDark), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Pick list over the screen (the ⋮ menu's style): radio rows, focus starts on the current value,
 * OK picks, Back cancels. Focus stays inside while it's open.
 */
@Composable
private fun PickList(
    focus: FocusRequester,
    title: String,
    options: List<String>,
    selected: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val current = focus
    val focusManager = LocalFocusManager.current
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (selected - 1).coerceAtLeast(0))
    // The rows are laid out lazily, so wait for a frame (or a few) before focusing the current one.
    LaunchedEffect(Unit) {
        // Drop focus from the screen behind (in touch mode the request below can't take it), so the
        // first D-pad press enters the list rather than restoring Back.
        focusManager.clearFocus(force = true)
        repeat(10) {
            withFrameNanos { }
            if (runCatching { current.requestFocus(FocusDirection.Enter) }.getOrDefault(false)) return@LaunchedEffect
        }
    }
    BackHandler(onBack = onDismiss)
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.7f).dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            // A tap outside closes it. Not `clickable`: that would make the backdrop focusable.
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .pointerInput(Unit) { detectTapGestures { } } // taps on the sheet itself don't close it
                .background(Palette.Page)
                .navigationBarsPadding()
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup(),
        ) {
            BasicText(
                title,
                style = Type.condensed24(Palette.BodyText),
                modifier = Modifier.padding(start = 16.du, end = 16.du, top = 16.du, bottom = 8.du),
            )
            LazyColumn(state = list, verticalArrangement = Arrangement.spacedBy(2.du)) {
                itemsIndexed(options) { i, option ->
                    OptionRow(
                        selected = i == selected,
                        icon = radioIcon(i == selected),
                        title = option,
                        onClick = { onPick(i) },
                        modifier = if (i == selected) Modifier.focusRequester(current) else Modifier,
                    )
                }
            }
        }
    }
}

@Immutable
private data class TimeChoice(val label: String, val value: LocalDateTime, val selected: Boolean)

private fun timeChoices(part: TimePart, at: LocalDateTime, today: LocalDate): List<TimeChoice> = when (part) {
    TimePart.Day -> (0 until DAYS_AHEAD).map { d ->
        val day = today.plusDays(d.toLong())
        TimeChoice(dayName(day, today), LocalDateTime.of(day, at.toLocalTime()), day == at.toLocalDate())
    }
    TimePart.Hour -> (0..23).map { h ->
        val value = at.withHour(h)
        TimeChoice("$h  (${TripPreferences.shortTime(value.withMinute(0))})", value, h == at.hour)
    }
    TimePart.Minute -> (0 until 60 step MINUTE_STEP).map { m ->
        TimeChoice(m.toString(), at.withMinute(m), m == at.minute)
    }
}

/** The next 5-minute slot from [now], e.g. 1:02pm → 1:05pm. */
private fun nextSlot(now: LocalDateTime): LocalDateTime {
    val base = now.withSecond(0).withNano(0)
    val extra = (MINUTE_STEP - base.minute % MINUTE_STEP) % MINUTE_STEP
    return base.plusMinutes(extra.toLong())
}

private fun dayName(day: LocalDate, today: LocalDate): String = when (day) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    else -> day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
}

/** "" today, "tomorrow ", "Friday ". */
private fun dayPhrase(at: LocalDateTime, today: LocalDate): String = when (at.toLocalDate()) {
    today -> ""
    today.plusDays(1) -> "tomorrow "
    else -> at.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " "
}

@DrawableRes
private fun radioIcon(on: Boolean) = if (on) R.drawable.ic_radio_button_checked else R.drawable.ic_radio_button_unchecked

@DrawableRes
private fun checkIcon(on: Boolean) = if (on) R.drawable.ic_check_box else R.drawable.ic_check_box_outline_blank
