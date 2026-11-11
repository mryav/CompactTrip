package com.mryav.compacttrip.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.mryav.compacttrip.R
import com.mryav.compacttrip.ui.components.ActionRow
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.theme.CompactTripsTheme
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/** First run, no API key yet (Figma 22:1431). */
@Composable
fun WelcomeScreen(onSetUpKey: () -> Unit, modifier: Modifier = Modifier) {
    val initialFocus = rememberInitialFocus()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Page)
            .focusEntry(initialFocus),
    ) {
        AppBar(title = "CompactTrips")
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 32.du),
            verticalArrangement = Arrangement.spacedBy(2.du),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.du),
                verticalArrangement = Arrangement.spacedBy(10.du),
            ) {
                BasicText("Welcome to CompactTrips!", style = Type.title(Palette.TitleText))
                BasicText("One place for all your trips inside NSW.", style = Type.body(Palette.BodyText))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.du),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicText(
                    "You’ll need an API key from Transport NSW to continue.",
                    style = Type.body(Palette.BodyText),
                )
            }
            Spacer(Modifier.weight(1f))
            ActionRow(
                label = "Set up API key",
                icon = R.drawable.ic_arrow_forward,
                onClick = onSetUpKey,
                modifier = Modifier.focusRequester(initialFocus),
            )
        }
    }
}

@Preview(name = "RUIO Joy 480x640", widthDp = 320, heightDp = 427)
@Composable
private fun WelcomePreview() {
    CompactTripsTheme { WelcomeScreen(onSetUpKey = {}) }
}
