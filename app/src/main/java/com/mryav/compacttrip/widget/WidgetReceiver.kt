package com.mryav.compacttrip.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mryav.compacttrip.CompactTripsApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Widget refresh taps and the once-a-minute tick. Work runs in [goAsync] with a time limit well
 * inside the broadcast deadline, so no background service or job is needed.
 */
class WidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val updater = (context.applicationContext as CompactTripsApplication).container.widgets
        val pending = goAsync()
        scope.launch {
            try {
                withTimeoutOrNull(LIMIT_MS) {
                    when (intent.action) {
                        WidgetIntents.ACTION_REFRESH -> {
                            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) updater.refresh(intArrayOf(id))
                        }
                        WidgetIntents.ACTION_TICK -> updater.onTick()
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        /** Requests time out at 10 s each; background broadcasts must finish within 60 s. */
        const val LIMIT_MS = 25_000L
    }
}
