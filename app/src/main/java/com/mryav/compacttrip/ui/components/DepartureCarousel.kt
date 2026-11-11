package com.mryav.compacttrip.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.lerp
import com.mryav.compacttrip.data.model.DepartureCard
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du
import kotlinx.coroutines.delay

/** Figma 24:1563: the loading placeholder is shorter than a loaded card. */
private const val PLACEHOLDER_HEIGHT = 155

/**
 * Horizontal Quick View cards. D-pad: LEFT/RIGHT between cards (the last card stops), DOWN leaves to
 * the next section, UP goes to [upTarget] (the notice chip or the trip section) when given.
 *
 * Entry animation (Figma 24:1843): [morph] 0→1 shrinks the first card from the full-width loading
 * placeholder to its real width while the others slide in behind it, and grows the cards from the
 * placeholder height to full height. [reveal] 0→1 (linear, over [revealDurationMs]) then uncovers
 * each card's content top-to-bottom, staggered left to right. Both are read in layout/draw only.
 *
 * Updates (each refresh): a departed card collapses horizontally towards its left edge, cropping its
 * content, while the cards behind it slide into its place. New cards arrive with their content
 * hidden and then revealed top-to-bottom; if the user was at the end of the carousel, it first
 * slides along so the new card comes into view. If the departing card had focus, focus moves to
 * the card that takes its place.
 *
 * [onCardClick] = null: cards aren't clickable, only focusable (shadow-only focus). [compact] uses
 * the shorter one-line card ([COMPACT_CARD_HEIGHT], same height as the placeholder).
 */
@OptIn(ExperimentalFoundationApi::class)
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // What's on screen: the current cards plus any departed ones still collapsing.
    val items = remember { mutableStateListOf<CarouselItem>().apply { addAll(cards.map { CarouselItem(it, entering = false) }) } }

    LaunchedEffect(cards) {
        val shownKeys = items.map { it.key }
        if (shownKeys == cards.map { it.key }) {
            items.forEachIndexed { i, item -> item.card = cards[i] } // same cards, fresher times/status
            return@LaunchedEffect
        }
        val wasAtEnd = listState.canScrollBackward && !listState.canScrollForward
        val update = items.merge(cards)
        items.clear()
        items.addAll(update.items)

        // Focus first (the departing card can't keep it), then animate in the app-wide scope so the
        // next refresh can't cut an animation short.
        update.departing.firstOrNull { it.focused }?.let { gone ->
            val i = update.items.indexOf(gone)
            val heir = update.items.drop(i + 1).firstOrNull { !it.exiting && !it.entering }
                ?: update.items.take(i).lastOrNull { !it.exiting && !it.entering }
            heir?.let { runCatching { it.focus.requestFocus() } }
        }
        update.departing.forEach { item ->
            item.exitJob = scope.launch {
                item.presence.animateTo(0f, tween(Motion.EXPAND_MS, easing = Motion.Expand))
                items.remove(item)
            }
        }
        update.restored.forEach { item ->
            item.exitJob?.cancel()
            item.exitJob = null
            scope.launch { item.presence.animateTo(1f, tween(Motion.EXPAND_MS, easing = Motion.Expand)) }
        }
        if (update.entering.isNotEmpty()) {
            scope.launch {
                if (wasAtEnd) slideIntoView(listState, update.entering.first().key)
                update.entering.forEachIndexed { i, item ->
                    launch {
                        delay(i * Motion.STAGGER_MS.toLong())
                        item.reveal.animateTo(1f, tween(Motion.REVEAL_MS, easing = Motion.Expand))
                        item.entering = false
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val sidePadding = 16.du
        val gap = 8.du
        val fullWidthPx = with(density) { (maxWidth - sidePadding * 2).roundToPx() }
        val fromHeightPx = with(density) { PLACEHOLDER_HEIGHT.du.roundToPx() }
        // Cards in a carousel share one height. The card height fits a one-line headsign; if any
        // headsign here wraps to a second line, every card grows by that line so none is cropped.
        val headsignStyle = Type.headsign(Palette.BodyText)
        val headsignWidthPx = with(density) { HEADSIGN_MAX_WIDTH.du.roundToPx() }
        val measurer = rememberTextMeasurer(cacheSize = 8)
        val headsignLines = remember(cards, compact, headsignStyle, headsignWidthPx) {
            if (compact) return@remember 1
            val oneLine = measurer.measure("A", headsignStyle).size.height
            val wraps = cards.any { card ->
                measurer.measure(
                    card.headsign,
                    headsignStyle,
                    maxLines = 2,
                    constraints = Constraints(maxWidth = headsignWidthPx),
                ).size.height > oneLine
            }
            if (wraps) 2 else 1
        }
        val toHeightPx = with(density) { quickViewCardHeight(compact, headsignLines).roundToPx() }
        val totalReveal = revealDurationMs(cards.size).toFloat()

        // D-pad focus: the focused card lines up with the carousel's left margin (the previous card
        // peeking at the edge), not flush against the screen edge. Near the end the scroll is
        // clamped, so the last cards stay where they are, towards the right.
        val leadingPx = with(density) { sidePadding.toPx() }
        val focusAlignment = remember(leadingPx) { AlignToLeadingEdge(leadingPx) }

        // The gap between cards is part of each item (end padding), so a collapsing card takes its
        // gap with it instead of leaving a hole.
        CompositionLocalProvider(LocalBringIntoViewSpec provides focusAlignment) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(start = sidePadding, end = sidePadding - gap),
            ) {
                itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                    QuickViewCard(
                        card = item.card,
                        now = now,
                        onClick = onCardClick?.let { click -> { click(item.card) } },
                        compact = compact,
                        contentReveal = {
                            val elapsed = reveal() * totalReveal - index * Motion.STAGGER_MS
                            minOf(Motion.Expand.transform((elapsed / Motion.REVEAL_MS).coerceIn(0f, 1f)), item.reveal.value)
                        },
                        modifier = Modifier
                            .collapseToStart { item.presence.value }
                            .padding(end = gap)
                            .morph(morph, if (index == 0) fullWidthPx else null, fromHeightPx, toHeightPx)
                            .focusRequester(item.focus)
                            .onFocusChanged { item.focused = it.isFocused }
                            .focusProperties {
                                canFocus = !item.exiting
                                if (upTarget != null) up = upTarget
                            },
                    )
                }
            }
        }
    }
}

