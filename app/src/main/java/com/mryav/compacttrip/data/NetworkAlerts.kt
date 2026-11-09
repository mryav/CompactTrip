package com.mryav.compacttrip.data

import com.mryav.compacttrip.data.dto.AddInfoDto
import com.mryav.compacttrip.data.dto.AddInfoResponse
import com.mryav.compacttrip.data.model.Notice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Alerts this broad (or area-wide) can go on Home, if they're also high priority. */
private const val MANY_LINES = 5
/** `bannerInfo` per the Figma note; TfNSW currently marks network-wide alerts `areaInfo`. */
private val NETWORK_TYPES = setOf("bannerInfo", "areaInfo")
private val HIGH_PRIORITIES = setOf("high", "veryHigh")

/**
 * Network alerts for the top of Home (Figma annotation on the high-priority banner): shown to
 * everyone when priority is high/veryHigh and the alert is network-wide or affects [MANY_LINES]+
 * lines. The feed is large and changes slowly, so it's fetched at most every [REFRESH_MS]; the last
 * good list stays if a fetch fails.
 */
class NetworkAlerts(private val http: TfnswHttp, private val store: SavedTripsStore, private val scope: CoroutineScope) {
    private val all = MutableStateFlow<List<Notice>>(emptyList())
    private val hidden = MutableStateFlow(store.hiddenAlerts())

    /** What Home shows: the current alerts minus the ones the user hid. */
    val alerts: StateFlow<List<Notice>> = combine(all, hidden) { list, hide -> list.filterNot { it.hideKey in hide } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * Hide [notice] from Home. Keyed by id + last update, so if TfNSW updates it, it shows again.
     * Keys for alerts that have ended are dropped on the next fetch.
     */
    fun hide(notice: Notice) {
        hidden.update { it + notice.hideKey }
        store.saveHiddenAlerts(hidden.value)
    }

    private var fetchedAt = 0L
    private var job: Job? = null

    fun refresh(ifOlderThanMs: Long = REFRESH_MS) {
        if (job?.isActive == true) return
        if (System.currentTimeMillis() - fetchedAt < ifOlderThanMs) return
        job = scope.launch {
            val today = ZonedDateTime.now(SYDNEY).format(DATE)
            val result = http.get(
                endpoint = "add_info",
                params = listOf("filterDateValid" to today, "filterPublicationStatus" to "current"),
                deserializer = AddInfoResponse.serializer(),
            )
            if (result is ApiResult.Ok) {
                val current = result.value.infos.current.toNetworkAlerts(Instant.now())
                all.value = current
                fetchedAt = System.currentTimeMillis()
                // Forget hidden keys for alerts that no longer exist.
                val live = current.mapTo(HashSet()) { it.hideKey }
                if (hidden.value.any { it !in live }) {
                    hidden.update { keys -> keys.filterTo(HashSet()) { it in live } }
                    store.saveHiddenAlerts(hidden.value)
                }
            }
        }
    }

    companion object {
        const val REFRESH_MS = 10 * 60_000L
        private val SYDNEY: ZoneId = ZoneId.of("Australia/Sydney")
        private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    }
}

private val Notice.hideKey: String get() = "$id@${updatedAt ?: 0}"

/** The Home-worthy alerts in [this], most important first, then newest. */
internal fun List<AddInfoDto>.toNetworkAlerts(now: Instant): List<Notice> =
    asSequence()
        .filter { it.priority in HIGH_PRIORITIES }
        .filter { it.type in NETWORK_TYPES || it.affected.lines.size >= MANY_LINES }
        .filter { it.isValidAt(now) }
        .sortedWith(compareByDescending<AddInfoDto> { priorityRank(it.priority) }.thenByDescending { it.updatedAt() ?: 0L })
        .map { info ->
            Notice(
                id = info.id,
                title = info.subtitle?.takeIf(String::isNotBlank) ?: info.urlText?.takeIf(String::isNotBlank) ?: "Service alert",
                urgent = true,
                html = info.content?.takeIf(String::isNotBlank),
                url = info.url?.takeIf(String::isNotBlank),
                updatedAt = info.updatedAt(),
                lines = emptyList(),
            )
        }
        .toList()

/** In force now: any validity window covers [now] (no windows, or unreadable dates: assume valid). */
private fun AddInfoDto.isValidAt(now: Instant): Boolean {
    val windows = timestamps.validity
    if (windows.isEmpty()) return true
    return windows.any { w ->
        val from = w.from?.toInstantOrNull()
        val to = w.to?.toInstantOrNull()
        (from == null || !now.isBefore(from)) && (to == null || !now.isAfter(to))
    }
}

private fun AddInfoDto.updatedAt(): Long? =
    (timestamps.lastModification ?: timestamps.creation)?.toInstantOrNull()?.toEpochMilli()

private fun String.toInstantOrNull(): Instant? = runCatching { Instant.parse(this) }.getOrNull()
