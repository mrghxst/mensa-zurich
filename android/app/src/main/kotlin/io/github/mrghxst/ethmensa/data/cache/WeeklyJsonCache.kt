// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.cache

import android.util.Log
import io.github.mrghxst.ethmensa.data.net.ApiJson
import io.github.mrghxst.ethmensa.domain.isoWeekKey
import io.github.mrghxst.ethmensa.domain.today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.time.Clock

/**
 * One JSON file per API response (APICache.swift). An entry is valid in the same ISO week
 * (Europe/Zurich) and language it was saved for. The week is part of the entry so a
 * "next week" view can use its own file later.
 */
class WeeklyJsonCache(
    private val dir: File,
    private val name: String,
    private val clock: Clock,
) {
    @Serializable
    private data class Entry(val savedAtEpochMs: Long, val language: String, val week: String, val payload: String)

    private fun file(): File = File(dir, "$name.json")

    /** The raw payload if it is from this week and [language], else null. */
    suspend fun read(language: String): String? = withContext(Dispatchers.IO) {
        val file = file()
        if (!file.exists()) return@withContext null
        val entry = runCatching { ApiJson.decodeFromString<Entry>(file.readText()) }.getOrNull()
            ?: return@withContext null
        entry.payload.takeIf { entry.language == language && entry.week == isoWeekKey(today(clock)) }
    }

    suspend fun write(language: String, payload: String) = withContext(Dispatchers.IO) {
        val entry = Entry(clock.millis(), language, isoWeekKey(today(clock)), payload)
        runCatching {
            dir.mkdirs()
            val tmp = File(dir, "$name.json.tmp")
            tmp.writeText(ApiJson.encodeToString(Entry.serializer(), entry))
            if (!tmp.renameTo(file())) {
                file().delete()
                tmp.renameTo(file())
            }
        }.onFailure { Log.w(TAG, "Could not write cache $name", it) }
        Unit
    }

    suspend fun clear() = withContext(Dispatchers.IO) { file().delete(); Unit }

    private companion object {
        const val TAG = "WeeklyJsonCache"
    }
}