/** Brings the requested item to [leadingPx] from the start of the carousel. */
@OptIn(ExperimentalFoundationApi::class)
private class AlignToLeadingEdge(private val leadingPx: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = offset - leadingPx
}

/** One card in the carousel, with its own enter/exit animation state. */
@Stable
private class CarouselItem(card: DepartureCard, entering: Boolean) {
    val key: String = card.key
    var card by mutableStateOf(card)
    /** 1 = full width; animates to 0 as a departed card collapses. */
    val presence = Animatable(1f)
    /** Content reveal for a newly arrived card (0 → 1, top to bottom). */
    val reveal = Animatable(if (entering) 0f else 1f)
    var exiting by mutableStateOf(false)
    var entering = entering
    var exitJob: Job? = null
    val focus = FocusRequester()
    var focused = false
}

private class CarouselUpdate(
    val items: List<CarouselItem>,
    val departing: List<CarouselItem>,
    val entering: List<CarouselItem>,
    /** Came back while still collapsing. */
    val restored: List<CarouselItem>,
)

/**
 * The new on-screen list: [cards] in order, with departed items kept (marked exiting) where they
 * were, so they can collapse in place. A card that comes back while still collapsing is restored.
 */
private fun List<CarouselItem>.merge(cards: List<DepartureCard>): CarouselUpdate {
    val byKey = associateBy { it.key }
    val newKeys = cards.mapTo(HashSet()) { it.key }
    val entering = mutableListOf<CarouselItem>()
    val restored = mutableListOf<CarouselItem>()
    val merged = cards.mapTo(ArrayList()) { card ->
        byKey[card.key]?.also {
            it.card = card
            if (it.exiting) restored += it
            it.exiting = false
        } ?: CarouselItem(card, entering = true).also { entering += it }
    }
    val departing = mutableListOf<CarouselItem>()
    forEachIndexed { i, item ->
        if (item.key in newKeys) return@forEachIndexed
        if (!item.exiting) departing += item
        item.exiting = true
        val anchor = subList(0, i).lastOrNull { it in merged }
        merged.add(if (anchor == null) 0 else merged.indexOf(anchor) + 1, item)
    }
    return CarouselUpdate(merged, departing, entering, restored)
}

/**
 * The user was at the end of the carousel and a card arrived after it: slide along (the first-load
 * easing) until it's fully in view. It sits just past the end, in the trailing padding, so it's
 * already laid out after the next frame.
 */
private suspend fun slideIntoView(listState: LazyListState, key: String) {
    withFrameNanos { }
    withFrameNanos { }
    val info = listState.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return
    val distance = item.offset + item.size - (info.viewportEndOffset - info.afterContentPadding)
    if (distance > 0) listState.animateScrollBy(distance.toFloat(), tween(Motion.EXPAND_MS, easing = Motion.Expand))
}

/** Layout-only horizontal collapse: width × [fraction], content anchored left and cropped. */
private fun Modifier.collapseToStart(fraction: () -> Float): Modifier = clipToBounds().layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val f = fraction().coerceIn(0f, 1f)
    layout((placeable.width * f).roundToInt(), placeable.height) { placeable.place(0, 0) }
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
