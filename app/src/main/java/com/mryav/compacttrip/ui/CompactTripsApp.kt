package com.mryav.compacttrip.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mryav.compacttrip.AppContainer
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.data.model.Journey
import com.mryav.compacttrip.data.model.Notice
import com.mryav.compacttrip.data.model.SavedTrip
import com.mryav.compacttrip.ui.components.Motion
import com.mryav.compacttrip.ui.screens.AddTripScreen
import com.mryav.compacttrip.ui.screens.ApiKeyScreen
import com.mryav.compacttrip.ui.screens.HomeAction
import com.mryav.compacttrip.ui.screens.HomeScreen
import com.mryav.compacttrip.ui.screens.SelectStopScreen
import com.mryav.compacttrip.ui.screens.NoticesScreen
import com.mryav.compacttrip.ui.screens.PlanTripScreen
import com.mryav.compacttrip.ui.screens.PlanTripSession
import com.mryav.compacttrip.ui.screens.ServiceAlertScreen
import com.mryav.compacttrip.ui.screens.SettingsScreen
import com.mryav.compacttrip.ui.screens.TripDetailsScreen
import com.mryav.compacttrip.ui.screens.ViewServicesScreen
import com.mryav.compacttrip.ui.screens.USER_GUIDE_URL
import com.mryav.compacttrip.ui.screens.WelcomeScreen
import kotlinx.coroutines.delay

enum class Route {
    Welcome, ApiKeySetup, ApiKeyEdit, Home, Settings, AddTrip, PlanTrip, SelectOrigin, SelectDestination,
    ViewServices, TripDetails, Notices, ServiceAlert,
}

/** Which screen the stop search is filling in. */
private enum class PickFor { AddTrip, PlanTrip }

/** Stand-in id for a planned (unsaved) trip shown in Trip Details. */
private const val PLANNED_TRIP_ID = "planned"

/** Routes that can be rebuilt after process death; the others depend on in-memory selections. */
private val RESTORABLE = setOf(Route.Welcome, Route.ApiKeySetup, Route.ApiKeyEdit, Route.Home, Route.Settings)

/** Routes showing live departures: refresh runs while one of these is on screen. */
private val LIVE = setOf(Route.Home, Route.ViewServices, Route.TripDetails, Route.Notices)

/** Countdown/"Updated" ticker. Only the composables that read it recompose. */
private const val TICK_MS = 15_000L

/** Home refresh cadence while visible; data younger than this is reused when returning to Home. */
private const val REFRESH_MS = 60_000L
private const val REUSE_MS = 30_000L

