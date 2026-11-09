package com.mryav.compacttrip

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mryav.compacttrip.ui.CompactTripsApp
import com.mryav.compacttrip.ui.theme.CompactTripsTheme

class MainActivity : ComponentActivity() {

    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light icons over the dark app bar.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val container = (application as CompactTripsApplication).container
        setContent {
            CompactTripsTheme {
                CompactTripsApp(
                    container = container,
                    openUrl = ::openUrl,
                    showMessage = ::showToast,
                )
            }
        }
    }

    /**
     * Keypad Back always means "go back". By default Compose first uses Back to move focus out of a
     * nested focus group (e.g. a trip's notices inside its section), which takes two presses. The
     * on-screen keyboard still gets Back first (it handles keys before the activity does).
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            // Some keypad phones ship without a browser.
            showToast("No browser found. Visit $url")
        }
    }

    private fun showToast(message: String) {
        toast?.cancel()
        toast = Toast.makeText(this, message, Toast.LENGTH_LONG).also { it.show() }
    }
}
