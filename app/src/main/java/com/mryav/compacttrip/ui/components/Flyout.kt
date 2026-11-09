package com.mryav.compacttrip.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.mryav.compacttrip.ui.theme.Palette

/**
 * Bottom sheet over a dimmed screen (pick lists, the trip menu). Slides up from the bottom edge with
 * the app's expand easing while the backdrop fades in; slides back down with the collapse easing.
 * A tap on the backdrop calls [onDismiss]. While shown, focus can't leave the sheet; once it's
 * closing, it can (so the caller can hand focus back).
 *
 * The backdrop isn't `clickable` on purpose: that would make it focusable.
 */
@Composable
fun Flyout(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shown by rememberUpdatedState(visible)
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(Motion.EXPAND_MS, easing = Motion.Expand)),
            exit = fadeOut(tween(Motion.COLLAPSE_MS, easing = Motion.Collapse)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) { detectTapGestures { if (shown) onDismiss() } },
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(Motion.EXPAND_MS, easing = Motion.Expand)) { height -> height },
            exit = slideOutVertically(tween(Motion.COLLAPSE_MS, easing = Motion.Collapse)) { height -> height },
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) { detectTapGestures { } } // taps on the sheet itself don't close it
                    .background(Palette.Page)
                    .navigationBarsPadding()
                    .focusProperties {
                        onEnter = { if (!shown) cancelFocusChange() }
                        onExit = { if (shown) cancelFocusChange() }
                    }
                    .focusGroup(),
                content = content,
            )
        }
    }
}