/** App root: a tiny back stack (no navigation library), instant screen switches. */
@Composable
fun CompactTripsApp(
    container: AppContainer,
    openUrl: (String) -> Unit,
    showMessage: (String) -> Unit,
) {
    val keyStore = container.keyStore
    val keyChecker = container.keyChecker
    val stack: SnapshotStateList<Route> = rememberSaveable(
        saver = listSaver(
            save = { s -> s.map { it.name } },
            restore = { s ->
                val routes = s.map(Route::valueOf).takeWhile { it in RESTORABLE }
                routes.ifEmpty { listOf(if (keyStore.hasKey) Route.Home else Route.Welcome) }.toMutableStateList()
            },
        ),
    ) { mutableStateListOf(if (keyStore.hasKey) Route.Home else Route.Welcome) }

    // Which way the last navigation went, for the screen transition.
    var navigatingForward by remember { mutableStateOf(true) }

    fun push(route: Route) {
        navigatingForward = true
        stack.add(route)
    }
    fun pop() {
        navigatingForward = false
        stack.removeAt(stack.lastIndex)
    }
    /** Replace the whole stack. [forward]: finishing a flow (e.g. key set up) rather than leaving one. */
    fun resetTo(route: Route, forward: Boolean = false) {
        navigatingForward = forward
        stack.clear()
        stack.add(route)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    val now = produceState(System.currentTimeMillis()) {
        while (true) {
            delay(TICK_MS)
            value = System.currentTimeMillis()
        }
    }

    // Add-trip picks survive trips to the stop search and back.
    var draftOrigin by remember { mutableStateOf<Stop?>(null) }
    var draftDestination by remember { mutableStateOf<Stop?>(null) }
    // Plan new trip: stops and results survive the stop search and Trip Details.
    val plan = remember { PlanTripSession() }
    var pickFor by remember { mutableStateOf(PickFor.AddTrip) }
    // Trip Details for a planned trip (not saved, so not in `trips`).
    var plannedTrip by remember { mutableStateOf<SavedTrip?>(null) }
    // Detail selections (in memory only; see RESTORABLE).
    var selectedTripId by remember { mutableStateOf<String?>(null) }
    var selectedJourney by remember { mutableStateOf<Journey?>(null) }
    var selectedNotice by remember { mutableStateOf<Notice?>(null) }

    val trips by container.savedTrips.trips.collectAsStateWithLifecycle()
    val board by container.board.state.collectAsStateWithLifecycle()
    val route = stack.last()

    // Live refresh while a departures screen is visible (paused in the background).
    val lifecycleOwner = LocalLifecycleOwner.current
    val live = route in LIVE
    LaunchedEffect(lifecycleOwner, live, trips) {
        if (!live) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            container.board.refresh(ifOlderThanMs = REUSE_MS)
            while (true) {
                delay(REFRESH_MS)
                container.board.refresh()
            }
        }
    }

    fun openTrip(trip: SavedTrip) {
        plannedTrip = null
        selectedTripId = trip.id
        push(Route.ViewServices)
    }
    fun openNotices(trip: SavedTrip) {
        plannedTrip = null
        selectedTripId = trip.id
        push(Route.Notices)
    }
    fun openJourney(trip: SavedTrip, journey: Journey) {
        plannedTrip = null
        selectedTripId = trip.id
        selectedJourney = journey
        push(Route.TripDetails)
    }
    fun openPlannedJourney(journey: Journey) {
        val o = plan.origin ?: return
        val d = plan.destination ?: return
        plannedTrip = SavedTrip(PLANNED_TRIP_ID, o, d)
        selectedTripId = null
        selectedJourney = journey
        push(Route.TripDetails)
    }
    fun openNotice(notice: Notice) {
        selectedNotice = notice
        push(Route.ServiceAlert)
    }
    val liveTrip = plannedTrip ?: trips.firstOrNull { it.id == selectedTripId }
    val selectedLoad = selectedTripId?.let { board.loads[it] }
    val selectedNotices = if (plannedTrip != null) plan.summary?.notices.orEmpty() else selectedLoad?.summary?.notices.orEmpty()

    // key(): each screen gets fresh state when it's (re)entered.
    // Screen transitions: full-width push. Each screen is keyed by route + stack depth so returning to
    // an earlier screen animates as "back".
    val current = Screen(route, stack.size)
    // A screen animating away keeps its last trip/journey/notice even if it's just been removed.
    val selectedTrip = rememberLastNonNull(liveTrip)
    val shownJourney = rememberLastNonNull(selectedJourney)
    val shownNotice = rememberLastNonNull(selectedNotice)
    AnimatedContent(
        targetState = current,
        transitionSpec = {
            val direction = if (navigatingForward) 1 else -1
            val slide = tween<IntOffset>(Motion.SCREEN_MS, easing = Motion.Push)
            val enter = slideInHorizontally(slide) { width -> direction * width }
            val exit = slideOutHorizontally(slide) { width -> -direction * width }
            (enter togetherWith exit).using(null)
        },
        label = "screen",
    ) { screen ->
        val route = screen.route
            when (route) {
                Route.Welcome -> key(route) {
                    WelcomeScreen(onSetUpKey = { push(Route.ApiKeySetup) })
                }
                Route.ApiKeySetup -> key(route) {
                    ApiKeyScreen(
                        initialKey = null,
                        checker = keyChecker,
                        onBack = { pop() },
                        onSave = { k ->
                            keyStore.save(k, checkSucceeded = true)
                            resetTo(Route.Home, forward = true)
                        },
                        onOpenUserGuide = { openUrl(USER_GUIDE_URL) },
                    )
                }
                Route.ApiKeyEdit -> key(route) {
                    ApiKeyScreen(
                        initialKey = keyStore.apiKey,
                        checker = keyChecker,
                        onBack = { pop() },
                        onSave = { k ->
                            keyStore.save(k, checkSucceeded = true)
                            pop()
                        },
                        onDelete = {
                            keyStore.clear()
                            resetTo(Route.Welcome)
                        },
                        onOpenUserGuide = { openUrl(USER_GUIDE_URL) },
                    )
                }
                Route.Home -> key(route) {
                    HomeScreen(
                        trips = trips,
                        board = board,
                        now = now,
                        onAction = { action ->
                            when (action) {
                                HomeAction.Settings -> push(Route.Settings)
                                HomeAction.AddTrip -> {
                                    draftOrigin = null
                                    draftDestination = null
                                    push(Route.AddTrip)
                                }
                                HomeAction.Refresh -> container.board.refresh()
                                HomeAction.PlanTrip -> {
                                    plan.reset()
                                    push(Route.PlanTrip)
                                }
                            }
                        },
                        onOpenTrip = ::openTrip,
                        onOpenNotices = ::openNotices,
                        onOpenDeparture = { trip, card ->
                            board.loads[trip.id]?.summary?.journeys?.firstOrNull { it.key == card.key }?.let { openJourney(trip, it) }
                        },
                    )
                }
                Route.Settings -> key(route) {
                    SettingsScreen(
                        apiKeyWorking = keyStore.lastCheckSucceeded,
                        onBack = { pop() },
                        onEditApiKey = { push(Route.ApiKeyEdit) },
                    )
                }
                Route.AddTrip -> key(route) {
                    AddTripScreen(
                        origin = draftOrigin,
                        destination = draftDestination,
                        loadPreview = { o, d -> container.trips.upcoming(o, d) },
                        awaitOnline = { container.http.awaitOnline() },
                        now = now,
                        onBack = { pop() },
                        onPickOrigin = {
                            pickFor = PickFor.AddTrip
                            push(Route.SelectOrigin)
                        },
                        onPickDestination = {
                            pickFor = PickFor.AddTrip
                            push(Route.SelectDestination)
                        },
                        onModes = { showMessage("Choosing modes is coming in the next update. Showing all modes.") },
                        onDone = {
                            val o = draftOrigin
                            val d = draftDestination
                            if (o != null && d != null) {
                                container.savedTrips.add(o, d)
                                container.board.refresh()
                            }
                            pop()
                        },
                    )
                }
                Route.PlanTrip -> key(route) {
                    PlanTripScreen(
                        session = plan,
                        load = { o, d -> container.trips.upcoming(o, d) },
                        awaitOnline = { container.http.awaitOnline() },
                        now = now,
                        onBack = { pop() },
                        onPickOrigin = {
                            pickFor = PickFor.PlanTrip
                            push(Route.SelectOrigin)
                        },
                        onPickDestination = {
                            pickFor = PickFor.PlanTrip
                            push(Route.SelectDestination)
                        },
                        onModes = { showMessage("Choosing modes is coming in the next update. Showing all modes.") },
                        onOpenJourney = ::openPlannedJourney,
                    )
                }
                Route.SelectOrigin, Route.SelectDestination -> key(route) {
                    val isOrigin = route == Route.SelectOrigin
                    val forPlan = pickFor == PickFor.PlanTrip
                    val otherEnd = when {
                        forPlan && isOrigin -> plan.destination
                        forPlan -> plan.origin
                        isOrigin -> draftDestination
                        else -> draftOrigin
                    }
                    SelectStopScreen(
                        title = if (isOrigin) "Select Origin" else "Select Destination",
                        recentStops = remember { container.savedTrips.recentStops() },
                        search = { q -> container.stops.search(q) },
                        awaitOnline = { container.http.awaitOnline() },
                        onPick = { stop ->
                            container.savedTrips.rememberStop(stop)
                            when {
                                forPlan && isOrigin -> plan.origin = stop
                                forPlan -> plan.destination = stop
                                isOrigin -> draftOrigin = stop
                                else -> draftDestination = stop
                            }
                            pop()
                        },
                        onBack = { pop() },
                        excludeStopId = otherEnd?.id,
                    )
                }
                Route.ViewServices -> key(route) {
                    val trip = selectedTrip
                    if (trip == null) {
                        if (screen == current) LaunchedEffect(Unit) { resetTo(Route.Home) }
                    } else {
                        ViewServicesScreen(
                            trip = trip,
                            load = selectedLoad,
                            updatedAt = board.updatedAt,
                            refreshing = board.refreshing,
                            now = now,
                            onBack = { pop() },
                            onRefresh = { container.board.refresh() },
                            onOpenJourney = { openJourney(trip, it) },
                            onOpenNotices = { openNotices(trip) },
                            onRemove = {
                                container.savedTrips.remove(trip.id)
                                container.board.refresh()
                                resetTo(Route.Home)
                                showMessage("Removed ${trip.title}")
                            },
                        )
                    }
                }
                Route.TripDetails -> key(route) {
                    val trip = selectedTrip
                    val journey = shownJourney
                    if (trip == null || journey == null) {
                        if (screen == current) LaunchedEffect(Unit) { resetTo(Route.Home) }
                    } else {
                        TripDetailsScreen(
                            trip = trip,
                            journey = journey,
                            notices = selectedNotices,
                            now = now,
                            onBack = { pop() },
                            onOpenNotice = ::openNotice,
                        )
                    }
                }
                Route.Notices -> key(route) {
                    val trip = selectedTrip
                    if (trip == null) {
                        if (screen == current) LaunchedEffect(Unit) { resetTo(Route.Home) }
                    } else {
                        NoticesScreen(
                            trip = trip,
                            notices = selectedLoad?.summary?.notices.orEmpty(),
                            onBack = { pop() },
                            onOpenNotice = ::openNotice,
                        )
                    }
                }
                Route.ServiceAlert -> key(route) {
                    val notice = shownNotice
                    if (notice == null) {
                        if (screen == current) LaunchedEffect(Unit) { resetTo(Route.Home) }
                    } else {
                        ServiceAlertScreen(notice = notice, onBack = { pop() }, onOpenUrl = openUrl)
                    }
                }
            }
    }
}

/** One entry of the back stack, as seen by the screen transition. */
private data class Screen(val route: Route, val depth: Int)

/** [value], or the last non-null value it had (so an exiting screen can keep rendering). */
@Composable
private fun <T : Any> rememberLastNonNull(value: T?): T? {
    val last = remember { arrayOfNulls<Any>(1) }
    if (value != null) last[0] = value
    @Suppress("UNCHECKED_CAST")
    return last[0] as T?
}
