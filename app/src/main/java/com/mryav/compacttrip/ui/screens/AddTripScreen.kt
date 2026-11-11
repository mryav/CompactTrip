package com.mryav.compacttrip.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.ApiResult
import com.mryav.compacttrip.data.RetryStatus
import com.mryav.compacttrip.data.loadWithRetry
import com.mryav.compacttrip.data.retryingIn
import com.mryav.compacttrip.data.model.SavedTrip
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.data.model.TripPreferences
import com.mryav.compacttrip.data.model.TripSummary
import com.mryav.compacttrip.ui.components.ActionRow
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.BackButton
import com.mryav.compacttrip.ui.components.CollapsingSwap
import com.mryav.compacttrip.ui.components.DepartureCarousel
import com.mryav.compacttrip.ui.components.DepartureLoadingPlaceholder
import com.mryav.compacttrip.ui.components.DoneAction
import com.mryav.compacttrip.ui.components.FocusSurface
import com.mryav.compacttrip.ui.components.Motion
import com.mryav.compacttrip.ui.components.NextAction
import com.mryav.compacttrip.ui.components.PlaceRow
import com.mryav.compacttrip.ui.components.StatusMessage
import com.mryav.compacttrip.ui.components.StatusSpinner
import com.mryav.compacttrip.ui.components.SurfaceIcon
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.foregroundShadow
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.components.revealDurationMs
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/** What the preview block shows (Figma 25:2749). */
@Immutable
private sealed interface Preview {
    /** First load: placeholder card, which morphs into the carousel. */
    data object Loading : Preview
    data class Loaded(val summary: TripSummary) : Preview
    /** Trying again after an error: the supplemental spinner block. */
    data object Retrying : Preview
    data class Failed(val retryInSeconds: Int) : Preview
    data object Offline : Preview
    data object NoTrips : Preview
    /** No service, but the planner says to walk (Figma 25:2761). */
    data class Walking(val minutes: Int) : Preview
    data object KeyProblem : Preview
}

/** Loading and Loaded share the white carousel block; the placeholder morphs into the cards. */
private fun Preview.kind(): Any = if (this is Preview.Loaded) Preview.Loading::class else this::class

/**
 * Add trip to home (Figma 4:1614 empty; 25:2749 preview states; 48:1723 hint and swap). Until both
 * stops are set, a hint says what the screen is for. Origin/Destination open the stop search; once
 * both are set a header with the two names (and a swap button, [onSwap]) and a compact preview of
 * the next departures appear. Done saves the trip whether or not the preview loads.
 *
 * Widget setup reuses it as "Select route" (Figma 30:4865, 48:2187): [title], [previewLabel] and
 * a "Next" header action ([next]) instead of Done, and [suggestions] (the trips already on Home)
 * in place of the hint, each a one-tap route.
 */
