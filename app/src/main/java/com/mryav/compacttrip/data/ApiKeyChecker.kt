package com.mryav.compacttrip.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.URL
import java.net.UnknownHostException

enum class KeyCheckResult { Success, Unauthorized, Offline, ServiceUnavailable }

/**
 * Verifies a TfNSW Open Data API key with the smallest Trip Planner call available
 * (stop_finder for Central by stop id, ~600 bytes). 401/403 = bad key, or the key's
 * Open Data app doesn't have the Trip Planner APIs added.
 */
class ApiKeyChecker(context: Context) {

    private val appContext = context.applicationContext

    // Looked up on first check, not at app start (keeps cold start lean).
    private val connectivity by lazy { appContext.getSystemService(ConnectivityManager::class.java) }

    suspend fun check(key: String): KeyCheckResult = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext KeyCheckResult.Offline
        val connection = try {
            URL(CHECK_URL).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            return@withContext KeyCheckResult.Offline
        }
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            try {
                connection.setRequestProperty("Authorization", "apikey $key")
            } catch (e: IllegalArgumentException) {
                // Characters that can't go in an HTTP header: definitely not a valid key.
                return@withContext KeyCheckResult.Unauthorized
            }
            when (connection.responseCode) {
                in 200..299 -> KeyCheckResult.Success
                401, 403 -> KeyCheckResult.Unauthorized
                else -> KeyCheckResult.ServiceUnavailable
            }
        } catch (e: UnknownHostException) {
            KeyCheckResult.Offline
        } catch (e: ConnectException) {
            KeyCheckResult.Offline
        } catch (e: NoRouteToHostException) {
            KeyCheckResult.Offline
        } catch (e: IOException) {
            if (isOnline()) KeyCheckResult.ServiceUnavailable else KeyCheckResult.Offline
        } finally {
            connection.disconnect()
        }
    }

    private fun isOnline(): Boolean {
        val manager = connectivity ?: return false
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val CHECK_URL = "https://api.transport.nsw.gov.au/v1/tp/stop_finder" +
            "?outputFormat=rapidJSON&coordOutputFormat=EPSG:4326" +
            "&type_sf=stop&name_sf=10101100&TfNSWSF=true"
    }
}
