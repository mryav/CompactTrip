package com.mryav.compacttrip.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.ApiResult
import com.mryav.compacttrip.data.RetryStatus
import com.mryav.compacttrip.data.loadWithRetry
import com.mryav.compacttrip.data.model.Journey
import com.mryav.compacttrip.data.model.JourneyLeg
import com.mryav.compacttrip.data.model.Notice
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.data.model.TripPreferences
import com.mryav.compacttrip.data.model.TripSummary
import com.mryav.compacttrip.data.retryingIn
import com.mryav.compacttrip.ui.components.ActionRow
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.AppBarIcon
import com.mryav.compacttrip.ui.components.BackButton
import com.mryav.compacttrip.ui.components.CollapsingSwap
import com.mryav.compacttrip.ui.components.Motion
import com.mryav.compacttrip.ui.components.PlaceRow
import com.mryav.compacttrip.ui.components.ServiceRow
import com.mryav.compacttrip.ui.components.StatusMessage
import com.mryav.compacttrip.ui.components.StatusSpinner
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.foregroundShadow
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.du
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Results refresh this often while the screen is open; results younger than [REUSE_MS] are kept on return. */
private const val REFRESH_MS = 60_000L
private const val REUSE_MS = 30_000L

/** Status block above the results (same blocks as the Add trip preview and the stop search). */
@Immutable
sealed interface PlanStatus {
    data object Spinner : PlanStatus
    data class Failed(val retryInSeconds: Int) : PlanStatus
    data object Offline : PlanStatus
    data object NoTrips : PlanStatus
    data class Walking(val minutes: Int) : PlanStatus
    data object KeyProblem : PlanStatus
}

/**
 * Plan new trip, held above the screen so the stops and results survive a visit to the stop search
 * or Trip Details. Not saved anywhere: a fresh one starts from Home each time.
 */
@Stable
class PlanTripSession {
    var origin by mutableStateOf<Stop?>(null)
    var destination by mutableStateOf<Stop?>(null)
    var preferences by mutableStateOf(TripPreferences.Default)
    var summary by mutableStateOf<TripSummary?>(null)
        internal set
    var status by mutableStateOf<PlanStatus?>(null)
        internal set
    var loading by mutableStateOf(false)
        internal set
    internal var refreshRequests by mutableIntStateOf(0)
    /** What the current results answer: both stops and the preferences. */
    internal var loadedFor: Any? = null
    internal var loadedAt = 0L
    /** The refresh count the current results answer: returning (same count) reuses fresh results; a press loads now. */
    internal var refreshSeen = -1

    val ready: Boolean get() = origin != null && destination != null && origin?.id != destination?.id

    fun refresh() {
        refreshRequests++
    }

    /** Start a fresh plan: no stops, leaving now, with the user's standing [preferences]. */
    fun reset(preferences: TripPreferences) {
        this.preferences = preferences
        origin = null
        destination = null
        summary = null
        status = null
        loadedFor = null
    }
}

/**
 * Plan new trip (Figma 6:890): a one-off lookup. Upcoming journeys scroll in the background; the
 * mode, origin and destination rows stay pinned in the foreground (like Add trip). Each journey
 * opens Trip Details. Results refresh every minute while the screen is open.
 */
