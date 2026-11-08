package com.mryav.compacttrip.data

import android.content.Context

/**
 * Holds the user's TfNSW Open Data API key.
 *
 * Retention is deliberate. Finding the key again is a big hassle for users, so:
 * - It lives in app-private SharedPreferences (sandboxed, survives app updates and reboots).
 * - Writes use commit() (synchronous) so a just-entered key survives an immediate kill or power loss.
 * - The file is included in Android backup / device-transfer (see res/xml backup_rules and
 *   data_extraction_rules), so a restore
 *   onto a new phone or a reinstall with backup brings it back.
 * - Nothing in the app ever deletes it automatically (not even after a 401): only an explicit
 *   delete by the user via [clear].
 * - Not encrypted with the Android Keystore on purpose: Keystore keys are never backed up, which
 *   would make a restored copy unreadable. The file is already private to this app.
 */
class ApiKeyStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** The saved key, or null when none has been set up. */
    val apiKey: String?
        get() = prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }

    val hasKey: Boolean get() = apiKey != null

    /** Result of the most recent check of the saved key (for the Settings subtitle). */
    val lastCheckSucceeded: Boolean
        get() = prefs.getBoolean(KEY_LAST_CHECK_OK, false)

    fun save(key: String, checkSucceeded: Boolean) {
        prefs.edit()
            .putString(KEY_API_KEY, key)
            .putBoolean(KEY_LAST_CHECK_OK, checkSucceeded)
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .commit()
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    private companion object {
        // Referenced by res/xml/backup_rules.xml and data_extraction_rules.xml. Keep in sync.
        const val FILE_NAME = "credentials"
        const val KEY_API_KEY = "tfnsw_api_key"
        const val KEY_LAST_CHECK_OK = "last_check_ok"
        const val KEY_SAVED_AT = "saved_at"
    }
}
