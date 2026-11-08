package com.mryav.compacttrip.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.mryav.compacttrip.R
import com.mryav.compacttrip.ui.components.ActionRow
import com.mryav.compacttrip.ui.components.AlertBanner
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.AppBarIcon
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.theme.CompactTripsTheme
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.du

/** Things the user can trigger on Home. No behaviour yet: the caller just reports them. */
enum class HomeAction(val label: String) {
    AddTrip("Add trip"),
    PlanTrip("Plan new trip"),
    OpenAlert("Network alert"),
    Settings("Settings"),
}

/** Home — Empty State (Figma nodes 4:1750, header updated in 21:177). */
@Composable
fun HomeScreen(
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialFocus = rememberInitialFocus()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Page)
            // Header first, then downwards. Figma marks "+" as the first item in focus;
            // Settings (tune) sits to its left and is reached with D-pad LEFT.
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
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 16.du),
            verticalArrangement = Arrangement.spacedBy(2.du),
        ) {
            AlertBanner(
                text = "Major line disruptions affecting whole network",
                onClick = { onAction(HomeAction.OpenAlert) },
            )
            Spacer(Modifier.weight(1f))
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

@Preview(name = "RUIO Joy 480x640", widthDp = 320, heightDp = 427)
@Composable
private fun HomeScreenPreview() {
    CompactTripsTheme { HomeScreen(onAction = {}) }
}
