package com.mryav.compacttrip.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import com.mryav.compacttrip.data.model.Notice
import com.mryav.compacttrip.data.model.SavedTrip
import com.mryav.compacttrip.ui.components.AlertBanner
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.BackButton
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/** Notices affecting a trip (Figma 11:1906). Red for high/veryHigh priority, blue otherwise. */
@Composable
fun NoticesScreen(
    trip: SavedTrip,
    notices: List<Notice>,
    onBack: () -> Unit,
    onOpenNotice: (Notice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialFocus = rememberInitialFocus()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Page)
            .focusEntry(initialFocus),
    ) {
        AppBar(
            title = "Notices",
            navigation = { BackButton(onClick = onBack, modifier = Modifier.focusRequester(initialFocus)) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(vertical = 16.du),
            verticalArrangement = Arrangement.spacedBy(2.du),
        ) {
            BasicText(
                "Affecting ${trip.origin.shortName} to ${trip.destination.shortName}",
                style = Type.label(Palette.BodyText),
                modifier = Modifier.padding(start = 8.du, end = 8.du, top = 8.du, bottom = 4.du),
            )
            if (notices.isEmpty()) {
                BasicText(
                    "No current notices",
                    style = Type.body(Palette.MutedText),
                    modifier = Modifier.padding(horizontal = 16.du, vertical = 20.du),
                )
            }
            notices.forEach { notice ->
                AlertBanner(text = notice.title, urgent = notice.urgent, onClick = { onOpenNotice(notice) })
            }
        }
    }
}
