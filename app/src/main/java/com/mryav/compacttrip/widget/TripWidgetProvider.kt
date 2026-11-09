package com.mryav.compacttrip.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.mryav.compacttrip.CompactTripsApplication

/**
 * Base for the three widgets. Each style is its own entry in the widget picker with its own sizes
 * and resize limits (res/xml/widget_*_info.xml); the style a widget gets comes from which one was
 * picked. Drawing and updating live in [WidgetUpdater].
 */
abstract class TripWidgetProvider(val style: WidgetStyle) : AppWidgetProvider() {

    private fun updater(context: Context) = (context.applicationContext as CompactTripsApplication).container.widgets

    /** Placed, after a reboot or app update, or resized: redraw from stored data and keep the tick going. */
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val updater = updater(context)
        updater.render(ids)
        updater.scheduleTick()
    }

    /** Resized: narrow and wide layouts differ. */
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        updater(context).render(intArrayOf(id))
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        updater(context).forget(ids)
    }

    override fun onDisabled(context: Context) {
        val updater = updater(context)
        if (updater.allIds().isEmpty()) updater.cancelTick()
    }

    /** Restored from a backup: the launcher hands out new ids for the same widgets. */
    override fun onRestored(context: Context, oldIds: IntArray, newIds: IntArray) {
        (context.applicationContext as CompactTripsApplication).container.widgetStore.remap(oldIds, newIds)
    }
}

class DefaultListWidget : TripWidgetProvider(WidgetStyle.DefaultList)
class HeadlessListWidget : TripWidgetProvider(WidgetStyle.Headless)
class FixedCardWidget : TripWidgetProvider(WidgetStyle.FixedCard)
