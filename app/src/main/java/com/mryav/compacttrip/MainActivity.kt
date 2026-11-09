package com.mryav.compacttrip

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mryav.compacttrip.ui.CompactTripsApp
import com.mryav.compacttrip.ui.theme.CompactTripsTheme
import com.mryav.compacttrip.widget.WidgetIntents

class MainActivity : KeypadActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light icons over the dark app bar.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val container = (application as CompactTripsApplication).container
        // Opened from a widget: show that route's services (only on a fresh start, not a rotation).
        val widgetRoute = if (savedInstanceState == null) WidgetIntents.route(intent) else null
        setContent {
            CompactTripsTheme {
                CompactTripsApp(
                    container = container,
                    widgetRoute = widgetRoute,
                    openUrl = ::openUrl,
                    showMessage = ::showToast,
                )
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            // Some keypad phones ship without a browser.
            showToast("No browser found. Visit $url")
        }
    }
}
