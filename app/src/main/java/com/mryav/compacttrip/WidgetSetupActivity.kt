package com.mryav.compacttrip

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mryav.compacttrip.ui.WidgetSetupApp
import com.mryav.compacttrip.ui.theme.CompactTripsTheme
import com.mryav.compacttrip.widget.FixedCardWidget
import com.mryav.compacttrip.widget.HeadlessListWidget
import com.mryav.compacttrip.widget.WidgetStyle

/**
 * Opened by the launcher when a CompactTrips widget is placed. Leaving without Done cancels it (the
 * launcher removes the widget). Which of the three widgets was picked sets the style.
 */
class WidgetSetupActivity : KeypadActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val id = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Until Done, backing out cancels the widget.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val container = (application as CompactTripsApplication).container
        if (!container.keyStore.hasKey) {
            showToast("Open CompactTrips and add your API key first")
            finish()
            return
        }
        val style = when (AppWidgetManager.getInstance(this).getAppWidgetInfo(id)?.provider?.className) {
            HeadlessListWidget::class.java.name -> WidgetStyle.Headless
            FixedCardWidget::class.java.name -> WidgetStyle.FixedCard
            else -> WidgetStyle.DefaultList
        }
        setContent {
            CompactTripsTheme {
                WidgetSetupApp(
                    container = container,
                    style = style,
                    onCancel = { finish() },
                    onDone = { config, snapshot ->
                        container.widgetStore.saveConfig(id, config)
                        if (snapshot != null) container.widgetStore.saveSnapshot(id, snapshot)
                        container.widgets.render(intArrayOf(id))
                        container.widgets.scheduleTick()
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                        finish()
                    },
                )
            }
        }
    }
}
