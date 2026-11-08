package com.mryav.compacttrip.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.mryav.compacttrip.R
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/** High / veryHigh priority service alert banner (red). Figma: "veryHigh or High priority banner". */
@Composable
fun AlertBanner(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = Palette.AlertHighBackground,
    ) { focused ->
        val content = if (focused) Palette.FocusedContent else Palette.AlertForeground
        Row(
            modifier = Modifier.padding(horizontal = 16.du, vertical = 20.du),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.du),
        ) {
            BasicText(
                text = text,
                modifier = Modifier.weight(1f),
                style = Type.condensed(content),
            )
            Image(
                painter = painterResource(R.drawable.ic_arrow_forward_ios),
                contentDescription = null,
                colorFilter = ColorFilter.tint(content),
                modifier = Modifier.size(40.du),
            )
        }
    }
}