@Composable
fun AddTripScreen(
    origin: Stop?,
    destination: Stop?,
    preferences: TripPreferences,
    loadPreview: suspend (Stop, Stop, TripPreferences) -> ApiResult<TripSummary>,
    awaitOnline: suspend () -> Unit,
    now: State<Long>,
    onBack: () -> Unit,
    onPickOrigin: () -> Unit,
    onPickDestination: () -> Unit,
    onPreferences: () -> Unit,
    onDone: () -> Unit,
    /** Swap origin and destination (shown once both are set). */
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Add trip to home",
    previewLabel: String = "Preview",
    hint: String = "Choose a starting point and destination to pin a trip to your home page.",
    next: Boolean = false,
    suggestions: List<SavedTrip> = emptyList(),
    onSuggestion: (SavedTrip) -> Unit = {},
) {
    val ready = origin != null && destination != null && origin.id != destination.id
    // Reset as soon as the stops change, so the previous trip's preview never shows for a frame.
    var preview by remember(origin?.id, destination?.id, preferences) { mutableStateOf<Preview>(Preview.Loading) }

    LaunchedEffect(origin?.id, destination?.id, preferences) {
        if (!ready) return@LaunchedEffect
        val r = loadWithRetry(
            load = { loadPreview(origin, destination, preferences) },
            awaitOnline = awaitOnline,
            onStatus = { status ->
                preview = when (status) {
                    RetryStatus.Offline -> Preview.Offline
                    is RetryStatus.Waiting -> Preview.Failed(status.seconds)
                    RetryStatus.Retrying -> Preview.Retrying
                }
            },
        )
        preview = when (r) {
            is ApiResult.Ok -> {
                val summary = r.value
                when {
                    summary.departures.isNotEmpty() -> Preview.Loaded(summary)
                    summary.walkMinutes != null -> Preview.Walking(summary.walkMinutes)
                    else -> Preview.NoTrips
                }
            }
            else -> Preview.KeyProblem
        }
    }

    val initialFocus = rememberInitialFocus()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Page)
            .focusEntry(initialFocus),
    ) {
        AppBar(
            title = title,
            navigation = { BackButton(onClick = onBack, modifier = Modifier.focusRequester(initialFocus)) },
        ) {
            if (next) NextAction(enabled = ready, onClick = onDone) else DoneAction(enabled = ready, onClick = onDone)
        }
        // The preview scrolls in the background; the pickers stay pinned in the foreground. Keeping
        // the scroll viewport above the pickers (rather than under them) means D-pad focus never
        // lands on something hidden behind them.
        val scroll = rememberScrollState()
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (ready) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(vertical = 16.du),
                    verticalArrangement = Arrangement.spacedBy(2.du),
                ) {
                    BasicText(
                        previewLabel,
                        style = Type.label(Palette.BodyText),
                        modifier = Modifier.padding(start = 8.du, end = 8.du, top = 8.du, bottom = 4.du),
                    )
                    RouteHeader(origin = origin, destination = destination, onSwap = onSwap)
                    key(origin.id, destination.id, preferences) {
                        CollapsingSwap(target = preview, kind = { it.kind() }) { shown ->
                            PreviewBlock(shown, now)
                        }
                    }
                }
            } else if (suggestions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(vertical = 16.du),
                    verticalArrangement = Arrangement.spacedBy(2.du),
                ) {
                    BasicText(
                        "Suggested",
                        style = Type.label(Palette.BodyText),
                        modifier = Modifier.padding(start = 8.du, end = 8.du, top = 8.du, bottom = 4.du),
                    )
                    suggestions.forEach { trip -> SuggestedTripRow(trip = trip, onClick = { onSuggestion(trip) }) }
                }
            } else {
                Hint(text = hint, modifier = Modifier.fillMaxSize())
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .foregroundShadow(visible = scroll.canScrollForward)
                .background(Palette.Page)
                .navigationBarsPadding()
                .padding(top = 8.du, bottom = 16.du),
            verticalArrangement = Arrangement.spacedBy(2.du),
        ) {
            // This trip's preferences in a few words (no timing: Home always shows what's leaving now).
            ActionRow(
                label = preferences.summary(java.time.LocalDate.now()) ?: "Trip preferences",
                icon = R.drawable.ic_arrow_forward,
                onClick = onPreferences,
                maxLines = 3,
            )
            PlaceRow(placeholder = "Origin", value = origin?.name, icon = R.drawable.ic_trip_origin, onClick = onPickOrigin)
            PlaceRow(
                placeholder = "Destination",
                value = destination?.name,
                icon = R.drawable.ic_pin_drop,
                onClick = onPickDestination,
            )
        }
    }
}

