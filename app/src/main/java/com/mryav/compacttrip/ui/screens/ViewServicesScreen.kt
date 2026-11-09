package com.mryav.compacttrip.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.LoadError
import com.mryav.compacttrip.data.TripLoad
import com.mryav.compacttrip.data.model.Journey
import com.mryav.compacttrip.data.model.SavedTrip
import com.mryav.compacttrip.ui.components.ActionRow
import com.mryav.compacttrip.ui.components.Flyout
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.AppBarIcon
import com.mryav.compacttrip.ui.components.BackButton
import com.mryav.compacttrip.ui.components.NoticesChip
import com.mryav.compacttrip.ui.components.ServiceRow
import com.mryav.compacttrip.ui.components.UpdatedLabel
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/**
 * View Services (Figma 6:600): every upcoming journey for a saved trip. The Updated label and the
 * trip header stay pinned while the list scrolls. ⋮ (or the Menu key) offers removing the trip.
 */
@Composable
fun ViewServicesScreen(
    trip: SavedTrip,
    load: TripLoad?,
    updatedAt: Long?,
    refreshing: Boolean,
    now: State<Long>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenJourney: (Journey) -> Unit,
    onOpenNotices: () -> Unit,
    onRemove: () -> Unit,
    onPreferences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val summary = load?.summary
    val initialFocus = rememberInitialFocus()
    val menuFocus = remember { FocusRequester() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Palette.Page)
                // While the menu is open, focus belongs in it (see TripPreferencesScreen's pick list).
                .focusProperties { onEnter = { (if (menuOpen) menuFocus else initialFocus).requestFocus() } }
                .focusGroup()
                .onPreviewKeyEvent { event ->
                    if (!menuOpen) return@onPreviewKeyEvent false
                    // Key down and up: the first press after a touch only leaves touch mode on key down.
                    runCatching { menuFocus.requestFocus() }
                    true
                }
                .onKeyEvent { event ->
                    if (event.key == Key.Menu && event.type == KeyEventType.KeyUp) {
                        menuOpen = true
                        true
                    } else {
                        false
                    }
                },
        ) {
            AppBar(
                title = "View Services",
                navigation = { BackButton(onClick = onBack, modifier = Modifier.focusRequester(initialFocus)) },
            ) {
                AppBarIcon(R.drawable.ic_refresh, "Refresh", onClick = onRefresh, busy = refreshing, busyDescription = "Refreshing")
                AppBarIcon(R.drawable.ic_more_vert, "More options", onClick = { menuOpen = true })
            }
            // Pinned header.
            UpdatedLabel(updatedAt = updatedAt, refreshing = refreshing, now = now)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Palette.Card)
                    .padding(horizontal = 16.du, vertical = 20.du),
                verticalArrangement = Arrangement.spacedBy(12.du),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.du)) {
                    BasicText(trip.origin.shortName, style = Type.title(Palette.TitleText), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicText(trip.destination.shortName, style = Type.title(Palette.TitleText), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (summary != null && summary.notices.isNotEmpty()) {
                    NoticesChip(notices = summary.notices, onClick = onOpenNotices)
                }
            }
            // Scrolling services.
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(2.du),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 2.du, bottom = 16.du),
            ) {
                val message = when {
                    load == null -> "Loading services…"
                    summary == null && load.error == LoadError.Offline -> "Offline. Connect to see services."
                    summary == null && load.error == LoadError.KeyProblem -> "Check your API key in Settings"
                    summary == null -> "Transport NSW isn't responding"
                    summary.journeys.isEmpty() -> "No upcoming services"
                    else -> null
                }
                if (message != null) {
                    item(key = "message") {
                        BasicText(
                            message,
                            style = Type.condensed(Palette.MutedText),
                            modifier = Modifier.padding(horizontal = 16.du, vertical = 20.du),
                        )
                    }
                }
                items(summary?.journeys.orEmpty(), key = { it.key }) { journey ->
                    ServiceRow(journey = journey, now = now, onClick = { onOpenJourney(journey) })
                }
            }
        }

        // Always composed so it can slide out.
        Flyout(visible = menuOpen, onDismiss = { menuOpen = false }) {
            TripMenu(
                trip = trip,
                cancel = menuFocus,
                onPreferences = {
                    menuOpen = false
                    onPreferences()
                },
                onRemove = {
                    menuOpen = false
                    onRemove()
                },
                onDismiss = { menuOpen = false },
            )
        }
    }
}

/**
 * Options sheet (no Figma design yet; built in the app's style). Focus is trapped inside while it's
 * open and starts on Cancel, so removing a trip always takes a deliberate move.
 */
@Composable
private fun TripMenu(
    trip: SavedTrip,
    cancel: FocusRequester,
    onPreferences: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
        repeat(10) {
            withFrameNanos { }
            if (runCatching { cancel.requestFocus(FocusDirection.Enter) }.getOrDefault(false)) return@LaunchedEffect
        }
    }
    BackHandler(onBack = onDismiss)
    Column(verticalArrangement = Arrangement.spacedBy(2.du)) {
        BasicText(
            trip.title,
            style = Type.condensed24(Palette.BodyText),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.du, end = 16.du, top = 16.du, bottom = 8.du),
        )
        ActionRow(
            label = "Trip preferences",
            subtitle = trip.preferences.summary(java.time.LocalDate.now()),
            icon = R.drawable.ic_arrow_forward,
            onClick = onPreferences,
        )
        ActionRow(label = "Remove trip from home", icon = null, onClick = onRemove)
        ActionRow(label = "Cancel", icon = null, onClick = onDismiss, modifier = Modifier.focusRequester(cancel))
    }
}
