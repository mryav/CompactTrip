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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Roboto
import com.mryav.compacttrip.ui.theme.dsp
import com.mryav.compacttrip.ui.theme.du

/**
 * Dark top bar: title on the left, icon actions on the right (Figma: 16 padding, 12 gap, actions 24 apart).
 * Focus order rule: header items are always first in focus, left to right, then focus moves down.
 */
@Composable
fun AppBar(
    title: String,
    modifier: Modifier = Modifier,
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
        BasicText(
            text = title,
            modifier = Modifier.weight(1f),
            style = TextStyle(color = Palette.OnDark, fontFamily = Roboto, fontSize = 30.dsp),
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