/** The two stop names with a swap button, above the preview (Figma 48:1714). */
@Composable
private fun RouteHeader(origin: Stop, destination: Stop, onSwap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Palette.Card)
            .padding(horizontal = 16.du, vertical = 20.du),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.du),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.du)) {
            BasicText(origin.shortName, style = Type.title(Palette.TitleText), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(destination.shortName, style = Type.title(Palette.TitleText), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        SurfaceIcon(R.drawable.ic_swap_vert, "Swap origin and destination", onClick = onSwap)
    }
}

/** A trip already on Home, offered as a one-tap route for a widget (Figma 48:1970). */
@Composable
private fun SuggestedTripRow(trip: SavedTrip, onClick: () -> Unit) {
    FocusSurface(onClick = onClick, color = Palette.Card, modifier = Modifier.fillMaxWidth()) { focused ->
        val text = if (focused) Palette.FocusedContent else Palette.TitleText
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.du, vertical = 20.du),
            verticalArrangement = Arrangement.spacedBy(4.du),
        ) {
            BasicText(trip.origin.shortName, style = Type.title(text), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(trip.destination.shortName, style = Type.title(text), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Centred "add home" icon and a line of explanation, filling the empty preview area (Figma 48:1609). */
@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.du, vertical = 16.du),
        verticalArrangement = Arrangement.spacedBy(2.du, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_add_home),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Palette.HintText),
            modifier = Modifier.size(48.du),
        )
        BasicText(text, style = Type.body(Palette.HintText).copy(textAlign = TextAlign.Center))
    }
}

@Composable
private fun PreviewBlock(preview: Preview, now: State<Long>) {
    when (preview) {
        Preview.Loading -> PreviewCarousel(summary = null, now = now)
        is Preview.Loaded -> PreviewCarousel(summary = preview.summary, now = now)
        Preview.Retrying -> StatusSpinner()
        is Preview.Failed -> StatusMessage(
            title = "Couldn’t load preview",
            detail = retryingIn(preview.retryInSeconds),
            icon = R.drawable.ic_cloud_alert,
            background = Palette.AlertHighBackground,
            foreground = Palette.AlertForeground,
        )
        Preview.KeyProblem -> StatusMessage(
            title = "Couldn’t load preview",
            detail = "Check your API key in Settings",
            icon = R.drawable.ic_cloud_alert,
            background = Palette.AlertHighBackground,
            foreground = Palette.AlertForeground,
        )
        Preview.Offline -> StatusMessage(
            title = "Offline",
            detail = "Connect to the internet to load a preview",
            icon = R.drawable.ic_cloud_off,
            background = Palette.NeutralStatusBackground,
            foreground = Palette.NeutralStatusForeground,
        )
        is Preview.Walking -> StatusMessage(
            title = "Walking distance: ${preview.minutes}min",
            detail = "The destination is within walking distance",
            icon = R.drawable.ic_directions_walk,
            background = Palette.NoticeBackground,
            foreground = Palette.NoticeText,
        )
        Preview.NoTrips -> StatusMessage(
            title = "No trips found",
            detail = "Couldn’t find a way to get there",
            icon = R.drawable.ic_wrong_location,
            background = Palette.NeutralStatusBackground,
            foreground = Palette.NeutralStatusForeground,
        )
    }
}

/**
 * White block with the departure cards. Cards aren't clickable here: they take focus (shadow only)
 * so the D-pad can scroll the carousel. The placeholder morphs into the cards (Figma 24:1843).
 */
@Composable
private fun PreviewCarousel(summary: TripSummary?, now: State<Long>) {
    val morph = remember { Animatable(if (summary != null) 1f else 0f) }
    val reveal = remember { Animatable(if (summary != null) 1f else 0f) }
    LaunchedEffect(summary != null) {
        if (summary == null || morph.value >= 1f) return@LaunchedEffect
        morph.animateTo(1f, tween(Motion.EXPAND_MS, easing = Motion.Expand))
        reveal.animateTo(1f, tween(revealDurationMs(summary.departures.size), easing = LinearEasing))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Palette.Card)
            .padding(vertical = 20.du),
    ) {
        if (summary == null) {
            DepartureLoadingPlaceholder()
        } else {
            DepartureCarousel(
                cards = summary.departures,
                now = now,
                onCardClick = null,
                morph = { morph.value },
                reveal = { reveal.value },
                compact = true,
            )
        }
    }
}
