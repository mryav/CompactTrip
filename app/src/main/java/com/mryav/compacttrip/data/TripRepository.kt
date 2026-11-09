package com.mryav.compacttrip.data

import com.mryav.compacttrip.data.dto.TripResponse
import com.mryav.compacttrip.data.model.RouteFor
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.data.model.Timing
import com.mryav.compacttrip.data.model.TripPreferences
import com.mryav.compacttrip.data.model.TripSummary
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TripRepository(private val http: TfnswHttp) {

    /** Told about every successful load (widgets showing the same route update from it). */
    var onLoaded: ((origin: Stop, destination: Stop, preferences: TripPreferences, summary: TripSummary) -> Unit)? = null

    /**
     * Next journeys from [origin] to [destination] (Sydney time), leaving now unless [preferences]
     * say otherwise. Never cached.
     */
    suspend fun upcoming(
        origin: Stop,
        destination: Stop,
        preferences: TripPreferences = TripPreferences.Default,
    ): ApiResult<TripSummary> {
        val result = http.get(
            endpoint = "trip",
            params = tripParams(origin, destination, preferences, ZonedDateTime.now(SYDNEY).toLocalDateTime()),
            deserializer = TripResponse.serializer(),
        )
        return when (result) {
            is ApiResult.Ok -> {
                val summary = result.value.toSummary()
                onLoaded?.invoke(origin, destination, preferences, summary)
                ApiResult.Ok(summary)
            }
            ApiResult.Unauthorized -> ApiResult.Unauthorized
            ApiResult.Offline -> ApiResult.Offline
            ApiResult.ServiceUnavailable -> ApiResult.ServiceUnavailable
            ApiResult.NoKey -> ApiResult.NoKey
        }
    }

    internal companion object {
        val SYDNEY: ZoneId = ZoneId.of("Australia/Sydney")

        /** Trip planner query. Excluded modes use `excludedMeans=checkbox` + `exclMOT_<class>=1`. */
        fun tripParams(origin: Stop, destination: Stop, prefs: TripPreferences, now: LocalDateTime): List<Pair<String, String>> {
            val time = if (prefs.timing == Timing.Now) now else prefs.at ?: now
            return buildList {
                add("depArrMacro" to if (prefs.timing == Timing.ArriveBy) "arr" else "dep")
                add("itdDate" to time.format(DATE))
                add("itdTime" to time.format(TIME))
                add("type_origin" to "stop")
                add("name_origin" to origin.id)
                add("type_destination" to "stop")
                add("name_destination" to destination.id)
                add("calcNumberOfTrips" to "6")
                add("TfNSWTR" to "true")
                if (prefs.routeFor != RouteFor.EarliestArrival) add("routeType" to prefs.routeFor.apiValue)
                if (prefs.accessibleOnly) add("wheelchair" to "on")
                val excluded = prefs.excludedModes.flatMap { it.productClasses }
                if (excluded.isNotEmpty()) {
                    add("excludedMeans" to "checkbox")
                    excluded.sorted().forEach { add("exclMOT_$it" to "1") }
                }
            }
        }

        val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm")
    }
}
