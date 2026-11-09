package com.mryav.compacttrip.data

import com.mryav.compacttrip.data.model.RouteFor
import com.mryav.compacttrip.data.model.Timing
import com.mryav.compacttrip.data.model.TravelMode
import com.mryav.compacttrip.data.model.TravelMode.Bus
import com.mryav.compacttrip.data.model.TravelMode.Ferry
import com.mryav.compacttrip.data.model.TravelMode.LightRail
import com.mryav.compacttrip.data.model.TravelMode.Metro
import com.mryav.compacttrip.data.model.TravelMode.Train
import com.mryav.compacttrip.data.model.TripPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** The Figma 27:3318 label examples, in the app's wording. */
class TripPreferencesTest {
    private val today = LocalDate.of(2026, 11, 9) // a Monday
    private fun at(h: Int, m: Int = 0, day: LocalDate = today) = LocalDateTime.of(day, java.time.LocalTime.of(h, m))
    private fun prefs(
        timing: Timing = Timing.Now,
        at: LocalDateTime? = null,
        accessible: Boolean = false,
        modes: Set<TravelMode> = TravelMode.entries.toSet(),
        routeFor: RouteFor = RouteFor.EarliestArrival,
    ) = TripPreferences(timing, at, accessible, modes, routeFor).summary(today)

    @Test
    fun defaults_haveNoSummary() {
        assertNull(prefs())
    }

    @Test
    fun figmaExamples() {
        assertEquals("Metro & ferry only.", prefs(modes = setOf(Metro, Ferry)))
        assertEquals("Leaving at 4:30pm. Bus & light rail only.", prefs(Timing.LeaveAt, at(16, 30), modes = setOf(Bus, LightRail)))
        assertEquals("Arriving by 8am. Accessible buses only.", prefs(Timing.ArriveBy, at(8), accessible = true, modes = setOf(Bus)))
        assertEquals("Arriving by 8am. Accessible buses & ferries only.", prefs(Timing.ArriveBy, at(8), accessible = true, modes = setOf(Bus, Ferry)))
        assertEquals("Leaving at 8am. Accessible trains only.", prefs(Timing.LeaveAt, at(8), accessible = true, modes = setOf(Train)))
        assertEquals(
            "Leaving at 8am. Accessible services only. Avoid metro & ferry.",
            prefs(Timing.LeaveAt, at(8), accessible = true, modes = setOf(Bus, LightRail, Train)),
        )
        assertEquals(
            "Leaving at 8am. Bus & train only. Least walking.",
            prefs(Timing.LeaveAt, at(8), modes = setOf(Bus, Train), routeFor = RouteFor.LeastWalking),
        )
    }

    @Test
    fun days_andSingleFacts() {
        assertEquals("Leaving tomorrow at 7:05am.", prefs(Timing.LeaveAt, at(7, 5, today.plusDays(1))))
        assertEquals("Arriving Friday by 12pm.", prefs(Timing.ArriveBy, at(12, 0, today.plusDays(4))))
        assertEquals("Accessible services only.", prefs(accessible = true))
        assertEquals("Avoid ferry.", prefs(modes = TravelMode.entries.toSet() - Ferry))
        assertEquals("Fewer changes.", prefs(routeFor = RouteFor.FewerChanges))
        assertEquals("Leaving at 12am.", prefs(Timing.LeaveAt, at(0)))
    }
}

class TripParamsTest {
    private val a = com.mryav.compacttrip.data.model.Stop("1", "A", "A", null, emptyList())
    private val b = com.mryav.compacttrip.data.model.Stop("2", "B", "B", null, emptyList())
    private val now = LocalDateTime.of(2026, 11, 9, 13, 2)

    @Test
    fun defaults_leaveNow_noFilters() {
        val p = TripRepository.tripParams(a, b, TripPreferences.Default, now).toMap()
        assertEquals("dep", p["depArrMacro"])
        assertEquals("20261109", p["itdDate"])
        assertEquals("1302", p["itdTime"])
        assertNull(p["routeType"])
        assertNull(p["wheelchair"])
        assertNull(p["excludedMeans"])
    }

    @Test
    fun preferences_mapToTripPlannerParams() {
        val prefs = TripPreferences(
            timing = Timing.ArriveBy,
            at = LocalDateTime.of(2026, 11, 10, 8, 0),
            accessibleOnly = true,
            modes = setOf(Train, Metro, LightRail),
            routeFor = RouteFor.FewerChanges,
        )
        val list = TripRepository.tripParams(a, b, prefs, now)
        val p = list.toMap()
        assertEquals("arr", p["depArrMacro"])
        assertEquals("20261110", p["itdDate"])
        assertEquals("0800", p["itdTime"])
        assertEquals("leastinterchange", p["routeType"])
        assertEquals("on", p["wheelchair"])
        assertEquals("checkbox", p["excludedMeans"])
        // Bus covers buses, coaches and school buses; ferry is 9.
        assertEquals(listOf("exclMOT_5", "exclMOT_7", "exclMOT_9", "exclMOT_11"), list.map { it.first }.filter { it.startsWith("exclMOT_") })
    }
}

class SavedTripPreferencesTest {
    private val serializer = com.mryav.compacttrip.data.model.SavedTrip.serializer()

    @Test
    fun oldSavedTrips_withoutPreferences_loadWithDefaults() {
        val old = """{"id":"x","origin":{"id":"1","name":"A","shortName":"A","locality":null,"modes":[]},""" +
            """"destination":{"id":"2","name":"B","shortName":"B","locality":null,"modes":[]}}"""
        assertEquals(TripPreferences.Default, TfnswJson.decodeFromString(serializer, old).preferences)
    }

    @Test
    fun preferences_roundTrip_withoutTiming() {
        val a = com.mryav.compacttrip.data.model.Stop("1", "A", "A", null, emptyList())
        val prefs = TripPreferences(
            timing = Timing.LeaveAt,
            at = LocalDateTime.of(2026, 11, 9, 8, 0),
            accessibleOnly = true,
            modes = setOf(Bus, Ferry),
            routeFor = RouteFor.LeastWalking,
        )
        val trip = com.mryav.compacttrip.data.model.SavedTrip("x", a, a, prefs)
        val json = TfnswJson.encodeToString(serializer, trip)
        val back = TfnswJson.decodeFromString(serializer, json).preferences
        assertEquals(prefs.copy(timing = Timing.Now, at = null), back)
        assertEquals(false, json.contains("timing") || json.contains("\"at\""))
    }
}
