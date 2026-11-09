package com.mryav.compacttrip.widget

import com.mryav.compacttrip.data.LoadError
import com.mryav.compacttrip.data.TfnswJson
import com.mryav.compacttrip.data.dto.TripResponse
import com.mryav.compacttrip.data.model.Mode
import com.mryav.compacttrip.data.model.RouteFor
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.data.model.Timing
import com.mryav.compacttrip.data.model.TravelMode
import com.mryav.compacttrip.data.model.TripPreferences
import com.mryav.compacttrip.data.toSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class WidgetModelsTest {

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("fixtures/$name")) { "missing fixture $name" }.readText()

    private val minute = 60_000L
    private val t0 = 1_800_000_000_000L

    private fun row(key: String, departsAt: Long) = WidgetRow(
        key = key,
        badges = listOf(WidgetBadge("T1", Mode.Train)),
        headsign = "Lidcombe",
        place = "Platform 2",
        departsAt = departsAt,
        arrivesAt = departsAt + 30 * minute,
        cancelled = false,
    )

    private fun loaded(vararg rows: WidgetRow) = WidgetSnapshot(fetchedAt = t0, rows = rows.toList())

    @Test
    fun snapshot_fromLiveTrip_keepsRowsWithTimes() {
        val summary = TfnswJson.decodeFromString(TripResponse.serializer(), fixture("trip_central_olympicpark.json")).toSummary()
        val snapshot = summary.toSnapshot(t0)

        assertEquals(t0, snapshot.fetchedAt)
        assertEquals(summary.journeys.size.coerceAtMost(MAX_ROWS), snapshot.rows.size)
        val first = snapshot.rows.first()
        assertEquals(summary.journeys.first().card.headsign, first.headsign)
        assertEquals(summary.journeys.first().arrivesAt, first.arrivesAt)
        assertTrue(first.badges.isNotEmpty())
        assertFalse(snapshot.noServices)
    }

    @Test
    fun departedServices_dropOffAMinuteAfterTheyreDue() {
        val s = loaded(row("a", t0 + 2 * minute), row("b", t0 + 10 * minute))

        assertEquals(listOf("a", "b"), (s.content(t0 + 2 * minute) as WidgetContent.Rows).rows.map { it.key })
        assertEquals(listOf("a", "b"), (s.content(t0 + 2 * minute + 59_000) as WidgetContent.Rows).rows.map { it.key })
        assertEquals(listOf("b"), (s.content(t0 + 3 * minute) as WidgetContent.Rows).rows.map { it.key })
    }

    @Test
    fun allDepartedOrAnHourOld_asksForATap() {
        val s = loaded(row("a", t0 + 5 * minute), row("b", t0 + 120 * minute))

        assertEquals(WidgetContent.Message(WidgetMessage.TapToLoad), s.content(t0 + 60 * minute))
        assertEquals(WidgetContent.Message(WidgetMessage.TapToLoad), loaded(row("a", t0)).content(t0 + 5 * minute))
        assertEquals(WidgetContent.Message(WidgetMessage.TapToLoad), null.content(t0))
    }

    @Test
    fun errors_keepRows_untilThereAreNone() {
        val s = loaded(row("a", t0 + 5 * minute)).copy(error = LoadError.Offline)

        assertTrue(s.content(t0) is WidgetContent.Rows)
        assertEquals(WidgetContent.Message(WidgetMessage.Offline), s.content(t0 + 10 * minute))
        assertEquals(WidgetContent.Message(WidgetMessage.Offline), null.withError(LoadError.Offline).content(t0))
        assertEquals(WidgetContent.Message(WidgetMessage.KeyProblem), s.copy(error = LoadError.KeyProblem).content(t0))
    }

    @Test
    fun walkingAndNoServices_haveTheirOwnMessages() {
        val walk = WidgetSnapshot(fetchedAt = t0, walkMinutes = 7)
        val none = WidgetSnapshot(fetchedAt = t0, noServices = true)

        assertEquals(WidgetContent.Message(WidgetMessage.Walking, "It's a 7 min walk"), walk.content(t0))
        assertEquals(WidgetContent.Message(WidgetMessage.NoServices), none.content(t0))
        assertEquals(WidgetContent.Message(WidgetMessage.TapToLoad), none.content(t0 + 61 * minute))
    }

    @Test
    fun updatedAgo_wholeMinutes() {
        assertEquals("Updated just now", widgetUpdatedAgo(t0, t0 + 59_000))
        assertEquals("Updated 1 minute ago", widgetUpdatedAgo(t0, t0 + minute))
        assertEquals("Updated 10 minutes ago", widgetUpdatedAgo(t0, t0 + 10 * minute + 30_000))
    }

    @Test
    fun config_roundTrips_andMatchesItsRouteWhateverTheTiming() {
        val central = Stop("200060", "Central Station", "Central", "Sydney", listOf(Mode.Train))
        val olympic = Stop("214110", "Olympic Park Station", "Olympic Park", "Sydney Olympic Park", listOf(Mode.Train))
        val prefs = TripPreferences(modes = setOf(TravelMode.Train), routeFor = RouteFor.FewerChanges)
        val config = WidgetConfig(central, olympic, prefs, WidgetStyle.FixedCard)

        val json = TfnswJson.encodeToString(WidgetConfig.serializer(), config)
        assertEquals(config, TfnswJson.decodeFromString(WidgetConfig.serializer(), json))

        assertTrue(config.matches(central, olympic, prefs))
        assertTrue(config.matches(central, olympic, prefs.copy(timing = Timing.LeaveAt, at = LocalDateTime.now())))
        assertFalse(config.matches(olympic, central, prefs))
        assertFalse(config.matches(central, olympic, TripPreferences.Default))
    }
}
