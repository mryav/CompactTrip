package com.mryav.compacttrip.widget

import com.mryav.compacttrip.data.LoadError
import com.mryav.compacttrip.data.model.Mode
import com.mryav.compacttrip.data.model.RealtimeStatus
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.data.model.TripPreferences
import com.mryav.compacttrip.data.model.TripSummary
import kotlinx.serialization.Serializable

/**
 * Widget styles (Figma 30:5207). Each is its own widget in the picker (labels and sizes in
 * res/xml/widget_*_info.xml), so the style is set by which one was placed.
 */
@Serializable
enum class WidgetStyle { DefaultList, Headless, FixedCard }

/** What a widget shows: a route (never timed: a widget always shows what's leaving now) in a style. */
@Serializable
data class WidgetConfig(
    val origin: Stop,
    val destination: Stop,
    val preferences: TripPreferences = TripPreferences.Default,
    val style: WidgetStyle = WidgetStyle.DefaultList,
) {
    /** Same route as a trip the app just loaded (preferences compare without timing). */
    fun matches(origin: Stop, destination: Stop, preferences: TripPreferences): Boolean =
        this.origin.id == origin.id && this.destination.id == destination.id &&
            this.preferences == preferences.withoutTiming()
}

fun TripPreferences.withoutTiming(): TripPreferences = TripPreferences(
    accessibleOnly = accessibleOnly,
    modes = modes,
    routeFor = routeFor,
)

@Serializable
data class WidgetBadge(val label: String, val mode: Mode)

/** One service, as stored for the widget (only what it draws). */
@Serializable
data class WidgetRow(
    val key: String,
    val badges: List<WidgetBadge>,
    val headsign: String,
    /** "Platform 2", "Park St". */
    val place: String?,
    val departsAt: Long,
    val arrivesAt: Long?,
    val cancelled: Boolean,
)

/** The last load for one widget. Rows are kept through errors so the widget never blanks. */
@Serializable
data class WidgetSnapshot(
    /** When data last arrived; null = never loaded. */
    val fetchedAt: Long? = null,
    val rows: List<WidgetRow> = emptyList(),
    val error: LoadError? = null,
    /** No service, but it's walkable. */
    val walkMinutes: Int? = null,
    /** Loaded fine, nothing found. */
    val noServices: Boolean = false,
)

/** Most rows a widget keeps (the planner returns about 6). */
const val MAX_ROWS = 8

/** Older than this (or every service departed), the widget asks for a tap instead of showing rows. */
const val STALE_AFTER_MS = 60 * 60_000L

/** A service stays on the widget ("Now") for a minute after it's due. */
const val DEPARTED_GRACE_MS = 60_000L

fun TripSummary.toSnapshot(now: Long): WidgetSnapshot {
    val rows = journeys.take(MAX_ROWS).map { j ->
        val card = j.card
        WidgetRow(
            key = card.key,
            badges = card.badges.map { WidgetBadge(it.label, it.mode) },
            headsign = card.headsign,
            place = card.platform,
            departsAt = card.departsAt,
            arrivesAt = j.arrivesAt,
            cancelled = card.status == RealtimeStatus.Cancelled,
        )
    }
    return WidgetSnapshot(
        fetchedAt = now,
        rows = rows,
        walkMinutes = if (rows.isEmpty()) walkMinutes else null,
        noServices = rows.isEmpty() && walkMinutes == null,
    )
}

/** A failed load keeps the previous rows (and when they arrived) and records why. */
fun WidgetSnapshot?.withError(error: LoadError): WidgetSnapshot = (this ?: WidgetSnapshot()).copy(error = error)

/** What the tappable message area says, and what tapping it does. */
enum class WidgetMessage(val text: String, val opensApp: Boolean) {
    TapToLoad("Tap here to load updates", false),
    Offline("No connection. Tap to retry", false),
    KeyProblem("Check your API key in CompactTrips", true),
    NoServices("No services found", true),
    Walking("", true),
}

/** What a widget shows right now: rows, or a message. */
sealed interface WidgetContent {
    data class Rows(val rows: List<WidgetRow>, val fetchedAt: Long) : WidgetContent
    data class Message(val message: WidgetMessage, val text: String = message.text) : WidgetContent
}

/** Departed services drop off locally (no network), a minute after they're due. */
fun WidgetSnapshot.visibleRows(now: Long): List<WidgetRow> = rows.filter { it.departsAt + DEPARTED_GRACE_MS > now }

fun WidgetSnapshot?.content(now: Long): WidgetContent {
    val s = this ?: return WidgetContent.Message(WidgetMessage.TapToLoad)
    if (s.error == LoadError.KeyProblem) return WidgetContent.Message(WidgetMessage.KeyProblem)
    val fetchedAt = s.fetchedAt
    val rows = s.visibleRows(now)
    if (fetchedAt != null && now - fetchedAt < STALE_AFTER_MS) {
        if (rows.isNotEmpty()) return WidgetContent.Rows(rows, fetchedAt)
        if (s.error == null) {
            s.walkMinutes?.let { return WidgetContent.Message(WidgetMessage.Walking, "It's a $it min walk") }
            if (s.noServices) return WidgetContent.Message(WidgetMessage.NoServices)
        }
    }
    return WidgetContent.Message(if (s.error == LoadError.Offline) WidgetMessage.Offline else WidgetMessage.TapToLoad)
}

/** Footer text. Widgets redraw once a minute, so whole minutes: "Updated 10 minutes ago". */
fun widgetUpdatedAgo(fetchedAt: Long, now: Long): String {
    val minutes = ((now - fetchedAt) / 60_000).coerceAtLeast(0)
    return when (minutes) {
        0L -> "Updated just now"
        1L -> "Updated 1 minute ago"
        else -> "Updated $minutes minutes ago"
    }
}
