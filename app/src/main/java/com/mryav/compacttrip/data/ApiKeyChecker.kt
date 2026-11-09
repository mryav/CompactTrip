package com.mryav.compacttrip.data

import com.mryav.compacttrip.data.dto.StopFinderResponse

enum class KeyCheckResult { Success, Unauthorized, Offline, ServiceUnavailable }

/**
 * Verifies a TfNSW Open Data API key with the smallest Trip Planner call available
 * (stop_finder for Central by stop id, ~600 bytes).
 */
class ApiKeyChecker(private val http: TfnswHttp) {

    suspend fun check(key: String): KeyCheckResult {
        val result = http.get(
            endpoint = "stop_finder",
            params = listOf("type_sf" to "stop", "name_sf" to "10101100", "TfNSWSF" to "true"),
            deserializer = StopFinderResponse.serializer(),
            keyOverride = key,
        )
        return when (result) {
            is ApiResult.Ok -> KeyCheckResult.Success
            ApiResult.Unauthorized, ApiResult.NoKey -> KeyCheckResult.Unauthorized
            ApiResult.Offline -> KeyCheckResult.Offline
            ApiResult.ServiceUnavailable -> KeyCheckResult.ServiceUnavailable
        }
    }
}
