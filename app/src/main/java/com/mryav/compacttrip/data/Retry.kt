package com.mryav.compacttrip.data

import kotlinx.coroutines.delay

/** First automatic retry after a failed request; doubles each time up to [MAX_RETRY_S]. Kept long to avoid API throttling. */
private const val FIRST_RETRY_S = 10
private const val MAX_RETRY_S = 60
/** After coming back online, at least this long since the last attempt before trying again. */
private const val OFFLINE_RETRY_GAP_MS = 10_000L

/** What a screen shows while [loadWithRetry] is recovering. */
sealed interface RetryStatus {
    /** Offline; waiting for a network. */
    data object Offline : RetryStatus
    /** The request failed; trying again in [seconds] (counts down every second). */
    data class Waiting(val seconds: Int) : RetryStatus
    /** Trying again now. */
    data object Retrying : RetryStatus
}

/**
 * Runs [load] until it gets an answer worth showing: [ApiResult.Ok], or a key problem (retrying
 * won't fix that). Offline waits for a network; other failures retry after 10 s, 20 s, 40 s, then
 * every 60 s. [onStatus] reports each step. Cancel the calling coroutine to stop.
 */
suspend fun <T> loadWithRetry(
    load: suspend () -> ApiResult<T>,
    awaitOnline: suspend () -> Unit,
    onStatus: (RetryStatus) -> Unit,
): ApiResult<T> {
    var failures = 0
    while (true) {
        val attemptAt = System.currentTimeMillis()
        when (val r = load()) {
            is ApiResult.Ok, ApiResult.Unauthorized, ApiResult.NoKey -> return r
            ApiResult.Offline -> {
                onStatus(RetryStatus.Offline)
                awaitOnline()
                val wait = OFFLINE_RETRY_GAP_MS - (System.currentTimeMillis() - attemptAt)
                if (wait > 0) delay(wait)
            }
            ApiResult.ServiceUnavailable -> {
                failures++
                val seconds = (FIRST_RETRY_S shl (failures - 1).coerceAtMost(3)).coerceAtMost(MAX_RETRY_S)
                for (s in seconds downTo 1) {
                    onStatus(RetryStatus.Waiting(s))
                    delay(1_000)
                }
            }
        }
        onStatus(RetryStatus.Retrying)
    }
}

/** "Retrying in 10 seconds…" */
fun retryingIn(seconds: Int): String = if (seconds == 1) "Retrying in 1 second…" else "Retrying in $seconds seconds…"
