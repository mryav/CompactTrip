package com.mryav.compacttrip

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mryav.compacttrip.data.ApiKeyChecker
import com.mryav.compacttrip.data.ApiKeyStore
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
        val keyStore = ApiKeyStore(this)
        val keyChecker = ApiKeyChecker(this)
        setContent {
            CompactTripsTheme {
                CompactTripsApp(
                    keyStore = keyStore,
                    keyChecker = keyChecker,
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

    private fun showToast(message: String) {
        toast?.cancel()
        toast = Toast.makeText(this, message, Toast.LENGTH_LONG).also { it.show() }
    }
}
