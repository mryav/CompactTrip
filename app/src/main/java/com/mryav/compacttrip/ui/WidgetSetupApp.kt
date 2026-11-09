package com.mryav.compacttrip.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import com.mryav.compacttrip.AppContainer
import com.mryav.compacttrip.data.ApiResult
import com.mryav.compacttrip.data.LoadError
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.ui.components.Motion
import com.mryav.compacttrip.ui.screens.AddTripScreen
import com.mryav.compacttrip.ui.screens.CreateWidgetScreen
import com.mryav.compacttrip.ui.screens.SelectStopScreen
import com.mryav.compacttrip.ui.screens.TripPreferencesScreen
import com.mryav.compacttrip.ui.screens.WidgetInfoScreen
import com.mryav.compacttrip.widget.WidgetConfig
import com.mryav.compacttrip.widget.WidgetSnapshot
import com.mryav.compacttrip.widget.WidgetStyle
import com.mryav.compacttrip.widget.toSnapshot
import com.mryav.compacttrip.widget.withError
import com.mryav.compacttrip.widget.withoutTiming
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId

private enum class SetupRoute { SelectRoute, SelectOrigin, SelectDestination, Preferences, CreateWidget, Info }

/** Countdown ticker for the previews. */
private const val TICK_MS = 15_000L

/**
 * Widget setup (Figma 30:5436): Select route → Create widget (→ Widget information). The same
 * screens and push transition as the app; [style] is the widget that was picked.
 */
@Composable
fun WidgetSetupApp(
    container: AppContainer,
    style: WidgetStyle,
    onCancel: () -> Unit,
    onDone: (WidgetConfig, WidgetSnapshot?) -> Unit,
) {
    val stack = remember { mutableStateListOf(SetupRoute.SelectRoute) }
    var forward by remember { mutableStateOf(true) }
    fun push(route: SetupRoute) {
        forward = true
        stack.add(route)
    }
    fun pop() {
        forward = false
        stack.removeAt(stack.lastIndex)
    }
    BackHandler(enabled = stack.size > 1) { pop() }

    val now = produceState(System.currentTimeMillis()) {
        while (true) {
            delay(TICK_MS)
            value = System.currentTimeMillis()
        }
    }

    var origin by remember { mutableStateOf<Stop?>(null) }
    var destination by remember { mutableStateOf<Stop?>(null) }
    var preferences by remember { mutableStateOf(container.savedTrips.planPreferences()) }
    // The Create widget preview, kept while visiting Widget information.
    var previewFor by remember { mutableStateOf<WidgetConfig?>(null) }
    var previewSnapshot by remember { mutableStateOf<WidgetSnapshot?>(null) }
    var previewLoading by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = stack.last() to stack.size,
        transitionSpec = {
            val direction = if (forward) 1 else -1
            val slide = tween<IntOffset>(Motion.SCREEN_MS, easing = Motion.Push)
            (slideInHorizontally(slide) { w -> direction * w } togetherWith slideOutHorizontally(slide) { w -> -direction * w })
                .using(null)
        },
        label = "setup",
    ) { (route, _) ->
        when (route) {
            SetupRoute.SelectRoute -> key(route) {
                AddTripScreen(
                    origin = origin,
                    destination = destination,
                    preferences = preferences,
                    loadPreview = { o, d, p -> container.trips.upcoming(o, d, p) },
                    awaitOnline = { container.http.awaitOnline() },
                    now = now,
                    onBack = onCancel,
                    onPickOrigin = { push(SetupRoute.SelectOrigin) },
                    onPickDestination = { push(SetupRoute.SelectDestination) },
                    onPreferences = { push(SetupRoute.Preferences) },
                    onDone = { push(SetupRoute.CreateWidget) },
                    title = "Select route",
                    previewLabel = "Available routes",
                    next = true,
                )
            }
            SetupRoute.SelectOrigin, SetupRoute.SelectDestination -> key(route) {
                val isOrigin = route == SetupRoute.SelectOrigin
                SelectStopScreen(
                    title = if (isOrigin) "Select Origin" else "Select Destination",
                    recentStops = remember { container.savedTrips.recentStops() },
                    search = { q -> container.stops.search(q) },
                    awaitOnline = { container.http.awaitOnline() },
                    onPick = { stop ->
                        container.savedTrips.rememberStop(stop)
                        if (isOrigin) origin = stop else destination = stop
                        pop()
                    },
                    onBack = { pop() },
                    excludeStopId = (if (isOrigin) destination else origin)?.id,
                )
            }
            SetupRoute.Preferences -> key(route) {
                TripPreferencesScreen(
                    initial = preferences,
                    showTiming = false, // a widget always shows what's leaving now
                    now = { LocalDateTime.now(ZoneId.of("Australia/Sydney")) },
                    onBack = { pop() },
                    onDone = { p ->
                        preferences = p
                        container.savedTrips.savePlanPreferences(p)
                        pop()
                    },
                )
            }
            SetupRoute.CreateWidget -> key(route) {
                val o = origin
                val d = destination
                if (o == null || d == null) {
                    LaunchedEffect(Unit) { pop() }
                } else {
                    val config = WidgetConfig(o, d, preferences.withoutTiming(), style)
                    LaunchedEffect(config) {
                        if (previewFor == config) return@LaunchedEffect
                        previewFor = config
                        previewSnapshot = null
                        previewLoading = true
                        try {
                            previewSnapshot = when (val r = container.trips.upcoming(o, d, config.preferences)) {
                                is ApiResult.Ok -> r.value.toSnapshot(System.currentTimeMillis())
                                ApiResult.Offline -> null.withError(LoadError.Offline)
                                ApiResult.Unauthorized, ApiResult.NoKey -> null.withError(LoadError.KeyProblem)
                                ApiResult.ServiceUnavailable -> null.withError(LoadError.Unavailable)
                            }
                        } finally {
                            // Interrupted (left the screen mid-load): load again next time.
                            if (previewSnapshot == null) previewFor = null
                            previewLoading = false
                        }
                    }
                    CreateWidgetScreen(
                        config = config,
                        snapshot = previewSnapshot,
                        loading = previewLoading,
                        now = now.value,
                        onBack = { pop() },
                        onOpenInfo = { push(SetupRoute.Info) },
                        onDone = { onDone(config, previewSnapshot?.takeIf { previewFor == config }) },
                    )
                }
            }
            SetupRoute.Info -> key(route) {
                WidgetInfoScreen(onBack = { pop() })
            }
        }
    }
}
