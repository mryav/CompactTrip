package com.mryav.compacttrip.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import com.mryav.compacttrip.R
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du
import kotlin.math.roundToInt

enum class ConnectionStatus { Hidden, Checking, Success, Unsuccessful, Offline, ServiceUnavailable }

/**
 * API key connection status (Figma 22:1562). Starts collapsed. Each status change collapses the
 * container (if open), swaps the content, then expands it downwards. The content is anchored to the
 * top and cropped by the growing container, so it's revealed top-to-bottom. The height is animated
 * in the layout phase only (no recomposition per frame).
 */
@Composable
fun ConnectionStatusBanner(status: ConnectionStatus, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(ConnectionStatus.Hidden) }
    val reveal = remember { Animatable(0f) }

    LaunchedEffect(status) {
        if (status == shown && reveal.value == 1f) return@LaunchedEffect
        if (reveal.value > 0f) reveal.animateTo(0f, tween(Motion.COLLAPSE_MS, easing = Motion.Collapse))
        shown = status
        if (status != ConnectionStatus.Hidden) {
            reveal.animateTo(1f, tween(Motion.EXPAND_MS, easing = Motion.Expand))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                val height = (placeable.height * reveal.value).roundToInt()
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        StatusContent(shown)
    }
}

@Composable
private fun StatusContent(status: ConnectionStatus) {
    when (status) {
        ConnectionStatus.Hidden -> Unit
        ConnectionStatus.Checking -> StatusSpinner()
        ConnectionStatus.Success -> StatusMessage(
            title = "Successful connection",
            detail = "Your API key is functional",
            icon = R.drawable.ic_check_circle,
            background = Palette.SuccessBackground,
            foreground = Palette.SuccessForeground,
        )
        ConnectionStatus.Unsuccessful -> StatusMessage(
            title = "Unsuccessful connection",
            detail = "Check your API key. Click the button above for more help.",
            icon = R.drawable.ic_error,
            background = Palette.AlertHighBackground,
            foreground = Palette.AlertForeground,
        )
        ConnectionStatus.Offline -> StatusMessage(
            title = "Offline",
            detail = "Connect to the internet to continue",
            icon = R.drawable.ic_cloud_off,
            background = Palette.NeutralStatusBackground,
            foreground = Palette.NeutralStatusForeground,
        )
        ConnectionStatus.ServiceUnavailable -> StatusMessage(
            title = "Can't reach Transport NSW",
            detail = "Their service isn't responding. Try again in a moment.",
            icon = R.drawable.ic_error,
            background = Palette.NeutralStatusBackground,
            foreground = Palette.NeutralStatusForeground,
        )
    }
}

/** Grey status block with the platform spinner (checking, retrying). */
@Composable
fun StatusSpinner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Palette.NeutralStatusBackground)
            .padding(horizontal = 16.du, vertical = 20.du),
        contentAlignment = Alignment.Center,
    ) {
        LoadingSpinner(color = Palette.NeutralStatusForeground)
    }
}

/** Status block: title and detail on the left, a 40 icon on the right (Figma 22:1562, 25:2749). */
@Composable
fun StatusMessage(
    title: String,
    detail: String,
    @DrawableRes icon: Int,
    background: Color,
    foreground: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.du, vertical = 20.du),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.du),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.du)) {
            BasicText(text = title, style = Type.condensed(foreground))
            BasicText(text = detail, style = Type.secondary(foreground))
        }
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(foreground),
            modifier = Modifier.size(40.du),
        )
    }
}
