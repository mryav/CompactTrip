package com.mryav.compacttrip.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.BoardState
import com.mryav.compacttrip.data.model.DepartureCard
import com.mryav.compacttrip.data.model.SavedTrip
import com.mryav.compacttrip.ui.components.ActionRow
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.AppBarIcon
import com.mryav.compacttrip.ui.components.TripSection
import com.mryav.compacttrip.ui.components.RefreshTag
import com.mryav.compacttrip.ui.components.rememberPullRefreshState
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.theme.CompactTripsTheme
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.du

/** Things the user can trigger on Home. */
enum class HomeAction(val label: String) {
    AddTrip("Add trip"),
    PlanTrip("Plan new trip"),
    Settings("Settings"),
    Refresh("Refresh"),
}

/**
 * Home (Figma 4:1750 empty, 4:944 with trips; focus order 22:759).
 * Header first ("+" is the first focus, per Figma; Settings is LEFT of it), then downwards.
 */
@Composable
fun HomeScreen(
    trips: List<SavedTrip>,
    board: BoardState,
    now: State<Long>,
    onAction: (HomeAction) -> Unit,
    onOpenTrip: (SavedTrip) -> Unit,
    onOpenNotices: (SavedTrip) -> Unit,
    onOpenDeparture: (SavedTrip, DepartureCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialFocus = rememberInitialFocus()
    val pull = rememberPullRefreshState(refreshing = board.refreshing, onRefresh = { onAction(HomeAction.Refresh) })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Page)
            .focusEntry(initialFocus),
    ) {
        AppBar(title = "CompactTrips") {
            AppBarIcon(R.drawable.ic_tune, "Settings", onClick = { onAction(HomeAction.Settings) })
            AppBarIcon(
                R.drawable.ic_add_2,
                "Add trip",
                onClick = { onAction(HomeAction.AddTrip) },
                modifier = Modifier.focusRequester(initialFocus),
            )
            AppBarIcon(R.drawable.ic_assistant_direction, "Plan new trip", onClick = { onAction(HomeAction.PlanTrip) })
            if (trips.isNotEmpty()) {
                AppBarIcon(
                    R.drawable.ic_refresh,
                    "Refresh",
                    onClick = { onAction(HomeAction.Refresh) },
                    busy = board.refreshing,
                    busyDescription = "Refreshing",
                )
            }
        }

        // Trips scroll; the action rows sit at the bottom when there's room, after the trips otherwise.
        // The collapsible "Updated …" tag sits above the feed and pushes it down while open. The min
        // height is the whole body (constant), so opening the tag never re-subcomposes this box.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            val viewport = maxHeight
            Column(modifier = Modifier.fillMaxSize()) {
                if (trips.isNotEmpty()) RefreshTag(state = pull, updatedAt = board.updatedAt, now = now)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(if (trips.isNotEmpty()) Modifier.nestedScroll(pull.connection) else Modifier)
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = viewport)
                        .padding(vertical = if (trips.isEmpty()) 16.du else 8.du),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.du)) {
                        trips.forEach { trip ->
                            androidx.compose.runtime.key(trip.id) {
                                TripSection(
                                    origin = trip.origin,
                                    destination = trip.destination,
                                    load = board.loads[trip.id],
                                    now = now,
                                    onOpen = { onOpenTrip(trip) },
                                    onNotices = { onOpenNotices(trip) },
                                    onCard = { onOpenDeparture(trip, it) },
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.padding(top = 16.du),
                        verticalArrangement = Arrangement.spacedBy(2.du),
                    ) {
                        ActionRow(
                            label = "Add trip to home page",
                            icon = R.drawable.ic_add_2,
                            onClick = { onAction(HomeAction.AddTrip) },
                        )
                        ActionRow(
                            label = "Plan new trip",
                            icon = R.drawable.ic_assistant_direction,
                            onClick = { onAction(HomeAction.PlanTrip) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "RUIO Joy 480x640", widthDp = 320, heightDp = 427)
@Composable
private fun HomeScreenPreview() {
    val now = remember { mutableLongStateOf(0L) }
    CompactTripsTheme {
        HomeScreen(
            trips = emptyList(),
            board = BoardState(),
            now = now,
            onAction = {},
            onOpenTrip = {},
            onOpenNotices = {},
            onOpenDeparture = { _, _ -> },
        )
    }
}