@Composable
fun PlanTripScreen(
    session: PlanTripSession,
    load: suspend (Stop, Stop, TripPreferences) -> ApiResult<TripSummary>,
    awaitOnline: suspend () -> Unit,
    now: State<Long>,
    onBack: () -> Unit,
    onPickOrigin: () -> Unit,
    onPickDestination: () -> Unit,
    onPreferences: () -> Unit,
    onOpenJourney: (Journey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val origin = session.origin
    val destination = session.destination
    val preferences = session.preferences

    LaunchedEffect(origin?.id, destination?.id, preferences, session.refreshRequests) {
        if (origin == null || destination == null || !session.ready) {
            session.summary = null
            session.status = null
            return@LaunchedEffect
        }
        val stops = Triple(origin.id, destination.id, preferences)
        val requested = session.refreshRequests
        var wait = 0L
        if (stops != session.loadedFor) {
            session.summary = null
            session.status = null
        } else if (requested == session.refreshSeen) {
            // Back from Trip Details or the stop search: keep fresh results.
            wait = (REUSE_MS - (System.currentTimeMillis() - session.loadedAt)).coerceAtLeast(0L)
        }
        session.refreshSeen = requested
        while (true) {
            if (wait > 0) delay(wait)
            loadOnce(session, origin, destination, preferences, load, awaitOnline)
            wait = REFRESH_MS
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
            title = "Plan new trip",
            navigation = { BackButton(onClick = onBack, modifier = Modifier.focusRequester(initialFocus)) },
        ) {
            if (session.ready) {
                AppBarIcon(
                    R.drawable.ic_refresh,
                    "Refresh",
                    onClick = session::refresh,
                    busy = session.loading,
                    busyDescription = "Refreshing",
                )
            }
        }
        // Outside the list, so it can't be scrolled out of view.
        CollapsingSwap(target = session.status, kind = { it::class }) { PlanStatusBlock(it) }
        val list = rememberLazyListState()
        val summary = session.summary
        LazyColumn(
            state = list,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.du),
            contentPadding = PaddingValues(top = 16.du, bottom = 16.du),
        ) {
            items(summary?.journeys.orEmpty(), key = { it.key }) { journey ->
                ServiceRow(
                    journey = journey,
                    now = now,
                    onClick = { onOpenJourney(journey) },
                    notices = summary?.noticesFor(journey),
                    modifier = Modifier.animateItem(),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .foregroundShadow(visible = list.canScrollForward)
                .background(Palette.Page)
                .navigationBarsPadding()
                .padding(top = 8.du, bottom = 16.du),
            verticalArrangement = Arrangement.spacedBy(2.du),
        ) {
            // The preferences in a few plain words (Figma 27:3318), or "Trip preferences" when default.
            val today = remember(now.value / 3_600_000) { java.time.LocalDate.now(java.time.ZoneId.of("Australia/Sydney")) }
            ActionRow(
                label = preferences.summary(today) ?: "Trip preferences",
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

/** One load, with retries. Existing results stay visible while it runs and if it fails. */
private suspend fun loadOnce(
    session: PlanTripSession,
    origin: Stop,
    destination: Stop,
    preferences: TripPreferences,
    load: suspend (Stop, Stop, TripPreferences) -> ApiResult<TripSummary>,
    awaitOnline: suspend () -> Unit,
) = coroutineScope {
    session.loading = true
    // First load: the spinner block only if it's slow (Figma 24:1563 rule).
    val slow = if (session.summary == null) {
        launch {
            delay(Motion.SPINNER_DELAY_MS)
            if (session.status == null) session.status = PlanStatus.Spinner
        }
    } else {
        null
    }
    val r = try {
        loadWithRetry(
            load = { load(origin, destination, preferences) },
            awaitOnline = awaitOnline,
            onStatus = { s ->
                session.status = when (s) {
                    RetryStatus.Offline -> PlanStatus.Offline
                    is RetryStatus.Waiting -> PlanStatus.Failed(s.seconds)
                    RetryStatus.Retrying -> PlanStatus.Spinner
                }
            },
        )
    } finally {
        slow?.cancel()
        session.loading = false
    }
    if (r is ApiResult.Ok) {
        val summary = r.value
        session.summary = summary
        session.loadedFor = Triple(origin.id, destination.id, preferences)
        session.loadedAt = System.currentTimeMillis()
        session.status = when {
            summary.departures.isNotEmpty() -> null
            summary.walkMinutes != null -> PlanStatus.Walking(summary.walkMinutes)
            else -> PlanStatus.NoTrips
        }
    } else {
        session.status = PlanStatus.KeyProblem
    }
}

/** The notices on [journey]'s own legs. */
private fun TripSummary.noticesFor(journey: Journey): List<Notice> {
    val ids = journey.legs.filterIsInstance<JourneyLeg.Ride>().flatMap { it.noticeIds }.toSet()
    return notices.filter { it.id in ids }
}

@Composable
private fun PlanStatusBlock(status: PlanStatus) {
    when (status) {
        PlanStatus.Spinner -> StatusSpinner()
        is PlanStatus.Failed -> StatusMessage(
            title = "Connection error",
            detail = retryingIn(status.retryInSeconds),
            icon = R.drawable.ic_cloud_alert,
            background = Palette.AlertHighBackground,
            foreground = Palette.AlertForeground,
        )
        PlanStatus.KeyProblem -> StatusMessage(
            title = "Couldn’t load trips",
            detail = "Check your API key in Settings",
            icon = R.drawable.ic_cloud_alert,
            background = Palette.AlertHighBackground,
            foreground = Palette.AlertForeground,
        )
        PlanStatus.Offline -> StatusMessage(
            title = "Offline",
            detail = "Connect to the internet to plan a trip",
            icon = R.drawable.ic_cloud_off,
            background = Palette.NeutralStatusBackground,
            foreground = Palette.NeutralStatusForeground,
        )
        PlanStatus.NoTrips -> StatusMessage(
            title = "No trips found",
            detail = "Couldn’t find a way to get there",
            icon = R.drawable.ic_wrong_location,
            background = Palette.NeutralStatusBackground,
            foreground = Palette.NeutralStatusForeground,
        )
        is PlanStatus.Walking -> StatusMessage(
            title = "Walking distance: ${status.minutes}min",
            detail = "The destination is within walking distance",
            icon = R.drawable.ic_directions_walk,
            background = Palette.NoticeBackground,
            foreground = Palette.NoticeText,
        )
    }
}
