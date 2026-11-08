package com.mryav.compacttrip

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mryav.compacttrip.ui.screens.HomeScreen
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
        setContent {
            CompactTripsTheme {
                HomeScreen(onAction = { showToast(it.label) })
            }
        }
    }

    /** Placeholder feedback until screens exist. */
    private fun showToast(message: String) {
        toast?.cancel()
        toast = Toast.makeText(this, message, Toast.LENGTH_SHORT).also { it.show() }
    }
}
