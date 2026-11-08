package com.mryav.compacttrip.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.R
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/**
 * Dark top bar: optional back button, title, then actions (Figma: 16 padding, 12 gap, actions 24 apart).
 * Focus order rule: header items are always first in focus, left to right, then focus moves down.
 */
@Composable
fun AppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Palette.AppBar)
            .statusBarsPadding()
            .padding(16.du),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.du),
    ) {
        navigation?.invoke()
        BasicText(
            text = title,
            modifier = Modifier.weight(1f),
            style = Type.appBar(Palette.OnDark),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.du),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/** 40×40 header icon. Focused: orange rounded (5) square behind the icon. */
@Composable
fun AppBarIcon(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier.size(40.du),
        shape = RoundedCornerShape(5.du),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(Palette.OnDark),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppBarIcon(R.drawable.ic_arrow_back, "Back", onClick, modifier)
}

/** "✓ Done" header action. Disabled: 40% opacity and skipped by focus. */
@Composable
fun DoneAction(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Done",
) {
    FocusSurface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.alpha(if (enabled) 1f else 0.4f),
        shape = RoundedCornerShape(5.du),
    ) { focused ->
        val tint = if (focused) Palette.FocusedContent else Palette.OnDarkSubtle
        Row(
            modifier = Modifier.padding(end = 4.du),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.du),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(40.du),
            )
            BasicText(text = label, style = Type.condensed(tint), maxLines = 1)
        }
    }
}
