package com.mryav.compacttrip.ui.components

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties

/**
 * Focus order rule: header items first (left to right), then downwards. Attach the returned
 * requester to the screen's first item; it gets focus when the screen opens (keypad mode).
 */
@Composable
fun rememberInitialFocus(): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(requester) { requester.requestFocus() }
    return requester
}

/** Screen root: the first D-pad press with nothing focused lands on [initial]. */
fun Modifier.focusEntry(initial: FocusRequester): Modifier =
    focusProperties { onEnter = { initial.requestFocus() } }.focusGroup()
