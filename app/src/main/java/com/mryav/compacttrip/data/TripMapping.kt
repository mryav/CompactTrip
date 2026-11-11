package com.mryav.compacttrip.data

import com.mryav.compacttrip.data.dto.InfoDto
import com.mryav.compacttrip.data.dto.LegDto
import com.mryav.compacttrip.data.dto.LocationDto
import com.mryav.compacttrip.data.dto.StopRefDto
import com.mryav.compacttrip.data.dto.TripResponse
import com.mryav.compacttrip.data.model.DepartureCard
import com.mryav.compacttrip.data.model.Journey
import com.mryav.compacttrip.data.model.JourneyLeg
import com.mryav.compacttrip.data.model.LineBadge
import com.mryav.compacttrip.data.model.LineRef
import com.mryav.compacttrip.data.model.Mode
import com.mryav.compacttrip.data.model.Notice
import com.mryav.compacttrip.data.model.RealtimeStatus
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.data.model.TripSummary
import java.time.Instant

/** Estimated within this of planned counts as on time. */
private const val ON_TIME_TOLERANCE_MS = 59_000L
private const val MAX_BADGES = 3

/** stop_finder (type_sf=any) → pickable stops, best match first. */
fun List<LocationDto>.toStops(limit: Int = 10): List<Stop> =
    asSequence()
        .filter { it.type == "stop" && it.id.isNotBlank() && it.isInNsw() }
        .sortedByDescending { it.matchQuality }
        .take(limit)
        .map { it.toStop() }
        .toList()

/**
 * stop_finder also returns interstate coach stops (e.g. Adelaide). Keep NSW + ACT by position;
 * stops without coordinates are kept.
 */
internal fun LocationDto.isInNsw(): Boolean {
    if (coord.size < 2) return true
    val (lat, lon) = coord
    return lon >= 140.99 && lat in -37.6..-28.1
}

fun LocationDto.toStop(): Stop {
    val locality = parent?.name?.takeIf { it.isNotBlank() }
    // "Central Station, Sydney" → "Central Station"
    val title = if (locality != null) name.removeSuffix(", $locality") else name
    val short = disassembledName?.takeIf { it.isNotBlank() } ?: title
    return Stop(
        id = id,
        name = title,
        shortName = short.removeSuffix(" Station"),
        locality = locality,
        modes = modes.mapNotNull(Mode::fromClass).filterNot(Mode::isWalk).distinct(),
    )
}

/**
 * trip → summary: one journey per distinct service, the trip's notices, and the quickest walk-only
 * option if the planner suggests one (those have no service, so they aren't journeys).
 */
fun TripResponse.toSummary(): TripSummary {
    val journeys = journeys.mapNotNull { it.legs.toJourney() }
        .distinctBy { it.key }
        .sortedBy { it.card.departsAt }
    val walkSeconds = this.journeys
        .filter { j -> j.legs.isNotEmpty() && j.legs.all { it.isWalk() } }
        .minOfOrNull { j -> j.legs.sumOf { it.duration } }
    return TripSummary(
        journeys = journeys,
        notices = this.journeys.flatMap { it.legs }.toNotices(),
        walkMinutes = walkSeconds?.let { ((it + 59) / 60).coerceAtLeast(1) },
    )
}

private fun LegDto.isWalk(): Boolean = Mode.fromClass(transportation?.product?.productClass ?: 0)?.isWalk == true

/** Distinct notices across [this] legs, most important first, each with the lines it appeared on. */
internal fun List<LegDto>.toNotices(): List<Notice> {
    val linesById = LinkedHashMap<String, MutableList<LineRef>>()
    val infoById = LinkedHashMap<String, InfoDto>()
    for (leg in this) {
        val line = leg.lineRef()
        for (info in leg.infos) {
            if (info.id.isBlank()) continue
            infoById.putIfAbsent(info.id, info)
            val lines = linesById.getOrPut(info.id) { mutableListOf() }
            if (line != null && lines.none { it.badge == line.badge }) lines += line
        }
    }
    return infoById.values
        .sortedByDescending { priorityRank(it.priority) }
        .map { info ->
            Notice(
                id = info.id,
                title = info.subtitle?.takeIf(String::isNotBlank) ?: info.urlText?.takeIf(String::isNotBlank) ?: "Service notice",
                urgent = priorityRank(info.priority) >= priorityRank("high"),
                html = info.content?.takeIf(String::isNotBlank),
                url = info.url?.takeIf(String::isNotBlank),
                updatedAt = (info.timestamps.lastModification ?: info.timestamps.creation)?.toEpochMillis(),
                lines = linesById[info.id].orEmpty(),
            )
        }
}

internal fun List<LegDto>.toJourney(): Journey? {
    val card = toCard() ?: return null
    val legs = mapNotNull { leg ->
        val mode = Mode.fromClass(leg.transportation?.product?.productClass ?: 0) ?: return@mapNotNull null
        if (mode.isWalk) {
            val minutes = (leg.duration + 59) / 60
            if (minutes > 0) {
                JourneyLeg.Walk(
                    minutes = minutes,
                    departsAt = (leg.origin.departureTimeEstimated ?: leg.origin.departureTimePlanned)?.toEpochMillis(),
                    arrivesAt = (leg.destination.arrivalTimeEstimated ?: leg.destination.arrivalTimePlanned)?.toEpochMillis(),
                )
            } else {
                null
            }
        } else {
            leg.toRide(mode)
        }
    }
    val last = lastOrNull()?.destination
    val arrives = (last?.arrivalTimeEstimated ?: last?.arrivalTimePlanned)?.toEpochMillis()
    return Journey(card = card, arrivesAt = arrives, legs = legs)
}

