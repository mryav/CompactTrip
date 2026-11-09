package com.mryav.compacttrip.data

import com.mryav.compacttrip.data.dto.StopFinderResponse
import com.mryav.compacttrip.data.model.Stop

class StopRepository(private val http: TfnswHttp) {

    /** Stops matching [query], best first (stop_finder type_sf=any, filtered to stops). */
    suspend fun search(query: String): ApiResult<List<Stop>> =
        when (
            val result = http.get(
                endpoint = "stop_finder",
                params = listOf("type_sf" to "any", "name_sf" to query, "TfNSWSF" to "true", "anyMaxSizeHitList" to "20"),
                deserializer = StopFinderResponse.serializer(),
            )
        ) {
            is ApiResult.Ok -> ApiResult.Ok(result.value.locations.toStops())
            ApiResult.Unauthorized -> ApiResult.Unauthorized
            ApiResult.Offline -> ApiResult.Offline
            ApiResult.ServiceUnavailable -> ApiResult.ServiceUnavailable
            ApiResult.NoKey -> ApiResult.NoKey
        }
}
