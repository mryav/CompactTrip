package com.mryav.compacttrip.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.mryav.compacttrip.data.ApiKeyChecker
import com.mryav.compacttrip.data.ApiKeyStore
import com.mryav.compacttrip.ui.screens.ApiKeyScreen
import com.mryav.compacttrip.ui.screens.HomeAction
import com.mryav.compacttrip.ui.screens.HomeScreen
import com.mryav.compacttrip.ui.screens.SettingsScreen
import com.mryav.compacttrip.ui.screens.USER_GUIDE_URL
import com.mryav.compacttrip.ui.screens.WelcomeScreen

enum class Route { Welcome, ApiKeySetup, ApiKeyEdit, Home, Settings }

/** App root: a tiny back stack (no navigation library), instant screen switches. */
@Composable
fun CompactTripsApp(
    keyStore: ApiKeyStore,
    keyChecker: ApiKeyChecker,
    openUrl: (String) -> Unit,
    showMessage: (String) -> Unit,
) {
    val stack: SnapshotStateList<Route> = rememberSaveable(
        saver = listSaver(save = { s -> s.map { it.name } }, restore = { s -> s.map(Route::valueOf).toMutableStateList() }),
    ) { mutableStateListOf(if (keyStore.hasKey) Route.Home else Route.Welcome) }

    fun push(route: Route) = stack.add(route)
    fun pop() = stack.removeAt(stack.lastIndex)
    fun resetTo(route: Route) {
        stack.clear()
        stack.add(route)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    // key(): each screen gets fresh state when it's (re)entered.
    when (val route = stack.last()) {
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
                    resetTo(Route.Home)
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
                onAction = { action ->
                    when (action) {
                        HomeAction.Settings -> push(Route.Settings)
                        else -> showMessage(action.label)
                    }
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
    }
}
