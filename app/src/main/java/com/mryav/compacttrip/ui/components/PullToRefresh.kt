package com.mryav.compacttrip.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Resting height of the "Updated …" tag. */
private const val TAG_DU = 40
/** Pulled this far, the tag says "Keep pulling to refresh…". */
private const val KEEP_PULLING_DU = 56
/** Released at or beyond this, a refresh is queued. */
private const val THRESHOLD_DU = 120
private const val MAX_PULL_DU = 160
/** Past the tag's own height the pull gets heavier, like the platform's pull-to-refresh. */
private const val RESISTANCE = 0.5f
/** How long "Updated …" stays before the tag collapses. */
private const val TAG_SHOWN_MS = 7_000L

enum class TagMessage { Refreshing, Updated }

enum class PullStage { Message, KeepPulling, Release }

/**
 * Pull-to-refresh for a scrolling feed, plus the collapsible "Updated …" tag above it.
 *
 * The tag is normally collapsed. Pulling down at the top of the feed opens it with the finger:
 * a slight pull shows "Updated 30s ago", further shows "Keep pulling to refresh…", and past the
 * threshold "Release to refresh". Releasing short of that bounces back to the tag, which then
 * collapses after 7 s. Refreshes (pulled, tapped or automatic) expand the tag with "Refreshing…";
 * it stays open while the text changes to "Updated just now", then collapses after 7 s.
 *
 * Attach [connection] with `Modifier.nestedScroll` above the feed's `verticalScroll`.
 */
@Stable
class PullRefreshState internal constructor(
    private val scope: CoroutineScope,
    private val tagPx: Float,
    private val keepPullingPx: Float,
    private val thresholdPx: Float,
    private val maxPx: Float,
) {
    /** Current height of the tag container in px; read it in layout only. */
    var height by mutableFloatStateOf(0f)
        private set
    var message by mutableStateOf(TagMessage.Updated)
        private set
    private var dragging by mutableStateOf(false)

    val stage: PullStage by derivedStateOf {
        when {
            !dragging -> PullStage.Message
            height >= thresholdPx -> PullStage.Release
            height > keepPullingPx -> PullStage.KeepPulling
            else -> PullStage.Message
        }
    }

    internal var refreshing = false
    internal var onRefresh: () -> Unit = {}
    private var job: Job? = null
    private var lastDelta = 0f

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        // Finger moving up while the tag is open: close the tag before the feed scrolls.
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y >= 0f || height <= 0f) return Offset.Zero
            return Offset(0f, drag(available.y))
        }

        // Finger moving down with the feed already at the top: pull the tag open.
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
            return Offset(0f, drag(available.y))
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!dragging) return Velocity.Zero
            val open = height > 0f
            release()
            return if (open) available else Velocity.Zero
        }
    }

    /** Returns the delta consumed. */
    private fun drag(delta: Float): Float {
        if (!dragging) {
            dragging = true
            job?.cancel()
        }
        val factor = if (height >= tagPx) RESISTANCE else 1f
        val next = (height + delta * factor).coerceIn(0f, maxPx)
        val consumed = (next - height) / factor
        height = next
        lastDelta = delta
        return if (delta > 0f) delta else consumed
    }

    private fun release() {
        dragging = false
        val pulled = height
        job = scope.launch {
            when {
                pulled <= 0f -> Unit
                pulled >= thresholdPx -> {
                    message = TagMessage.Refreshing
                    onRefresh()
                    settle()
                }
                // Pushed back up: the user is closing it.
                lastDelta < 0f && pulled < tagPx -> collapse()
                else -> {
                    message = if (refreshing) TagMessage.Refreshing else TagMessage.Updated
                    settle()
                    hideLater()
                }
            }
        }
    }

    /** Show [msg]: expand the tag if it's closed; if it's already open, swap the text in place. */
    internal fun show(msg: TagMessage) {
        if (dragging) return // the release decides what to show
        job?.cancel()
        job = scope.launch {
            message = msg
            if (height > 0f) settle() else animateHeight(tagPx, tween(Motion.EXPAND_MS, easing = Motion.Expand))
            hideLater()
        }
    }

    private suspend fun hideLater() {
        if (message != TagMessage.Updated) return
        delay(TAG_SHOWN_MS)
        collapse()
    }

    private suspend fun settle() =
        animateHeight(tagPx, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))

    private suspend fun collapse() = animateHeight(0f, tween(Motion.COLLAPSE_MS, easing = Motion.Collapse))

    private suspend fun animateHeight(target: Float, spec: AnimationSpec<Float>) {
        animate(height, target, animationSpec = spec) { value, _ -> height = value }
    }
}

/** [refreshing] drives the tag: it comes back with "Refreshing…", then "Updated just now". */
@Composable
fun rememberPullRefreshState(refreshing: Boolean, onRefresh: () -> Unit): PullRefreshState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val tag = TAG_DU.du
    val keepPulling = KEEP_PULLING_DU.du
    val threshold = THRESHOLD_DU.du
    val max = MAX_PULL_DU.du
    val state = remember(scope, density, tag) {
        with(density) { PullRefreshState(scope, tag.toPx(), keepPulling.toPx(), threshold.toPx(), max.toPx()) }
    }
    SideEffect {
        state.refreshing = refreshing
        state.onRefresh = onRefresh
    }
    // Skip the first composition unless a refresh is already running: entering a screen shouldn't
    // flash "Updated …".
    val initial = remember { refreshing }
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) {
        if (!started) {
            started = true
            if (!initial) return@LaunchedEffect
        }
        state.show(if (refreshing) TagMessage.Refreshing else TagMessage.Updated)
    }
    return state
}

/**
 * The collapsible "Updated …" tag (right-aligned label). Its height follows [state]; the
 * content is anchored to the top and cropped, so it's revealed top-to-bottom.
 */
@Composable
fun RefreshTag(state: PullRefreshState, updatedAt: Long?, now: State<Long>, modifier: Modifier = Modifier) {
    val stage = state.stage
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(stage) {
        if (stage == PullStage.Release) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                layout(placeable.width, state.height.roundToInt()) { placeable.place(0, 0) }
            }
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TAG_DU.du)
                .padding(horizontal = 8.du),
            contentAlignment = Alignment.CenterEnd,
        ) {
            // The app ticker is coarse (15 s); read the clock afresh whenever the tag's text changes.
            val tick = now.value
            val clock = remember(stage, state.message, updatedAt, tick) { System.currentTimeMillis() }
            val text = when (stage) {
                PullStage.Release -> "Release to refresh"
                PullStage.KeepPulling -> "Keep pulling to refresh…"
                PullStage.Message -> when {
                    state.message == TagMessage.Refreshing -> "Refreshing…"
                    updatedAt == null -> "Not updated yet"
                    else -> "Updated ${updatedAgo(updatedAt, clock)}"
                }
            }
            BasicText(text, style = Type.label(Palette.BodyText), maxLines = 1)
        }
    }
}
