package com.mryav.compacttrip.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.util.lerp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.LoadError
import com.mryav.compacttrip.data.TripLoad
import com.mryav.compacttrip.data.model.DepartureCard
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.data.model.TripSummary
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/**
 * One saved trip on Home (Figma 4:944, focus order 22:759): the section itself (focusable),
 * then its notices (focusable), then the departure cards. DOWN from the section enters the notices
 * or the carousel; UP from a card returns to the notices or the section.
 *
 * First load (Figma 24:1843): a full-width placeholder card, then the placeholder morphs into the
 * first card while the rest slide in and the notice line expands in and fades up, then each card's
 * content is revealed. Data that's already there when the section appears is shown immediately.
 */
@Composable
fun TripSection(
    origin: Stop,
    destination: Stop,
    load: TripLoad?,
    now: State<Long>,
    onOpen: () -> Unit,
    onNotices: () -> Unit,
    onCard: (DepartureCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val section = remember { FocusRequester() }
    val notices = remember { FocusRequester() }
    val carousel = remember { FocusRequester() }
    val summary = load?.summary
    val hasNotices = summary != null && summary.noticeCount > 0
    val hasCards = summary != null && summary.departures.isNotEmpty()

    val morph = remember { Animatable(if (summary != null) 1f else 0f) }
    val reveal = remember { Animatable(if (summary != null) 1f else 0f) }
    LaunchedEffect(summary != null) {
        if (summary == null || morph.value >= 1f) return@LaunchedEffect
        morph.animateTo(1f, tween(Motion.EXPAND_MS, easing = Motion.Expand))
        reveal.animateTo(1f, tween(revealDurationMs(summary.departures.size), easing = LinearEasing))
    }

    FocusSurface(
        onClick = onOpen,
        color = Palette.Card,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(section)
            .focusProperties {
                down = when {
                    hasNotices -> notices
                    hasCards -> carousel
                    else -> FocusRequester.Default
                }
            },
    ) { focused ->
        val titleColor = if (focused) Palette.FocusedContent else Palette.TitleText
        Column(
            modifier = Modifier.padding(vertical = 20.du),
            verticalArrangement = Arrangement.spacedBy(12.du),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.du)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.du)) {
                    BasicText(origin.shortName, style = Type.title(titleColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicText(destination.shortName, style = Type.title(titleColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (summary != null && hasNotices) {
                    // Expands downwards (with its top gap) and fades up during the morph.
                    Box(
                        modifier = Modifier
                            .expandIn { morph.value }
                            .graphicsLayer { alpha = morph.value }
                            .padding(top = 8.du),
                    ) {
                        NoticeLine(
                            summary = summary,
                            sectionFocused = focused,
                            onClick = onNotices,
                            modifier = Modifier
                                .focusRequester(notices)
                                .focusProperties { up = section },
                        )
                    }
                }
                StatusLine(load = load, sectionFocused = focused)
            }
            if (load == null) DepartureLoadingPlaceholder()
            if (summary != null && hasCards) {
                DepartureCarousel(
                    cards = summary.departures,
                    now = now,
                    onCardClick = onCard,
                    upTarget = if (hasNotices) notices else section,
                    morph = { morph.value },
                    reveal = { reveal.value },
                    modifier = Modifier
                        .focusRequester(carousel)
                        .focusGroup(),
                )
            }
        }
    }
}

/** "ⓘ 1 notice" (blue), or for a high-priority alert its headline in red plus "+N more". */
@Composable
private fun NoticeLine(summary: TripSummary, sectionFocused: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusSurface(onClick = onClick, shape = RoundedCornerShape(6.du), modifier = modifier) { focused ->
        val white = focused || sectionFocused
        val headline = summary.firstNotice
        if (summary.urgent && headline != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.du)) {
                BasicText(
                    headline,
                    style = Type.headsign(if (white) Palette.FocusedContent else Palette.AlertForeground),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (summary.noticeCount > 1) {
                    BasicText(
                        "+${summary.noticeCount - 1} more",
                        style = Type.secondary(if (white) Palette.FocusedContent else Palette.MutedText),
                    )
                }
            }
        } else {
            val color = if (white) Palette.FocusedContent else Palette.NoticeText
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.du)) {
                Image(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color),
                    modifier = Modifier.size(24.du),
                )
                val n = summary.noticeCount
                BasicText(if (n == 1) "1 notice" else "$n notices", style = Type.secondary(color))
            }
        }
    }
}

/** Loading / error / no-service line. Stale data stays visible above it. */
@Composable
private fun StatusLine(load: TripLoad?, sectionFocused: Boolean) {
    val summary = load?.summary
    val text = when {
        load == null -> return // the placeholder card shows loading
        load.error == LoadError.KeyProblem -> "Check your API key in Settings"
        load.error == LoadError.Offline -> if (summary == null) "Offline. Connect to see departures." else "Offline. Showing last update."
        load.error == LoadError.Unavailable -> if (summary == null) "Transport NSW isn't responding" else "Couldn't refresh. Showing last update."
        summary != null && summary.departures.isEmpty() && summary.walkMinutes != null ->
            "Walking distance: ${summary.walkMinutes}min"
        summary != null && summary.departures.isEmpty() -> "No upcoming services"
        else -> return
    }
    BasicText(
        text,
        style = Type.secondary(if (sectionFocused) Palette.FocusedContent else Palette.MutedText),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.du),
    )
}

/** Layout-only height reveal: the content is placed at the top and cropped by the growing height. */
private fun Modifier.expandIn(fraction: () -> Float): Modifier = clipToBounds().layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val height = lerp(0, placeable.height, fraction().coerceIn(0f, 1f))
    layout(placeable.width, height) { placeable.place(0, 0) }
}
