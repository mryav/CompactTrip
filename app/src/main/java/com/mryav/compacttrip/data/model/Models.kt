package com.mryav.compacttrip.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/** TfNSW transport mode, from the API's product class. */
enum class Mode(val productClass: Int) {
    Train(1), Metro(2), LightRail(4), Bus(5), Coach(7), Ferry(9), SchoolBus(11), Walk(100);

    val isWalk: Boolean get() = this == Walk

    companion object {
        fun fromClass(productClass: Int): Mode? = when (productClass) {
            99, 100 -> Walk
            else -> entries.firstOrNull { it.productClass == productClass }
        }
    }
}

/** A stop/station/wharf the user can pick. */
@Immutable
@Serializable
data class Stop(
    val id: String,
    /** e.g. "Central Station". */
    val name: String,
    /** Short form used in trip titles, e.g. "Central". */
    val shortName: String,
    /** e.g. "Sydney". */
    val locality: String?,
    val modes: List<Mode>,
)

@Immutable
@Serializable
data class SavedTrip(
    val id: String,
    val origin: Stop,
    val destination: Stop,
    /** Modes, accessibility and route type for this trip (no timing). Older saves default to all. */
    val preferences: TripPreferences = TripPreferences.Default,
) {
    val title: String get() = "${origin.shortName} → ${destination.shortName}"
}

enum class RealtimeStatus { OnTime, Delayed, Unknown, Cancelled }

@Immutable
data class LineBadge(val label: String, val mode: Mode)

/** One upcoming journey, as shown on a Quick View card. */
@Immutable
data class DepartureCard(
    val key: String,
    val badges: List<LineBadge>,
    val headsign: String,
    /** "Platform 2", "Now Platform 1", or a bus stand. */
    val platform: String?,
    val platformChanged: Boolean,
    /** "Board cars 5–10" when only part of the train fits the destination platform. */
    val boardCars: String?,
    /** Best-known departure (estimated if live, else planned), epoch millis. */
    val departsAt: Long,
    val status: RealtimeStatus,
    /** Minutes behind the timetable (0 unless [status] is Delayed). */
    val delayMinutes: Int = 0,
)

/** A line affected by a notice, e.g. T4 + "Eastern Suburbs & Illawarra Line (Waterfall or Cronulla to Bondi Junction)". */
@Immutable
data class LineRef(val badge: LineBadge, val name: String?)

/** A service alert attached to a trip's legs. */
@Immutable
data class Notice(
    val id: String,
    val title: String,
    /** high/veryHigh: shown red. */
    val urgent: Boolean,
    val html: String?,
    val url: String?,
    /** Last modified (or created), epoch millis. */
    val updatedAt: Long?,
    val lines: List<LineRef>,
)

sealed interface JourneyLeg {
    /** Best-known start of this leg, epoch millis (always known for a ride). */
    val departsAt: Long?
    /** Best-known end of this leg, epoch millis. */
    val arrivesAt: Long?

    @Immutable
    data class Ride(
        val badge: LineBadge,
        val headsign: String,
        /** "Bondi Junction Station, Platform 2". */
        val from: String,
        val to: String,
        override val departsAt: Long,
        override val arrivesAt: Long?,
        val status: RealtimeStatus,
        /** Minutes behind the timetable (0 unless [status] is Delayed). */
        val delayMinutes: Int,
        /** Stops travelled (excluding the boarding stop). */
        val stops: Int,
        /** The stops passed between [from] and [to], "Edgecliff Station, Platform 2"; empty if the planner gave none. */
        val stopNames: List<String>,
        val minutes: Int,
        val noticeIds: List<String>,
    ) : JourneyLeg

    @Immutable
    data class Walk(
        val minutes: Int,
        override val departsAt: Long? = null,
        override val arrivesAt: Long? = null,
    ) : JourneyLeg
}

/** One way to make the trip: the Quick View card plus everything Trip Details needs. */
@Immutable
data class Journey(
    val card: DepartureCard,
    /** Arrival at the final destination (estimated if live), epoch millis. */
    val arrivesAt: Long?,
    val legs: List<JourneyLeg>,
) {
    val key: String get() = card.key
}

/** Everything Home and View Services need for one saved trip. */
@Immutable
data class TripSummary(
    val journeys: List<Journey>,
    /** Distinct notices across all journeys, most important first. */
    val notices: List<Notice>,
    /** Minutes of the quickest walk-only option, when the planner offers one (stops within walking distance). */
    val walkMinutes: Int? = null,
) {
    val departures: List<DepartureCard> get() = journeys.map { it.card }
    val noticeCount: Int get() = notices.size
    /** Headline of the most important notice. */
    val firstNotice: String? get() = notices.firstOrNull()?.title
    /** True when that notice is high/veryHigh priority: shown as red text instead of "ⓘ N notices". */
    val urgent: Boolean get() = notices.firstOrNull()?.urgent == true
}