private fun LegDto.toRide(mode: Mode): JourneyLeg.Ride? {
    val t = transportation ?: return null
    val planned = origin.departureTimePlanned?.toEpochMillis() ?: return null
    val estimated = origin.departureTimeEstimated?.toEpochMillis()
    val arrival = (destination.arrivalTimeEstimated ?: destination.arrivalTimePlanned)?.toEpochMillis()
    return JourneyLeg.Ride(
        badge = LineBadge(t.disassembledName ?: t.number ?: mode.name, mode),
        headsign = t.destination?.name ?: destination.name,
        from = origin.disassembledName ?: origin.name,
        to = destination.disassembledName ?: destination.name,
        departsAt = estimated ?: planned,
        arrivesAt = arrival,
        status = statusOf(planned, estimated, realtimeStatus),
        delayMinutes = delayMinutes(planned, estimated),
        stops = (stopSequence.size - 1).coerceAtLeast(1),
        stopNames = stopSequence.drop(1).dropLast(1).mapNotNull { it.label() },
        minutes = (duration + 59) / 60,
        noticeIds = infos.map { it.id }.filter(String::isNotBlank).distinct(),
    )
}

/** "Edgecliff Station, Platform 2": the planner's own name, or name + platform when it has only the parts. */
private fun StopRefDto.label(): String? {
    disassembledName?.takeIf(String::isNotBlank)?.let { return it }
    val name = name?.takeIf(String::isNotBlank) ?: return null
    val platform = properties.platformName?.takeIf(String::isNotBlank)
    return if (platform != null) "$name, $platform" else name
}

/** Whole minutes late, 0 when on time (or not live). */
private fun delayMinutes(planned: Long, estimated: Long?): Int =
    if (estimated == null || estimated - planned <= ON_TIME_TOLERANCE_MS) 0 else ((estimated - planned) / 60_000).toInt()

/** "T2 Leppington & Inner West Line" + description → badge T2, "Leppington & Inner West Line (City to …)". */
private fun LegDto.lineRef(): LineRef? {
    val t = transportation ?: return null
    val mode = Mode.fromClass(t.product.productClass)?.takeUnless { it.isWalk } ?: return null
    val code = t.disassembledName ?: t.number ?: return null
    val name = t.number?.removePrefix(code)?.trim()?.takeIf(String::isNotBlank)
    val description = t.description?.takeIf(String::isNotBlank)
    val full = when {
        name != null && description != null -> "$name ($description)"
        else -> name ?: description
    }
    return LineRef(LineBadge(code, mode), full)
}

private fun statusOf(planned: Long, estimated: Long?, realtimeStatus: List<String>): RealtimeStatus = when {
    realtimeStatus.any { it.equals("TRIP_CANCELLED", ignoreCase = true) } -> RealtimeStatus.Cancelled
    estimated == null -> RealtimeStatus.Unknown
    estimated - planned > ON_TIME_TOLERANCE_MS -> RealtimeStatus.Delayed
    else -> RealtimeStatus.OnTime
}

internal fun List<LegDto>.toCard(): DepartureCard? {
    val rides = filter { leg -> Mode.fromClass(leg.transportation?.product?.productClass ?: 0)?.isWalk == false }
    val first = rides.firstOrNull() ?: return null
    val transport = first.transportation ?: return null
    val origin = first.origin

    val planned = origin.departureTimePlanned?.toEpochMillis() ?: return null
    val estimated = origin.departureTimeEstimated?.toEpochMillis()
    val status = statusOf(planned, estimated, flatMap { it.realtimeStatus })

    val props = origin.properties
    val platformChanged = props.platformName != null && props.plannedPlatformName != null &&
        props.platformName != props.plannedPlatformName
    val platform = platformLabel(props.platformName, origin.disassembledName)

    return DepartureCard(
        key = "${transport.disassembledName}@$planned@${origin.id}",
        badges = rides.take(MAX_BADGES).mapNotNull { leg ->
            val t = leg.transportation ?: return@mapNotNull null
            val mode = Mode.fromClass(t.product.productClass) ?: return@mapNotNull null
            LineBadge(label = t.disassembledName ?: t.number ?: mode.name, mode = mode)
        },
        headsign = transport.destination?.name ?: first.destination.name,
        platform = if (platformChanged && platform != null) "Now $platform" else platform,
        platformChanged = platformChanged,
        boardCars = boardCars(props.travelInCarsFrom, props.travelInCarsTo, props.numberOfCars),
        departsAt = estimated ?: planned,
        status = status,
        delayMinutes = delayMinutes(planned, estimated),
    )
}

/** Trains: "Platform 5". Buses/ferries: the stand part, e.g. "Everton Rd, Stand B". */
internal fun platformLabel(platformName: String?, stopName: String?): String? {
    if (platformName != null && platformName.startsWith("Platform", ignoreCase = true)) return platformName
    val source = platformName ?: stopName ?: return null
    return source.substringAfter(", ", source).takeIf { it.isNotBlank() }
}

/** Only worth showing when the customer must use part of the train. */
internal fun boardCars(from: String?, to: String?, total: String?): String? {
    val f = from?.toIntOrNull() ?: return null
    val t = to?.toIntOrNull() ?: return null
    val n = total?.toIntOrNull()
    if (f <= 1 && n != null && t >= n) return null // any car
    return "Board cars $f–$t"
}

/** TfNSW alert priorities, least to most important. */
internal fun priorityRank(priority: String?): Int = when (priority) {
    "veryLow" -> 0
    "low" -> 1
    "normal" -> 2
    "high" -> 3
    "veryHigh" -> 4
    else -> 2
}

private fun String.toEpochMillis(): Long? = runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()
