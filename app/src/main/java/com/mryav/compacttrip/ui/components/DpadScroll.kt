package com.mryav.compacttrip.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import com.mryav.compacttrip.ui.theme.du
import kotlinx.coroutines.launch

/**
 * For screens with long non-interactive text (Service Alert): D-pad DOWN moves focus if there's
 * somewhere to go, otherwise scrolls; UP scrolls back first, then moves focus. Keypad phones have
 * no other way to read text that runs off the screen.
 */
@Composable
fun Modifier.dpadScroll(state: ScrollState): Modifier {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { 200.du.toPx() }
    return onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        when (event.key) {
            Key.DirectionDown -> {
                if (focusManager.moveFocus(FocusDirection.Down)) return@onKeyEvent true
                if (!state.canScrollForward) return@onKeyEvent false
                scope.launch { state.animateScrollBy(step, tween(Motion.COLLAPSE_MS)) }
                true
            }
            Key.DirectionUp -> {
                if (state.canScrollBackward) {
                    scope.launch { state.animateScrollBy(-step, tween(Motion.COLLAPSE_MS)) }
                    true
                } else {
                    focusManager.moveFocus(FocusDirection.Up)
                }
            }
            else -> false
        }
    }
}
