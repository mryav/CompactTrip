package com.mryav.compacttrip.widget

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.ApiResult
import com.mryav.compacttrip.data.LoadError
import com.mryav.compacttrip.data.TripRepository
import com.mryav.compacttrip.data.model.Stop
import com.mryav.compacttrip.data.model.Timing
import com.mryav.compacttrip.data.model.TripPreferences
import com.mryav.compacttrip.data.model.TripSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.Collections

/**
 * Keeps widgets current while costing as little battery as possible:
 * - **Minute tick**: a non-wakeup alarm redraws every widget from stored data (countdowns, departed
 *   services dropping off, "Updated N min ago"). No network. It never wakes the phone: while
 *   the screen is off it waits, and fires when the screen comes back on.
 * - **Auto refresh**: on a tick, widgets with data older than [AUTO_REFRESH_MS] are reloaded, but
 *   only while the screen is on, battery saver is off and there's a connection. So nothing is
 *   fetched while the phone is in a pocket, and a glance after unlocking gets fresh data.
 * - **Manual**: the refresh button or "Tap here to load updates".
 * - **App push**: whenever the app loads a route a widget shows, that widget is updated for free
 *   (see [onTripLoaded]).
 */
class WidgetUpdater(
    context: Context,
    private val store: WidgetStore,
    private val trips: TripRepository,
) {
    private val app = context.applicationContext
    private val manager by lazy { AppWidgetManager.getInstance(app) }
    private val refreshing: MutableSet<Int> = Collections.synchronizedSet(HashSet())
    private val refreshLock = Mutex()

    /** Every placed CompactTrips widget, across the three styles. */
    fun allIds(): IntArray = PROVIDERS.flatMap { manager.getAppWidgetIds(ComponentName(app, it)).asList() }.toIntArray()

    /** Redraw from stored data. Widgets still being set up (no route yet) are left alone. */
    fun render(ids: IntArray = allIds(), now: Long = System.currentTimeMillis()) {
        for (id in ids) {
            val config = store.config(id) ?: continue
            val views = WidgetRenderer.widget(
                context = app,
                id = id,
                config = config,
                snapshot = store.snapshot(id),
                now = now,
                widthDp = widthDp(id),
                refreshing = id in refreshing,
            )
            manager.updateAppWidget(id, views)
            if (config.style != WidgetStyle.FixedCard) manager.notifyAppWidgetViewDataChanged(id, R.id.list)
        }
    }

    /**
     * Reload [ids] from the network. Results are saved by [onTripLoaded] (every load reports there);
     * failures keep the old rows and record why.
     */
    suspend fun refresh(ids: IntArray) {
        val targets = ids.filter { store.config(it) != null }
        if (targets.isEmpty()) return
        refreshing += targets
        render(targets.toIntArray())
        try {
            refreshLock.withLock {
                val gate = Semaphore(MAX_PARALLEL)
                coroutineScope {
                    targets.map { id ->
                        async {
                            val config = store.config(id) ?: return@async
                            val result = gate.withPermit { trips.upcoming(config.origin, config.destination, config.preferences) }
                            val error = when (result) {
                                is ApiResult.Ok -> null
                                ApiResult.Offline -> LoadError.Offline
                                ApiResult.Unauthorized, ApiResult.NoKey -> LoadError.KeyProblem
                                ApiResult.ServiceUnavailable -> LoadError.Unavailable
                            }
                            if (error != null) store.saveSnapshot(id, store.snapshot(id).withError(error))
                        }
                    }.awaitAll()
                }
            }
        } finally {
            refreshing -= targets.toSet()
            render(targets.toIntArray())
        }
    }

    /** The app (or a widget refresh) loaded a route: update every widget showing it. */
    fun onTripLoaded(origin: Stop, destination: Stop, preferences: TripPreferences, summary: TripSummary) {
        // A planned trip for later isn't what a widget shows.
        if (preferences.timing != Timing.Now) return
        val ids = store.ids().filter { id -> store.config(id)?.matches(origin, destination, preferences) == true }
        if (ids.isEmpty()) return
        val now = System.currentTimeMillis()
        val snapshot = summary.toSnapshot(now)
        ids.forEach { store.saveSnapshot(it, snapshot) }
        render(ids.toIntArray(), now)
    }

    /** Once a minute while the screen is on: redraw, and reload what's gone stale when it's cheap to. */
    suspend fun onTick() {
        val ids = allIds()
        if (ids.isEmpty()) return cancelTick()
        scheduleTick()
        val now = System.currentTimeMillis()
        render(ids, now)
        if (!autoRefreshAllowed(now)) return
        val stale = ids.filter { id ->
            val fetchedAt = store.snapshot(id)?.fetchedAt
            fetchedAt == null || now - fetchedAt >= AUTO_REFRESH_MS
        }
        if (stale.isEmpty()) return
        store.lastAutoRefresh = now
        refresh(stale.toIntArray())
    }

    /** Next redraw at the start of the next minute. Non-wakeup: never wakes the phone. */
    fun scheduleTick() {
        val alarms = app.getSystemService(AlarmManager::class.java) ?: return
        val next = (System.currentTimeMillis() / 60_000 + 1) * 60_000 + TICK_OFFSET_MS
        val pending = WidgetIntents.tick(app)
        // Exact non-wakeup alarms need no permission on Android 11; newer phones may ask for it,
        // and without it get a looser window.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExact(AlarmManager.RTC, next, pending)
        } else {
            alarms.setWindow(AlarmManager.RTC, next, TICK_WINDOW_MS, pending)
        }
    }

    fun cancelTick() {
        app.getSystemService(AlarmManager::class.java)?.cancel(WidgetIntents.tick(app))
    }

    fun forget(ids: IntArray) = ids.forEach { store.delete(it) }

    private fun autoRefreshAllowed(now: Long): Boolean {
        val power = app.getSystemService(PowerManager::class.java) ?: return false
        if (!power.isInteractive || power.isPowerSaveMode) return false
        if (now - store.lastAutoRefresh < AUTO_RETRY_MS) return false
        val connectivity = app.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** The widget's current width (portrait), for picking narrow or wide layouts. 0 = unknown → wide. */
    private fun widthDp(id: Int): Int =
        manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: Int.MAX_VALUE

    companion object {
        /** Reload data older than this ("updates every 15-30 minutes"). */
        const val AUTO_REFRESH_MS = 15 * 60_000L
        /** After an automatic reload (failed or not), wait at least this long before the next. */
        const val AUTO_RETRY_MS = 5 * 60_000L
        private const val TICK_OFFSET_MS = 1_000L
        private const val TICK_WINDOW_MS = 15_000L
        /** TfNSW allows ~5 requests/s; stay well under. */
        private const val MAX_PARALLEL = 3
        val PROVIDERS = listOf(DefaultListWidget::class.java, HeadlessListWidget::class.java, FixedCardWidget::class.java)
    }
}
