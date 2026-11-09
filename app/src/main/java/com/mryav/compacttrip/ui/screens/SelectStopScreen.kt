package com.mryav.compacttrip.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.ApiResult
import com.mryav.compacttrip.data.RetryStatus
import com.mryav.compacttrip.data.loadWithRetry
import com.mryav.compacttrip.data.retryingIn
import com.mryav.compacttrip.data.model.Mode
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.BackButton
import com.mryav.compacttrip.ui.components.CollapsingSwap
import com.mryav.compacttrip.ui.components.DesignTextField
import com.mryav.compacttrip.ui.components.Motion
import com.mryav.compacttrip.ui.components.ModeIcon
import com.mryav.compacttrip.ui.components.StatusMessage
import com.mryav.compacttrip.ui.components.StatusSpinner
import com.mryav.compacttrip.ui.components.StopRow
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du
import kotlinx.coroutines.delay

private const val SEARCH_DEBOUNCE_MS = 300L
private const val MIN_QUERY = 2

/** Status block above the results (Figma 25:2884). */
private sealed interface SearchStatus {
    /** Slow search (after [Motion.SPINNER_DELAY_MS]) or retrying: the supplemental spinner block. */
    data object Spinner : SearchStatus
    data class Failed(val retryInSeconds: Int) : SearchStatus
    data object Offline : SearchStatus
    data object NoStops : SearchStatus
    data object KeyProblem : SearchStatus
}

/** Modes searched; shown under the field. All modes until Select modes exists. */
private val ALL_MODES = listOf(Mode.Metro, Mode.LightRail, Mode.Bus, Mode.Train, Mode.Ferry)

/**
 * Select Origin / Destination (Figma 4:2327, 4:2096, 4:2521). Results above, search pinned to the
 * bottom next to the keypad. Focus starts in the search bar. An empty query shows recently picked stops. The stop already chosen
 * for the other end of the trip is never offered.
 */
@Composable
fun SelectStopScreen(
    title: String,
    recentStops: List<Stop>,
    search: suspend (String) -> ApiResult<List<Stop>>,
    awaitOnline: suspend () -> Unit,
    onPick: (Stop) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** The stop chosen for the other end of the trip; never offered here. */
    excludeStopId: String? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Stop>?>(null) }
    var status by remember { mutableStateOf<SearchStatus?>(null) }
    var searching by remember { mutableStateOf(false) }
    var showSpinner by remember { mutableStateOf(false) }

    // Spinner only for slow searches (Figma 24:1563 rule): no animation, nothing for quick ones.
    LaunchedEffect(searching) {
        showSpinner = false
        if (searching) {
            delay(Motion.SPINNER_DELAY_MS)
            showSpinner = true
        }
    }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < MIN_QUERY) {
            results = null
            status = null
            return@LaunchedEffect
        }
        delay(SEARCH_DEBOUNCE_MS)
        searching = true
        // Previous results stay until this search answers; errors clear them (they'd be for another query).
        val r = try {
            loadWithRetry(
                load = { search(q) },
                awaitOnline = awaitOnline,
                onStatus = { s ->
                    if (s !is RetryStatus.Retrying) results = null
                    status = when (s) {
                        RetryStatus.Offline -> SearchStatus.Offline
                        is RetryStatus.Waiting -> SearchStatus.Failed(s.seconds)
                        RetryStatus.Retrying -> SearchStatus.Spinner
                    }
                },
            )
        } finally {
            searching = false
        }
        if (r is ApiResult.Ok) {
            results = r.value
            status = if (r.value.none { it.id != excludeStopId }) SearchStatus.NoStops else null
        } else {
            results = null
            status = SearchStatus.KeyProblem
        }
    }

    // Recent stops only while nothing has been typed; a search shows only its own results/messages.
    val searchingFor = query.trim().length >= MIN_QUERY
    val shown = (if (searchingFor) results.orEmpty() else recentStops).filterNot { it.id == excludeStopId }
    val keyboard = LocalSoftwareKeyboardController.current
    val initialFocus = rememberInitialFocus()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Page)
            .focusEntry(initialFocus),
    ) {
        AppBar(
            title = title,
            navigation = { BackButton(onClick = onBack) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(vertical = 16.du),
            verticalArrangement = Arrangement.spacedBy(2.du),
        ) {
            // Outside the list: a list item inserted above the visible rows would be scrolled out of view.
            CollapsingSwap(
                target = status ?: if (showSpinner && searchingFor) SearchStatus.Spinner else null,
                kind = { it::class },
            ) { shown -> SearchStatusBlock(shown) }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.du),
            ) {
                if (!searchingFor && shown.isNotEmpty()) {
                    item(key = "recent") {
                        BasicText(
                            "Recent",
                            style = Type.condensed24(Palette.BodyText),
                            modifier = Modifier.padding(horizontal = 8.du, vertical = 4.du),
                        )
                    }
                }
                items(shown, key = { it.id }) { stop -> StopRow(stop = stop, onClick = { onPick(stop) }) }
            }
            DesignTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search for service",
                trailingIcon = R.drawable.ic_search,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search,
                ),
                onDone = { keyboard?.hide() },
                // Exception to the header-first rule (user request): searching is the point of this
                // screen, so focus starts in the search bar and keypad typing works immediately.
                modifier = Modifier.focusRequester(initialFocus),
                below = {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.du)) {
                        ALL_MODES.forEach { ModeIcon(it, size = 28.du) }
                    }
                },
            )
        }
    }
}

@Composable
private fun SearchStatusBlock(status: SearchStatus) {
    when (status) {
        SearchStatus.Spinner -> StatusSpinner()
        is SearchStatus.Failed -> StatusMessage(
            title = "Connection error",
            detail = retryingIn(status.retryInSeconds),
            icon = R.drawable.ic_cloud_alert,
            background = Palette.AlertHighBackground,
            foreground = Palette.AlertForeground,
        )
        SearchStatus.KeyProblem -> StatusMessage(
            title = "Couldn’t search",
            detail = "Check your API key in Settings",
            icon = R.drawable.ic_cloud_alert,
            background = Palette.AlertHighBackground,
            foreground = Palette.AlertForeground,
        )
        SearchStatus.Offline -> StatusMessage(
            title = "Offline",
            detail = "Connect to the internet to search",
            icon = R.drawable.ic_cloud_off,
            background = Palette.NeutralStatusBackground,
            foreground = Palette.NeutralStatusForeground,
        )
        SearchStatus.NoStops -> StatusMessage(
            title = "No stops found",
            detail = "Couldn’t find anything",
            icon = R.drawable.ic_wrong_location,
            background = Palette.NeutralStatusBackground,
            foreground = Palette.NeutralStatusForeground,
        )
    }
}
