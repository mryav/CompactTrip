package com.mryav.compacttrip.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.ApiResult
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.data.model.TripSummary
import com.mryav.compacttrip.ui.components.ActionRow
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.BackButton
import com.mryav.compacttrip.ui.components.CollapsingSwap
import com.mryav.compacttrip.ui.components.DepartureCarousel
import com.mryav.compacttrip.ui.components.DepartureLoadingPlaceholder
import com.mryav.compacttrip.ui.components.DoneAction
import com.mryav.compacttrip.ui.components.Motion
import com.mryav.compacttrip.ui.components.PlaceRow
import com.mryav.compacttrip.ui.components.StatusMessage
import com.mryav.compacttrip.ui.components.StatusSpinner
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.foregroundShadow
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.components.revealDurationMs
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du
import kotlinx.coroutines.delay

/** First automatic retry after "Couldn't load preview"; doubles each time up to [MAX_RETRY_S]. Kept long to avoid API throttling. */
private const val FIRST_RETRY_S = 10
private const val MAX_RETRY_S = 60
/** After coming back online, at least this long since the last attempt before trying again. */
private const val OFFLINE_RETRY_GAP_MS = 10_000L

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
 * Add trip to home (Figma 4:1614 empty; 25:2749 preview states). Origin/Destination open the stop
 * search; once both are set a compact preview of the next departures appears. Done saves the trip
 * whether or not the preview loads.
 */
@Composable
fun AddTripScreen(
    origin: Stop?,
    destination: Stop?,
    loadPreview: suspend (Stop, Stop) -> ApiResult<TripSummary>,
    awaitOnline: suspend () -> Unit,
    now: State<Long>,
    onBack: () -> Unit,
    onPickOrigin: () -> Unit,
    onPickDestination: () -> Unit,
    onModes: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = origin != null && destination != null && origin.id != destination.id
    // Reset as soon as the stops change, so the previous trip's preview never shows for a frame.
    var preview by remember(origin?.id, destination?.id) { mutableStateOf<Preview>(Preview.Loading) }

    LaunchedEffect(origin?.id, destination?.id) {
        if (!ready) return@LaunchedEffect
        var failures = 0
        while (true) {
            val attemptAt = System.currentTimeMillis()
            when (val r = loadPreview(origin, destination)) {
                is ApiResult.Ok -> {
                    val summary = r.value
                    preview = when {
                        summary.departures.isNotEmpty() -> Preview.Loaded(summary)
                        summary.walkMinutes != null -> Preview.Walking(summary.walkMinutes)
                        else -> Preview.NoTrips
                    }
                    return@LaunchedEffect
                }
                ApiResult.Unauthorized, ApiResult.NoKey -> {
                    preview = Preview.KeyProblem
                    return@LaunchedEffect
                }
                ApiResult.Offline -> {
                    preview = Preview.Offline
                    awaitOnline()
                    val wait = OFFLINE_RETRY_GAP_MS - (System.currentTimeMillis() - attemptAt)
                    if (wait > 0) delay(wait)
                }
                ApiResult.ServiceUnavailable -> {
                    failures++
                    val seconds = (FIRST_RETRY_S shl (failures - 1).coerceAtMost(3)).coerceAtMost(MAX_RETRY_S)
                    for (s in seconds downTo 1) {
                        preview = Preview.Failed(s)
                        delay(1_000)
                    }
                }
            }
            preview = Preview.Retrying
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
            title = "Add trip to home",
            navigation = { BackButton(onClick = onBack, modifier = Modifier.focusRequester(initialFocus)) },
        ) {
            DoneAction(enabled = ready, onClick = onDone)
        }
        // The preview scrolls in the background; the pickers stay pinned in the foreground. Keeping
        // the scroll viewport above the pickers (rather than under them) means D-pad focus never
        // lands on something hidden behind them.
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(vertical = 16.du),
            verticalArrangement = Arrangement.spacedBy(2.du),
        ) {
            if (ready) {
                BasicText(
                    "Preview",
                    style = Type.condensed24(Palette.BodyText),
                    modifier = Modifier.padding(start = 8.du, end = 8.du, top = 8.du, bottom = 4.du),
                )
                key(origin.id, destination.id) {
                    CollapsingSwap(target = preview, kind = { it.kind() }) { shown ->
                        PreviewBlock(shown, now)
                    }
                }
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
            ActionRow(label = "Showing all modes", icon = R.drawable.ic_arrow_forward, onClick = onModes)
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

@Composable
private fun PreviewBlock(preview: Preview, now: State<Long>) {
    when (preview) {
        Preview.Loading -> PreviewCarousel(summary = null, now = now)
        is Preview.Loaded -> PreviewCarousel(summary = preview.summary, now = now)
        Preview.Retrying -> StatusSpinner()
        is Preview.Failed -> StatusMessage(
            title = "Couldn’t load preview",
            detail = preview.retryInSeconds.let { if (it == 1) "Retrying in 1 second…" else "Retrying in $it seconds…" },
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
