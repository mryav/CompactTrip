package com.mryav.compacttrip.data.dto

import kotlinx.serialization.Serializable

/**
 * `add_info` (service alerts, ~2.5 MB). Only what the Home banner needs is kept; each affected line
 * is decoded as an empty object (counted, not stored) and everything else is skipped.
 */
@Serializable
data class AddInfoResponse(val infos: AddInfoInfosDto = AddInfoInfosDto())

@Serializable
data class AddInfoInfosDto(val current: List<AddInfoDto> = emptyList())

@Serializable
data class AddInfoDto(
    val id: String = "",
    val priority: String? = null,
    /** lineInfo, stopInfo, areaInfo, routeInfo, tripMessage, infrastructure, bannerInfo. */
    val type: String? = null,
    val subtitle: String? = null,
    val urlText: String? = null,
    val url: String? = null,
    /** Alert body, HTML. */
    val content: String? = null,
    val timestamps: AddInfoTimestampsDto = AddInfoTimestampsDto(),
    val affected: AddInfoAffectedDto = AddInfoAffectedDto(),
)

@Serializable
data class AddInfoTimestampsDto(
    val creation: String? = null,
    val lastModification: String? = null,
    val validity: List<ValidityDto> = emptyList(),
)

@Serializable
data class ValidityDto(val from: String? = null, val to: String? = null)

@Serializable
data class AddInfoAffectedDto(val lines: List<AffectedLineDto> = emptyList())

/** Only counted. */
@Serializable
class AffectedLineDto
