package com.mryav.compacttrip.widget

import android.content.Context
import com.mryav.compacttrip.data.TfnswJson
import kotlinx.serialization.KSerializer

/**
 * Widget routes and their last data, per appWidgetId. Private SharedPreferences with commit()
 * writes, like the other stores; configs are in backup (res/xml backup rules), and restored
 * widgets get their new ids via [remap].
 */
class WidgetStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun config(id: Int): WidgetConfig? = read(CONFIG + id, WidgetConfig.serializer())

    fun saveConfig(id: Int, config: WidgetConfig) = write(CONFIG + id, WidgetConfig.serializer(), config)

    fun snapshot(id: Int): WidgetSnapshot? = read(SNAPSHOT + id, WidgetSnapshot.serializer())

    fun saveSnapshot(id: Int, snapshot: WidgetSnapshot) = write(SNAPSHOT + id, WidgetSnapshot.serializer(), snapshot)

    /** When widgets last reloaded on their own (manual refreshes don't count). */
    var lastAutoRefresh: Long
        get() = prefs.getLong(LAST_AUTO_REFRESH, 0L)
        set(value) {
            prefs.edit().putLong(LAST_AUTO_REFRESH, value).commit()
        }

    /** Ids with a saved route. */
    fun ids(): List<Int> = prefs.all.keys.filter { it.startsWith(CONFIG) }.mapNotNull { it.removePrefix(CONFIG).toIntOrNull() }

    fun delete(id: Int) {
        prefs.edit().remove(CONFIG + id).remove(SNAPSHOT + id).commit()
    }

    /** After a backup restore the launcher gives restored widgets new ids. */
    fun remap(oldIds: IntArray, newIds: IntArray) {
        val configs = oldIds.map { config(it) }
        oldIds.forEach { delete(it) }
        oldIds.indices.forEach { i -> configs[i]?.let { saveConfig(newIds[i], it) } }
    }

    private fun <T> read(key: String, serializer: KSerializer<T>): T? =
        prefs.getString(key, null)?.let { raw -> runCatching { TfnswJson.decodeFromString(serializer, raw) }.getOrNull() }

    private fun <T> write(key: String, serializer: KSerializer<T>, value: T) {
        prefs.edit().putString(key, TfnswJson.encodeToString(serializer, value)).commit()
    }

    private companion object {
        // Referenced by res/xml backup_rules.xml and data_extraction_rules.xml. Keep in sync.
        const val FILE_NAME = "widgets"
        const val CONFIG = "config_"
        const val SNAPSHOT = "snapshot_"
        const val LAST_AUTO_REFRESH = "last_auto_refresh"
    }
}
