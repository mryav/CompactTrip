package com.mryav.compacttrip.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mryav.compacttrip.MainActivity
import com.mryav.compacttrip.data.TfnswJson

/** Intents a widget sends: open the app on its route, refresh, and the once-a-minute redraw. */
object WidgetIntents {
    const val ACTION_REFRESH = "com.mryav.compacttrip.widget.REFRESH"
    const val ACTION_TICK = "com.mryav.compacttrip.widget.TICK"
    const val ACTION_OPEN_ROUTE = "com.mryav.compacttrip.OPEN_ROUTE"
    private const val EXTRA_ROUTE = "com.mryav.compacttrip.ROUTE"

    /**
     * Opens the app on this widget's route: the list of services, not one service (widget data may
     * be out of date). Starts a fresh task so Back from there goes to Home.
     * [forList]: the list's click template, which must be mutable so rows can fill it in.
     */
    fun openApp(context: Context, id: Int, config: WidgetConfig, forList: Boolean = false): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_ROUTE)
            // Unique per widget (and per template), so each keeps its own extras.
            .setData(Uri.parse("compacttrip://widget/$id" + if (forList) "/list" else ""))
            .putExtra(EXTRA_ROUTE, TfnswJson.encodeToString(WidgetConfig.serializer(), config))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val mutability = if (forList) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or mutability)
    }

    /** The route a widget asked the app to open, if any. */
    fun route(intent: Intent?): WidgetConfig? {
        if (intent?.action != ACTION_OPEN_ROUTE) return null
        val raw = intent.getStringExtra(EXTRA_ROUTE) ?: return null
        return runCatching { TfnswJson.decodeFromString(WidgetConfig.serializer(), raw) }.getOrNull()
    }

    fun refresh(context: Context, id: Int): PendingIntent {
        val intent = Intent(context, WidgetReceiver::class.java)
            .setAction(ACTION_REFRESH)
            .setData(Uri.parse("compacttrip://widget/$id/refresh"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun tick(context: Context): PendingIntent {
        val intent = Intent(context, WidgetReceiver::class.java).setAction(ACTION_TICK)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** The scrolling list's adapter. The data URI keeps each widget (and width) its own adapter. */
    fun listService(context: Context, id: Int, wide: Boolean): Intent =
        Intent(context, WidgetListService::class.java)
            .setData(Uri.parse("compacttrip://widget/$id/rows/" + if (wide) "wide" else "narrow"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(WidgetListService.EXTRA_WIDE, wide)
}
