package com.mryav.compacttrip.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException

/** Outcome of a Trip Planner call. Errors never touch the saved key. */
sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    /** HTTP 401/403: bad key, or the key's Open Data app lacks the Trip Planner APIs. */
    data object Unauthorized : ApiResult<Nothing>
    data object Offline : ApiResult<Nothing>
    /** Server error, timeout or unreadable response. */
    data object ServiceUnavailable : ApiResult<Nothing>
    data object NoKey : ApiResult<Nothing>
}

/** JSON settings for TfNSW responses: only the fields we declare are read; the rest is skipped. */
val TfnswJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

/**
 * Minimal HTTP client for the TfNSW Trip Planner APIs (no extra libraries: Android's
 * HttpURLConnection already does gzip). Responses are decoded straight from the stream.
 */
class TfnswHttp(context: Context, private val savedKey: () -> String?) {

    private val appContext = context.applicationContext
    private val connectivity by lazy { appContext.getSystemService(ConnectivityManager::class.java) }

    /**
     * GET [endpoint] (e.g. "trip") with [params]. Uses the saved key unless [keyOverride] is given
     * (the API key screen checks a key before saving it).
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun <T> get(
        endpoint: String,
        params: List<Pair<String, String>>,
        deserializer: DeserializationStrategy<T>,
        keyOverride: String? = null,
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        val key = keyOverride ?: savedKey() ?: return@withContext ApiResult.NoKey
        if (!isOnline()) return@withContext ApiResult.Offline

        val query = (COMMON_PARAMS + params).joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        val connection = try {
            URL("$BASE_URL$endpoint?$query").openConnection() as HttpURLConnection
        } catch (e: IOException) {
            return@withContext ApiResult.Offline
        }
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            try {
                connection.setRequestProperty("Authorization", "apikey $key")
            } catch (e: IllegalArgumentException) {
                return@withContext ApiResult.Unauthorized // not even a valid header value
            }
            when (connection.responseCode) {
                in 200..299 -> connection.inputStream.buffered().use { stream ->
                    ApiResult.Ok(TfnswJson.decodeFromStream(deserializer, stream))
                }
                401, 403 -> ApiResult.Unauthorized
                else -> ApiResult.ServiceUnavailable
            }
        } catch (e: UnknownHostException) {
            ApiResult.Offline
        } catch (e: ConnectException) {
            ApiResult.Offline
        } catch (e: NoRouteToHostException) {
            ApiResult.Offline
        } catch (e: SerializationException) {
            ApiResult.ServiceUnavailable
        } catch (e: IllegalArgumentException) {
            ApiResult.ServiceUnavailable // malformed JSON surfaces as this too
        } catch (e: IOException) {
            if (isOnline()) ApiResult.ServiceUnavailable else ApiResult.Offline
        } finally {
            connection.disconnect()
        }
    }

    /** Returns once the device has an internet-capable network (immediately if it already has). */
    suspend fun awaitOnline() {
        if (isOnline()) return
        val manager = connectivity ?: return
        val online = CompletableDeferred<Unit>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) online.complete(Unit)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        try {
            online.await()
        } finally {
            manager.unregisterNetworkCallback(callback)
        }
    }

    private fun isOnline(): Boolean {
        val manager = connectivity ?: return false
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private companion object {
        const val BASE_URL = "https://api.transport.nsw.gov.au/v1/tp/"
        const val TIMEOUT_MS = 10_000
        val COMMON_PARAMS = listOf("outputFormat" to "rapidJSON", "coordOutputFormat" to "EPSG:4326")
    }
}
