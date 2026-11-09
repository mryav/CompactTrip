package com.mryav.compacttrip

import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity

/** Shared by the app and widget setup: keypad Back behaviour and toasts. */
abstract class KeypadActivity : ComponentActivity() {

    private var toast: Toast? = null

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

    protected fun showToast(message: String) {
        toast?.cancel()
        toast = Toast.makeText(this, message, Toast.LENGTH_LONG).also { it.show() }
    }
}
