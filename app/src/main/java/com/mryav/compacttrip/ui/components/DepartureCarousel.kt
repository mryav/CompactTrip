package com.mryav.compacttrip.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.lerp
import com.mryav.compacttrip.data.model.DepartureCard
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.du
import kotlinx.coroutines.delay

/** Figma 24:1563: the loading placeholder is shorter than a loaded card. */
private const val PLACEHOLDER_HEIGHT = 155

/** Add trip preview card (Figma 25:2749): one-line headsign, same height as the placeholder. */
const val COMPACT_CARD_HEIGHT = 155

/**
 * Horizontal Quick View cards. D-pad: LEFT/RIGHT between cards (the last card stops), DOWN leaves to
 * the next section, UP goes to [upTarget] (the notice chip or the trip section) when given.
 *
 * Entry animation (Figma 24:1843): [morph] 0→1 shrinks the first card from the full-width loading
 * placeholder to its real width while the others slide in behind it, and grows the cards from the
 * placeholder height to full height. [reveal] 0→1 (linear, over [revealDurationMs]) then uncovers
 * each card's content top-to-bottom, staggered left to right. Both are read in layout/draw only.
 *
 * [onCardClick] = null: cards aren't clickable, only focusable (shadow-only focus). [compact] uses
 * the shorter one-line card ([COMPACT_CARD_HEIGHT]).
 */
@Composable
fun DepartureCarousel(
    cards: List<DepartureCard>,
    now: State<Long>,
    onCardClick: ((DepartureCard) -> Unit)?,
    modifier: Modifier = Modifier,
    upTarget: FocusRequester? = null,
    morph: () -> Float = { 1f },
    reveal: () -> Float = { 1f },
    compact: Boolean = false,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val sidePadding = 16.du
        val fullWidthPx = with(density) { (maxWidth - sidePadding * 2).roundToPx() }
        val fromHeightPx = with(density) { PLACEHOLDER_HEIGHT.du.roundToPx() }
        val toHeightPx = with(density) { (if (compact) COMPACT_CARD_HEIGHT else CARD_HEIGHT).du.roundToPx() }
        val totalReveal = revealDurationMs(cards.size).toFloat()

        LazyRow(
            contentPadding = PaddingValues(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(8.du),
        ) {
            itemsIndexed(cards, key = { _, card -> card.key }) { index, card ->
                QuickViewCard(
                    card = card,
                    now = now,
                    onClick = onCardClick?.let { click -> { click(card) } },
                    compact = compact,
                    contentReveal = {
                        val elapsed = reveal() * totalReveal - index * Motion.STAGGER_MS
                        Motion.Expand.transform((elapsed / Motion.REVEAL_MS).coerceIn(0f, 1f))
                    },
                    modifier = Modifier
                        .morph(morph, if (index == 0) fullWidthPx else null, fromHeightPx, toHeightPx)
                        .then(if (upTarget != null) Modifier.focusProperties { up = upTarget } else Modifier),
                )
            }
        }
    }
}

/** Linear duration of the [reveal] phase: one reveal plus the stagger of the cards on screen. */
fun revealDurationMs(cardCount: Int): Int =
    Motion.REVEAL_MS + Motion.STAGGER_MS * (cardCount.coerceIn(1, 4) - 1)

/**
 * Layout-only morph: height from [fromHeightPx] to [toHeightPx]; when [fromWidthPx] is given, width
 * from it to the item's natural width. At progress 1 this is just the natural size.
 */
private fun Modifier.morph(progress: () -> Float, fromWidthPx: Int?, fromHeightPx: Int, toHeightPx: Int): Modifier =
    layout { measurable, constraints ->
        val p = progress().coerceIn(0f, 1f)
        val height = lerp(fromHeightPx, toHeightPx, p)
        val placeable = if (fromWidthPx != null) {
            val natural = measurable.maxIntrinsicWidth(toHeightPx)
                .coerceIn(constraints.minWidth, constraints.maxWidth)
            measurable.measure(Constraints.fixed(lerp(fromWidthPx, natural, p), height))
        } else {
            measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        }
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

/**
 * Full-width empty card shown while a trip's departures load (Figma 24:1563). The platform spinner
 * appears only if loading takes longer than [Motion.SPINNER_DELAY_MS].
 */
@Composable
fun DepartureLoadingPlaceholder(modifier: Modifier = Modifier) {
    var showSpinner by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(Motion.SPINNER_DELAY_MS)
        showSpinner = true
    }
    val shape = RoundedCornerShape(4.du)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.du)
            .height(PLACEHOLDER_HEIGHT.du)
            .background(Palette.CardSurface, shape)
            .border(1.du, Palette.CardBorder, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (showSpinner) LoadingSpinner(color = Palette.BodyText)
    }
}
