package com.mryav.compacttrip.data

import android.content.Context
import com.mryav.compacttrip.data.model.SavedTrip
import com.mryav.compacttrip.data.model.Stop
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

    fun add(origin: Stop, destination: Stop): SavedTrip {
        val trip = SavedTrip(id = UUID.randomUUID().toString(), origin = origin, destination = destination)
        writeTrips(_trips.value + trip)
        return trip
    }

    fun remove(id: String) = writeTrips(_trips.value.filterNot { it.id == id })

    /** Last picked stops, newest first (empty-search suggestions). */
    fun recentStops(): List<Stop> = decode(prefs.getString(KEY_RECENT_STOPS, null), stopsSerializer)

    fun rememberStop(stop: Stop) {
        val updated = (listOf(stop) + recentStops().filterNot { it.id == stop.id }).take(MAX_RECENT)
        prefs.edit().putString(KEY_RECENT_STOPS, TfnswJson.encodeToString(stopsSerializer, updated)).commit()
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
    }
}
