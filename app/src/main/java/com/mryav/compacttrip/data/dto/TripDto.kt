package com.mryav.compacttrip.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * trip response: only the fields the app uses. The big ones (coords, stopSequence, footPathInfo,
 * ~75% of the payload) are deliberately absent so they're skipped while streaming, never held in memory.
 */
@Serializable
data class TripResponse(val journeys: List<JourneyDto> = emptyList())

@Serializable
data class JourneyDto(val legs: List<LegDto> = emptyList())

@Serializable
data class LegDto(
    val duration: Int = 0,
    val origin: LegStopDto = LegStopDto(),
    val destination: LegStopDto = LegStopDto(),
    val transportation: TransportationDto? = null,
    val realtimeStatus: List<String> = emptyList(),
    val isRealtimeControlled: Boolean = false,
    val infos: List<InfoDto> = emptyList(),
    /** Only counted ("Ride 3 stops"); each entry is read as an id so the rest is skipped. */
    val stopSequence: List<StopRefDto> = emptyList(),
)

@Serializable
data class StopRefDto(val id: String = "")

@Serializable
data class LegStopDto(
    val id: String = "",
    val name: String = "",
    val disassembledName: String? = null,
    val departureTimePlanned: String? = null,
    val departureTimeEstimated: String? = null,
    val arrivalTimePlanned: String? = null,
    val arrivalTimeEstimated: String? = null,
    val properties: LegStopPropertiesDto = LegStopPropertiesDto(),
)

@Serializable
data class LegStopPropertiesDto(
    val platformName: String? = null,
    val plannedPlatformName: String? = null,
    @SerialName("NumberOfCars") val numberOfCars: String? = null,
    @SerialName("TravelInCarsFrom") val travelInCarsFrom: String? = null,
    @SerialName("TravelInCarsTo") val travelInCarsTo: String? = null,
)

@Serializable
data class TransportationDto(
    val disassembledName: String? = null,
    val number: String? = null,
    val description: String? = null,
    val product: ProductDto = ProductDto(),
    val destination: NamedDto? = null,
)

@Serializable
data class ProductDto(@SerialName("class") val productClass: Int = 0)

@Serializable
data class InfoDto(
    val id: String = "",
    val priority: String? = null,
    val subtitle: String? = null,
    val urlText: String? = null,
    val url: String? = null,
    /** Alert body, HTML. */
    val content: String? = null,
    val timestamps: InfoTimestampsDto = InfoTimestampsDto(),
)

@Serializable
data class InfoTimestampsDto(val creation: String? = null, val lastModification: String? = null)
