package com.mryav.compacttrip.data

import androidx.compose.runtime.Immutable
import com.mryav.compacttrip.data.model.TripSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

enum class LoadError { Offline, KeyProblem, Unavailable }

@Immutable
data class TripLoad(
    /** Last good data; kept while refreshing or after an error so the screen never blanks. */
    val summary: TripSummary? = null,
    val error: LoadError? = null,
)

@Immutable
data class BoardState(
    val loads: Map<String, TripLoad> = emptyMap(),
    /** When data last arrived (epoch millis), for "Updated Ns ago". */
    val updatedAt: Long? = null,
    val refreshing: Boolean = false,
)

/**
 * Live departures for every saved trip. Lives for the whole app process, so returning to Home
 * shows the last data instantly while a refresh runs.
 */
class TripBoard(
    private val trips: TripRepository,
    private val saved: SavedTripsStore,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(BoardState())
    val state: StateFlow<BoardState> = _state.asStateFlow()
    private var job: Job? = null

    /** Refresh all trips. With [ifOlderThanMs], skips if data is fresher than that. */
    fun refresh(ifOlderThanMs: Long? = null) {
        if (job?.isActive == true) return
        val updatedAt = _state.value.updatedAt
        val missing = saved.trips.value.any { it.id !in _state.value.loads }
        if (!missing && ifOlderThanMs != null && updatedAt != null &&
            System.currentTimeMillis() - updatedAt < ifOlderThanMs
        ) return

        job = scope.launch {
            val list = saved.trips.value
            if (list.isEmpty()) {
                _state.value = BoardState()
                return@launch
            }
            _state.update { it.copy(refreshing = true) }
            val gate = Semaphore(MAX_PARALLEL)
            val results = list.map { trip ->
                async { trip.id to gate.withPermit { trips.upcoming(trip.origin, trip.destination, trip.preferences) } }
            }.awaitAll().toMap()

            _state.update { old ->
                val loads = list.associate { trip ->
                    val previous = old.loads[trip.id]
                    trip.id to when (val r = results.getValue(trip.id)) {
                        is ApiResult.Ok -> TripLoad(summary = r.value)
                        ApiResult.Offline -> TripLoad(previous?.summary, LoadError.Offline)
                        ApiResult.Unauthorized, ApiResult.NoKey -> TripLoad(previous?.summary, LoadError.KeyProblem)
                        ApiResult.ServiceUnavailable -> TripLoad(previous?.summary, LoadError.Unavailable)
                    }
                }
                BoardState(
                    loads = loads,
                    updatedAt = if (results.values.any { it is ApiResult.Ok }) System.currentTimeMillis() else old.updatedAt,
                    refreshing = false,
                )
            }
        }
    }

    private companion object {
        /** TfNSW allows ~5 requests/s; stay well under. */
        const val MAX_PARALLEL = 3
    }
}
