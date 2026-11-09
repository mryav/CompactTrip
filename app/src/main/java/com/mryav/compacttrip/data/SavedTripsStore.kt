package com.mryav.compacttrip.data

import android.content.Context
import com.mryav.compacttrip.data.model.RouteFor
import com.mryav.compacttrip.data.model.SavedTrip
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.data.model.Timing
import com.mryav.compacttrip.data.model.TravelMode
import com.mryav.compacttrip.data.model.TripPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import java.util.UUID

/**
 * Saved trips and recently picked stops. Same retention approach as [ApiKeyStore]: private
 * SharedPreferences, commit() writes, included in backup/device transfer (res/xml backup rules).
 */
class SavedTripsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val tripsSerializer = ListSerializer(SavedTrip.serializer())
    private val stopsSerializer = ListSerializer(Stop.serializer())

    private val _trips = MutableStateFlow(readTrips())
    val trips: StateFlow<List<SavedTrip>> = _trips.asStateFlow()

    fun add(origin: Stop, destination: Stop, preferences: TripPreferences = TripPreferences.Default): SavedTrip {
        val trip = SavedTrip(
            id = UUID.randomUUID().toString(),
            origin = origin,
            destination = destination,
            preferences = preferences.copy(timing = Timing.Now, at = null),
        )
        writeTrips(_trips.value + trip)
        return trip
    }

    fun remove(id: String) = writeTrips(_trips.value.filterNot { it.id == id })

    fun updatePreferences(id: String, preferences: TripPreferences) = writeTrips(
        _trips.value.map { if (it.id == id) it.copy(preferences = preferences.copy(timing = Timing.Now, at = null)) else it },
    )

    /** Last picked stops, newest first (empty-search suggestions). */
    fun recentStops(): List<Stop> = decode(prefs.getString(KEY_RECENT_STOPS, null), stopsSerializer)

    fun rememberStop(stop: Stop) {
        val updated = (listOf(stop) + recentStops().filterNot { it.id == stop.id }).take(MAX_RECENT)
        prefs.edit().putString(KEY_RECENT_STOPS, TfnswJson.encodeToString(stopsSerializer, updated)).commit()
    }

    /**
     * Standing preferences (modes, accessibility, route type): the starting point for a new plan or a
     * new saved trip. Timing isn't kept: a plan always starts from "now".
     */
    fun planPreferences(): TripPreferences = TripPreferences(
        accessibleOnly = prefs.getBoolean(KEY_ACCESSIBLE, false),
        modes = prefs.getStringSet(KEY_MODES, null)
            ?.mapNotNull { name -> TravelMode.entries.firstOrNull { it.name == name } }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: TravelMode.entries.toSet(),
        routeFor = RouteFor.entries.firstOrNull { it.name == prefs.getString(KEY_ROUTE_FOR, null) } ?: RouteFor.EarliestArrival,
    )

    fun savePlanPreferences(p: TripPreferences) {
        prefs.edit()
            .putBoolean(KEY_ACCESSIBLE, p.accessibleOnly)
            .putStringSet(KEY_MODES, p.modes.mapTo(HashSet()) { it.name })
            .putString(KEY_ROUTE_FOR, p.routeFor.name)
            .commit()
    }

    private fun readTrips(): List<SavedTrip> = decode(prefs.getString(KEY_TRIPS, null), tripsSerializer)

    private fun writeTrips(trips: List<SavedTrip>) {
        prefs.edit().putString(KEY_TRIPS, TfnswJson.encodeToString(tripsSerializer, trips)).commit()
        _trips.value = trips
    }

    private fun <T> decode(raw: String?, serializer: kotlinx.serialization.KSerializer<List<T>>): List<T> =
        if (raw.isNullOrBlank()) emptyList() else runCatching { TfnswJson.decodeFromString(serializer, raw) }.getOrDefault(emptyList())

    private companion object {
        // Referenced by res/xml backup_rules.xml and data_extraction_rules.xml. Keep in sync.
        const val FILE_NAME = "trips"
        const val KEY_TRIPS = "saved_trips"
        const val KEY_RECENT_STOPS = "recent_stops"
        const val MAX_RECENT = 6
        const val KEY_ACCESSIBLE = "plan_accessible_only"
        const val KEY_MODES = "plan_modes"
        const val KEY_ROUTE_FOR = "plan_route_for"
    }
}
