package com.mryav.compacttrip.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.ApiKeyChecker
import com.mryav.compacttrip.data.KeyCheckResult
import com.mryav.compacttrip.ui.components.ActionRow
import com.mryav.compacttrip.ui.components.ApiKeyField
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.BackButton
import com.mryav.compacttrip.ui.components.ConnectionStatus
import com.mryav.compacttrip.ui.components.ConnectionStatusBanner
import com.mryav.compacttrip.ui.components.DoneAction
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du
import kotlinx.coroutines.delay

const val USER_GUIDE_URL = "https://opendata.transport.nsw.gov.au/developers/userguide"

/** Pause after typing/pasting stops before checking the key. */
private const val CHECK_DEBOUNCE_MS = 900L

/** Keep the spinner up at least this long so the expand → collapse → reveal sequence reads clearly. */
private const val MIN_SPINNER_MS = 900L

/**
 * Insert / edit the API key (Figma 22:1016 and status variants). The key is checked automatically
 * once input settles (or immediately on the keyboard's Done). "Done" in the header is enabled only
 * once the exact key in the field has connected successfully. When editing an existing key
 * ([onDelete] != null), clearing the field turns the header action into "Delete".
 */
@Composable
fun ApiKeyScreen(
    initialKey: String?,
    checker: ApiKeyChecker,
    onBack: () -> Unit,
    onSave: (key: String) -> Unit,
    onOpenUserGuide: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    var key by rememberSaveable { mutableStateOf(initialKey.orEmpty()) }
    var status by remember { mutableStateOf(ConnectionStatus.Hidden) }
    var verifiedKey by remember { mutableStateOf<String?>(null) }
    /** Key that the current result banner describes (null while checking or empty). */
    var resultKey by remember { mutableStateOf<String?>(null) }
    var fieldFocused by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    var checkNow by remember { mutableIntStateOf(0) }
    val lastCheckNow = remember { intArrayOf(0) }

    LaunchedEffect(key, checkNow) {
        verifiedKey = null
        resultKey = null
        if (key.isEmpty()) {
            status = ConnectionStatus.Hidden
            return@LaunchedEffect
        }
        val immediate = checkNow != lastCheckNow[0]
        lastCheckNow[0] = checkNow
        if (!immediate) delay(CHECK_DEBOUNCE_MS)

        status = ConnectionStatus.Checking
        val started = SystemClock.uptimeMillis()
        val result = checker.check(key)
        delay((MIN_SPINNER_MS - (SystemClock.uptimeMillis() - started)).coerceAtLeast(0))
        status = when (result) {
            KeyCheckResult.Success -> ConnectionStatus.Success
            KeyCheckResult.Unauthorized -> ConnectionStatus.Unsuccessful
            KeyCheckResult.Offline -> ConnectionStatus.Offline
            KeyCheckResult.ServiceUnavailable -> ConnectionStatus.ServiceUnavailable
        }
        resultKey = key
        if (result == KeyCheckResult.Success) verifiedKey = key
    }

    // While typing, keep the key field in view as the status banner above it grows/shrinks.
    LaunchedEffect(fieldFocused) {
        if (fieldFocused) snapshotFlow { scroll.maxValue }.collect { scroll.scrollTo(it) }
    }

    val deleting = onDelete != null && key.isEmpty()
    val canFinish = deleting || (verifiedKey != null && verifiedKey == key)

    val keyboard = LocalSoftwareKeyboardController.current
    val initialFocus = rememberInitialFocus()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Page)
            .focusEntry(initialFocus),
    ) {
        AppBar(
            title = "API Key",
            navigation = { BackButton(onClick = onBack, modifier = Modifier.focusRequester(initialFocus)) },
        ) {
            DoneAction(
                enabled = canFinish,
                label = if (deleting) "Delete" else "Done",
                onClick = { if (deleting) onDelete() else onSave(key) },
            )
        }
        // Scrolls when space runs short (on-screen keyboard on a 640 px screen): the top group
        // and the bottom group are pushed apart to fill the screen otherwise, as in Figma.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            val viewportHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .heightIn(min = viewportHeight)
                    .padding(vertical = 16.du),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.du)) {
                    BasicText(
                        text = "Insert a NSW Transport Open Data API key to continue.",
                        style = Type.title(Palette.TitleText),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Palette.Card)
                            .padding(horizontal = 16.du, vertical = 20.du),
                    )
                    ConnectionStatusBanner(status = status)
                }
                Column(
                    modifier = Modifier.padding(top = 2.du),
                    verticalArrangement = Arrangement.spacedBy(2.du),
                ) {
                    ActionRow(
                        label = "Open user guide",
                        icon = R.drawable.ic_arrow_outward,
                        color = Palette.LinkRow,
                        onClick = onOpenUserGuide,
                    )
                    ApiKeyField(
                        value = key,
                        onValueChange = { key = it },
                        onDone = {
                            keyboard?.hide()
                            // Only re-check if this exact key doesn't already have a result.
                            if (key.isNotEmpty() && resultKey != key) checkNow++
                        },
                        modifier = Modifier.onFocusChanged { fieldFocused = it.isFocused },
                    )
                }
            }
        }
    }
}
