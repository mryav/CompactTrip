package com.mryav.compacttrip

import android.app.Application
import com.mryav.compacttrip.data.ApiKeyChecker
import com.mryav.compacttrip.data.ApiKeyStore
import com.mryav.compacttrip.data.SavedTripsStore
import com.mryav.compacttrip.data.StopRepository
import com.mryav.compacttrip.data.TfnswHttp
import com.mryav.compacttrip.data.NetworkAlerts
import com.mryav.compacttrip.data.TripBoard
import com.mryav.compacttrip.data.TripRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency wiring (no DI framework). Everything is cheap to construct. */
class AppContainer(app: Application) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val keyStore = ApiKeyStore(app)
    val http = TfnswHttp(app) { keyStore.apiKey }
    val keyChecker = ApiKeyChecker(http)
    val stops = StopRepository(http)
    val trips = TripRepository(http)
    val savedTrips = SavedTripsStore(app)
    val board = TripBoard(trips, savedTrips, appScope)
    val alerts = NetworkAlerts(http, savedTrips, appScope)
}

class CompactTripsApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
