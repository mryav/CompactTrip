package com.mryav.compacttrip.data.dto

import kotlinx.serialization.Serializable

/** stop_finder response: only the fields the app uses. */
@Serializable
data class StopFinderResponse(val locations: List<LocationDto> = emptyList())

@Serializable
data class LocationDto(
    val id: String = "",
    val name: String = "",
    val disassembledName: String? = null,
    val type: String = "",
    val matchQuality: Int = 0,
    val modes: List<Int> = emptyList(),
    val parent: NamedDto? = null,
    /** [latitude, longitude] (EPSG:4326). */
    val coord: List<Double> = emptyList(),
)

@Serializable
data class NamedDto(val name: String? = null)
