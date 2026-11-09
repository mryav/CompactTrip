package com.mryav.compacttrip.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.mryav.compacttrip.CompactTripsApplication
import com.mryav.compacttrip.R

/** Rows for the scrolling list widgets, read from stored data each time the widget redraws. */
class WidgetListService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Rows(
        context = applicationContext,
        id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID),
        wide = intent.getBooleanExtra(EXTRA_WIDE, true),
    )

    private class Rows(private val context: Context, private val id: Int, private val wide: Boolean) : RemoteViewsFactory {
        private val store = (context.applicationContext as CompactTripsApplication).container.widgetStore
        private var rows: List<WidgetRow> = emptyList()
        private var now = 0L

        override fun onCreate() = Unit

        override fun onDataSetChanged() {
            now = System.currentTimeMillis()
            rows = (store.snapshot(id).content(now) as? WidgetContent.Rows)?.rows.orEmpty()
        }

        override fun onDestroy() = Unit
        override fun getCount(): Int = rows.size

        override fun getViewAt(position: Int): RemoteViews =
            rows.getOrNull(position)?.let { WidgetRenderer.row(context, it, wide, now) } ?: loadingView

        override fun getLoadingView(): RemoteViews = RemoteViews(context.packageName, R.layout.widget_loading_row)
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = rows.getOrNull(position)?.key?.hashCode()?.toLong() ?: position.toLong()
        override fun hasStableIds(): Boolean = true
    }

    companion object {
        const val EXTRA_WIDE = "com.mryav.compacttrip.widget.WIDE"
    }
}
