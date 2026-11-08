package com.mryav.compacttrip.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import com.mryav.compacttrip.R
import com.mryav.compacttrip.ui.components.ActionRow
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.BackButton
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.du

/** Settings (Figma 22:1518). */
@Composable
fun SettingsScreen(
    apiKeyWorking: Boolean,
    onBack: () -> Unit,
    onEditApiKey: () -> Unit,
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
            title = "Settings",
            navigation = { BackButton(onClick = onBack, modifier = Modifier.focusRequester(initialFocus)) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.du),
            verticalArrangement = Arrangement.spacedBy(2.du),
        ) {
            ActionRow(
                label = "API key",
                subtitle = if (apiKeyWorking) {
                    "Your API key is functional. Edit or delete your API key."
                } else {
                    "Your API key couldn't be verified. Edit or delete your API key."
                },
                icon = R.drawable.ic_arrow_forward,
                onClick = onEditApiKey,
            )
        }
    }
}
