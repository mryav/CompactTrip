package com.mryav.compacttrip.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

enum class Timing { Now, LeaveAt, ArriveBy }

/** Trip planner `routeType`. */
@Serializable
enum class RouteFor(val label: String, val apiValue: String) {
    EarliestArrival("Earliest arrival", "leasttime"),
    FewerChanges("Fewer changes", "leastinterchange"),
    LeastWalking("Least walking", "leastwalking"),
}

/** The modes a user can allow or avoid (Figma 27:3020), with the product classes each covers. */
@Serializable
enum class TravelMode(
    val label: String,
    /** In a sentence: "Bus & light rail only." */
    val singular: String,
    /** After "Accessible": "Accessible buses & ferries only." */
    val plural: String,
    val icon: Mode,
    val productClasses: List<Int>,
) {
    Bus("Bus", "bus", "buses", Mode.Bus, listOf(5, 7, 11)), // coaches and school buses count as buses
    LightRail("Light Rail", "light rail", "light rail", Mode.LightRail, listOf(4)),
    Train("Train", "train", "trains", Mode.Train, listOf(1)),
    Metro("Metro", "metro", "metro", Mode.Metro, listOf(2)),
    Ferry("Ferry", "ferry", "ferries", Mode.Ferry, listOf(9)),
}

/**
 * Trip options (Figma 27:3318). Defaults: now, all modes, earliest arrival. Saved trips store these
 * too, but never the timing: a trip on Home always shows what's leaving now.
 */
@Immutable
@Serializable
data class TripPreferences(
    @Transient val timing: Timing = Timing.Now,
    /** Sydney local time for [Timing.LeaveAt] / [Timing.ArriveBy]. */
    @Transient val at: LocalDateTime? = null,
    val accessibleOnly: Boolean = false,
    val modes: Set<TravelMode> = TravelMode.entries.toSet(),
    val routeFor: RouteFor = RouteFor.EarliestArrival,
) {
    val excludedModes: List<TravelMode> get() = TravelMode.entries.filterNot { it in modes }

    /**
     * The trip button's label in plain words, or null when everything is default ("Trip preferences").
     * Up to three short sentences: when, what, how. e.g. "Leaving at 8am. Bus & train only. Least walking."
     */
    fun summary(today: LocalDate): String? {
        val sentences = listOfNotNull(timingSentence(today), modesSentence(), routeSentence())
        return sentences.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private fun timingSentence(today: LocalDate): String? {
        val time = at ?: return null
        val day = when (time.toLocalDate()) {
            today -> ""
            today.plusDays(1) -> "tomorrow "
            else -> time.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " "
        }
        return when (timing) {
            Timing.Now -> null
            Timing.LeaveAt -> "Leaving ${day}at ${shortTime(time)}."
            Timing.ArriveBy -> "Arriving ${day}by ${shortTime(time)}."
        }
    }

    /**
     * Two allowed modes or fewer: "Bus & light rail only." More: name what's avoided instead,
     * "Avoid metro & ferry." Accessibility folds in: "Accessible trains only.", "Accessible services only."
     */
    private fun modesSentence(): String? {
        val allowed = TravelMode.entries.filter { it in modes }
        val excluded = excludedModes
        return when {
            excluded.isEmpty() -> if (accessibleOnly) "Accessible services only." else null
            allowed.size <= 2 ->
                if (accessibleOnly) "Accessible ${list(allowed.map { it.plural })} only."
                else "${list(allowed.map { it.singular }).capitalised()} only."
            accessibleOnly -> "Accessible services only. Avoid ${list(excluded.map { it.singular })}."
            else -> "Avoid ${list(excluded.map { it.singular })}."
        }
    }

    private fun routeSentence(): String? =
        if (routeFor == RouteFor.EarliestArrival) null else "${routeFor.label}."

    companion object {
        val Default = TripPreferences()

        /** "8am", "4:30pm". */
        fun shortTime(time: LocalDateTime): String {
            val hour12 = (time.hour % 12).let { if (it == 0) 12 else it }
            val suffix = if (time.hour < 12) "am" else "pm"
            return if (time.minute == 0) "$hour12$suffix" else "$hour12:${"%02d".format(time.minute)}$suffix"
        }

        private fun list(items: List<String>): String = when (items.size) {
            0 -> ""
            1 -> items[0]
            else -> items.dropLast(1).joinToString(", ") + " & " + items.last()
        }

        private fun String.capitalised() = replaceFirstChar { it.uppercase() }
    }
}
