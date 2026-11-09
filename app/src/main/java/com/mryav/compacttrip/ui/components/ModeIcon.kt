package com.mryav.compacttrip.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.model.Mode

/** Official TfNSW mode pictogram (Figma 2:8), drawn from 96 px WebPs. */
@Composable
fun ModeIcon(mode: Mode, size: Dp, modifier: Modifier = Modifier) {
    val res = mode.iconRes() ?: return
    Box(
        modifier = modifier
            .size(size)
            // Figma: drop shadow 0 0 1 black — a hairline edge so light icons read on white.
            .drawBehind {
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = this.size.minDimension / 2f - 0.5f,
                    style = Stroke(width = 1f),
                )
            },
    ) {
        Image(
            painter = painterResource(res),
            contentDescription = mode.label(),
            // The metro artwork has no built-in margin; Figma insets it to 88% to match the others.
            modifier = Modifier
                .fillMaxSize()
                .then(if (mode == Mode.Metro) Modifier.padding(size * 0.0576f) else Modifier),
        )
    }
}

/** Modes shown as icons, in Figma's order (M, L, B, T, F). Coaches/school buses use the bus icon. */
fun List<Mode>.iconModes(): List<Mode> {
    val mapped = map { if (it == Mode.Coach || it == Mode.SchoolBus) Mode.Bus else it }.toSet()
    return listOf(Mode.Metro, Mode.LightRail, Mode.Bus, Mode.Train, Mode.Ferry).filter { it in mapped }
}

@DrawableRes
private fun Mode.iconRes(): Int? = when (this) {
    Mode.Train -> R.drawable.mode_train
    Mode.Metro -> R.drawable.mode_metro
    Mode.LightRail -> R.drawable.mode_lightrail
    Mode.Bus, Mode.Coach, Mode.SchoolBus -> R.drawable.mode_bus
    Mode.Ferry -> R.drawable.mode_ferry
    Mode.Walk -> null
}

fun Mode.label(): String = when (this) {
    Mode.Train -> "Train"
    Mode.Metro -> "Metro"
    Mode.LightRail -> "Light rail"
    Mode.Bus -> "Bus"
    Mode.Coach -> "Coach"
    Mode.Ferry -> "Ferry"
    Mode.SchoolBus -> "School bus"
    Mode.Walk -> "Walk"
}
