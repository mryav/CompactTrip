package com.mryav.compacttrip.data

import com.mryav.compacttrip.data.dto.JourneyDto
import com.mryav.compacttrip.data.dto.LegDto
import com.mryav.compacttrip.data.dto.LegStopDto
import com.mryav.compacttrip.data.dto.LegStopPropertiesDto
import com.mryav.compacttrip.data.dto.NamedDto
import com.mryav.compacttrip.data.dto.ProductDto
import com.mryav.compacttrip.data.dto.StopFinderResponse
import com.mryav.compacttrip.data.dto.StopRefDto
import com.mryav.compacttrip.data.dto.StopRefPropertiesDto
import com.mryav.compacttrip.data.dto.TransportationDto
import com.mryav.compacttrip.data.dto.TripResponse
import com.mryav.compacttrip.data.model.Mode
import com.mryav.compacttrip.data.model.RealtimeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripMappingTest {

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("fixtures/$name")) { "missing fixture $name" }.readText()

    // --- Live fixtures (captured from the real API, trimmed) ---

    @Test
    fun stopFinder_keepsOnlyStops_bestMatchFirst() {
        val response = TfnswJson.decodeFromString(StopFinderResponse.serializer(), fixture("stop_finder_any_central.json"))
        val stops = response.locations.toStops()

        assertTrue(stops.isNotEmpty())
        assertTrue(stops.none { it.locality == "South Australia" })
        val central = stops.first()
        assertEquals("Central Station", central.name)
        assertEquals("Sydney", central.locality)
        assertTrue(central.modes.containsAll(listOf(Mode.Train, Mode.Metro, Mode.LightRail, Mode.Bus)))
    }

    @Test
    fun trip_centralToRootyHill_mapsToSortedUniqueCards() {
        val summary = TfnswJson.decodeFromString(TripResponse.serializer(), fixture("trip_central_rootyhill.json")).toSummary()

        assertTrue(summary.departures.isNotEmpty())
        assertEquals(summary.departures.size, summary.departures.map { it.key }.toSet().size)
        assertEquals(summary.departures.sortedBy { it.departsAt }, summary.departures)
        val first = summary.departures.first()
        assertEquals("T1", first.badges.first().label)
        assertEquals("Penrith via Parramatta", first.headsign)
        assertEquals("Platform 18", first.platform)
        assertTrue(summary.noticeCount > 0)
    }

    @Test
    fun trip_withoutTrains_usesBusAndLightRailBadges() {
        val summary = TfnswJson.decodeFromString(TripResponse.serializer(), fixture("trip_central_olympicpark_no_train.json")).toSummary()

        assertTrue(summary.departures.isNotEmpty())
        assertTrue(summary.departures.flatMap { it.badges }.none { it.mode == Mode.Train })
    }

    @Test
    fun trip_journeysHaveLegsAndArrival() {
        val summary = TfnswJson.decodeFromString(TripResponse.serializer(), fixture("trip_central_olympicpark.json")).toSummary()
        val journey = summary.journeys.first()

        val rides = journey.legs.filterIsInstance<com.mryav.compacttrip.data.model.JourneyLeg.Ride>()
        assertTrue(rides.isNotEmpty())
        val first = rides.first()
        assertEquals("T2", first.badge.label)
        assertEquals("Central Station, Platform 5", first.from)
        assertEquals("Strathfield Station, Platform 3", first.to)
        assertEquals(4, first.stops) // 5 stops in the sequence, boarding stop excluded
        assertTrue(first.stopNames.isEmpty()) // the fixture's sequence has ids only
        assertTrue(journey.arrivesAt!! > journey.card.departsAt)
        val walks = journey.legs.filterIsInstance<com.mryav.compacttrip.data.model.JourneyLeg.Walk>()
        assertTrue(walks.all { it.departsAt != null && it.arrivesAt != null })
    }

    @Test
    fun ride_listsIntermediateStops_andDelayMinutes() {
        val leg = LegDto(
            duration = 600,
            origin = LegStopDto(
                name = "Bondi Junction Station",
                disassembledName = "Bondi Junction Station, Platform 2",
                departureTimePlanned = "2026-11-08T01:30:00Z",
                departureTimeEstimated = "2026-11-08T01:35:10Z",
            ),
            destination = LegStopDto(name = "Town Hall Station", disassembledName = "Town Hall Station, Platform 4"),
            transportation = TransportationDto(disassembledName = "T4", product = ProductDto(1), destination = NamedDto("Cronulla")),
            stopSequence = listOf(
                StopRefDto(id = "1", disassembledName = "Bondi Junction Station, Platform 2"),
                StopRefDto(id = "2", disassembledName = "Edgecliff Station, Platform 2"),
                StopRefDto(id = "3", name = "Kings Cross Station", properties = StopRefPropertiesDto(platformName = "Platform 2")),
                StopRefDto(id = "4", name = "Martin Place Station"),
                StopRefDto(id = "5", disassembledName = "Town Hall Station, Platform 4"),
            ),
        )
        val ride = listOf(leg).toJourney()!!.legs.single() as com.mryav.compacttrip.data.model.JourneyLeg.Ride
        assertEquals(listOf("Edgecliff Station, Platform 2", "Kings Cross Station, Platform 2", "Martin Place Station"), ride.stopNames)
        assertEquals(4, ride.stops)
        assertEquals(RealtimeStatus.Delayed, ride.status)
        assertEquals(5, ride.delayMinutes)
    }

    @Test
    fun trip_noticesAreDistinct_mostImportantFirst_withContentAndLines() {
        val summary = TfnswJson.decodeFromString(TripResponse.serializer(), fixture("trip_central_olympicpark.json")).toSummary()

        assertEquals(summary.notices.size, summary.notices.map { it.id }.toSet().size)
        val notice = summary.notices.first { it.id == "ems-80459" }
        assertEquals("Buses replace trains between Macarthur, Glenfield and Fairfield", notice.title)
        assertTrue(notice.html!!.contains("<li>"))
        assertEquals("https://transportnsw.info/alerts/details#/ems-80459", notice.url)
        assertTrue(notice.lines.any { it.badge.label == "T2" && it.name!!.startsWith("Leppington & Inner West Line") })
    }

    // --- Rules ---

    private fun leg(
        line: String = "T1",
        productClass: Int = 1,
        planned: String? = "2026-11-08T12:00:00Z",
        estimated: String? = null,
        platform: String? = "Platform 2",
        plannedPlatform: String? = "Platform 2",
        realtimeStatus: List<String> = emptyList(),
        carsFrom: String? = null,
        carsTo: String? = null,
        cars: String? = null,
    ) = LegDto(
        origin = LegStopDto(
            id = "1",
            name = "Central Station, $platform, Sydney",
            disassembledName = "Central Station, $platform",
            departureTimePlanned = planned,
            departureTimeEstimated = estimated,
            properties = LegStopPropertiesDto(platform, plannedPlatform, cars, carsFrom, carsTo),
        ),
        transportation = TransportationDto(disassembledName = line, product = ProductDto(productClass), destination = NamedDto("Somewhere")),
        realtimeStatus = realtimeStatus,
    )

    private val walk = LegDto(transportation = TransportationDto(product = ProductDto(100)))

    @Test
    fun status_onTimeWithinTolerance_delayedBeyond_unknownWithoutEstimate() {
        assertEquals(RealtimeStatus.OnTime, listOf(leg(estimated = "2026-11-08T12:00:59Z")).toCard()!!.status)
        assertEquals(RealtimeStatus.Delayed, listOf(leg(estimated = "2026-11-08T12:01:30Z")).toCard()!!.status)
        assertEquals(RealtimeStatus.Unknown, listOf(leg(estimated = null)).toCard()!!.status)
        assertEquals(
            RealtimeStatus.Cancelled,
            listOf(leg(estimated = "2026-11-08T12:00:00Z", realtimeStatus = listOf("TRIP_CANCELLED", "MONITORED"))).toCard()!!.status,
        )
    }

    @Test
    fun departsAt_prefersEstimate() {
        val card = listOf(leg(estimated = "2026-11-08T12:03:00Z")).toCard()!!
        assertEquals(java.time.Instant.parse("2026-11-08T12:03:00Z").toEpochMilli(), card.departsAt)
    }

    @Test
    fun walkFirst_isSkipped() {
        val card = listOf(walk, leg(line = "N70", productClass = 5)).toCard()!!
        assertEquals(listOf("N70"), card.badges.map { it.label })
        assertEquals(Mode.Bus, card.badges.first().mode)
    }

    @Test
    fun walkOnly_hasNoCard() {
        assertNull(listOf(walk).toCard())
    }

    @Test
    fun walkOnlyJourney_givesWalkMinutes_quickestFirst() {
        val response = TripResponse(
            journeys = listOf(
                JourneyDto(listOf(walk.copy(duration = 400))),
                JourneyDto(listOf(walk.copy(duration = 170))),
                JourneyDto(listOf(walk, leg(line = "T1", productClass = 1))),
            ),
        )
        val summary = response.toSummary()
        assertEquals(3, summary.walkMinutes)
        assertEquals(1, summary.departures.size)
        assertNull(TripResponse(journeys = listOf(JourneyDto(listOf(leg(line = "T1", productClass = 1))))).toSummary().walkMinutes)
    }

    @Test
    fun platformChange_isFlagged() {
        val card = listOf(leg(platform = "Platform 1", plannedPlatform = "Platform 2")).toCard()!!
        assertTrue(card.platformChanged)
        assertEquals("Now Platform 1", card.platform)
        assertFalse(listOf(leg()).toCard()!!.platformChanged)
    }

    @Test
    fun boardCars_onlyWhenPartOfTheTrain() {
        assertEquals("Board cars 5–10", boardCars("5", "10", "10"))
        assertNull(boardCars("1", "8", "8"))
        assertNull(boardCars(null, "8", "8"))
    }

    @Test
    fun busStand_showsStandPart() {
        assertEquals("Everton Rd, Stand B", platformLabel("Strathfield Station, Everton Rd, Stand B", null))
        assertEquals("Platform 5", platformLabel("Platform 5", "Central Station, Platform 5"))
    }
}
