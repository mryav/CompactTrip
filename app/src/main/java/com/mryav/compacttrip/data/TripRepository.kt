package com.mryav.compacttrip.data

import com.mryav.compacttrip.data.dto.TripResponse
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.data.model.TripSummary
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TripRepository(private val http: TfnswHttp) {

    /** Next journeys from [origin] to [destination], leaving now (Sydney time). Never cached. */
    suspend fun upcoming(origin: Stop, destination: Stop): ApiResult<TripSummary> {
        val now = ZonedDateTime.now(SYDNEY)
        val result = http.get(
            endpoint = "trip",
            params = listOf(
                "depArrMacro" to "dep",
                "itdDate" to now.format(DATE),
                "itdTime" to now.format(TIME),
                "type_origin" to "stop",
                "name_origin" to origin.id,
                "type_destination" to "stop",
                "name_destination" to destination.id,
                "calcNumberOfTrips" to "6",
                "TfNSWTR" to "true",
            ),
            deserializer = TripResponse.serializer(),
        )
        return when (result) {
            is ApiResult.Ok -> ApiResult.Ok(result.value.toSummary())
            ApiResult.Unauthorized -> ApiResult.Unauthorized
            ApiResult.Offline -> ApiResult.Offline
            ApiResult.ServiceUnavailable -> ApiResult.ServiceUnavailable
            ApiResult.NoKey -> ApiResult.NoKey
        }
    }

    private companion object {
        val SYDNEY: ZoneId = ZoneId.of("Australia/Sydney")
        val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm")
    }
}
