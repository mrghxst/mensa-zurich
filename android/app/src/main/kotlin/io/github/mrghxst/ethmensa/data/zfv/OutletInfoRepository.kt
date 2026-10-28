// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.zfv

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.mrghxst.ethmensa.data.net.ApiJson
import io.github.mrghxst.ethmensa.data.net.fetchText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Clock
import java.time.Duration

/**
 * ZFV outlet id → UZH id, name, address and hours. Downloaded from the upstream repository at
 * most every 72 h. Falls back to the last download, then the bundled asset, then empty.
 */
class OutletInfoRepository(
    private val client: OkHttpClient,
    private val store: DataStore<Preferences>,
    private val bundled: () -> String?,
    private val clock: Clock,
    private val url: String = UPSTREAM_URL,
) {
    private val mutex = Mutex()
    private var memory: Map<String, OutletInfo>? = null

    /** Cached infos without any network access. */
    suspend fun cached(): Map<String, OutletInfo> = mutex.withLock {
        memory ?: (stored()?.let(::parse) ?: bundledInfos()).also { memory = it }
    }

    /** Refreshes the mapping if it is older than 72 h, then returns the best available one. */
    suspend fun load(): Map<String, OutletInfo> = mutex.withLock {
        val prefs = store.data.first()
        val savedAt = prefs[SAVED_AT] ?: 0L
        val fresh = clock.millis() - savedAt < REFRESH.toMillis()
        val downloaded = prefs[JSON]?.let(::parse)
        if (fresh && downloaded != null) return@withLock downloaded.also { memory = it }
        val result = runCatching {
            val json = client.fetchText(Request.Builder().url(url).build())
            val parsed = parse(json) ?: error("Invalid mapping JSON")
            store.edit {
                it[JSON] = json
                it[SAVED_AT] = clock.millis()
            }
            parsed
        }.onFailure { Log.w(TAG, "Mapping refresh failed", it) }.getOrNull()
        (result ?: downloaded ?: bundledInfos()).also { memory = it }
    }

    private suspend fun stored(): String? = store.data.first()[JSON]

    private fun bundledInfos(): Map<String, OutletInfo> = bundled()?.let(::parse) ?: emptyMap()

    private fun parse(json: String): Map<String, OutletInfo>? =
        runCatching { ApiJson.decodeFromString<Map<String, OutletInfo>>(json) }.getOrNull()

    companion object {
        const val UPSTREAM_URL =
            "https://raw.githubusercontent.com/alexandrereol/ethmensa/refs/heads/main/ethmensa/Data/API/ZFV/zfv_uzh_mapping.json"
        private const val TAG = "OutletInfo"
        private val REFRESH: Duration = Duration.ofHours(72)
        private val JSON = stringPreferencesKey("mapping_json")
        private val SAVED_AT = longPreferencesKey("mapping_saved_at")
    }
}
