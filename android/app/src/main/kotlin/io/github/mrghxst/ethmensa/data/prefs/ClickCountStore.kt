// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Per-mensa open counter for smart sorting (ClickCountDBManager.swift), keyed `click/{mensaId}`. */
class ClickCountStore(private val store: DataStore<Preferences>) {
    val counts: Flow<Map<String, Int>> = store.data.map { prefs ->
        prefs.asMap().entries
            .filter { it.key.name.startsWith(PREFIX) }
            .associate { it.key.name.removePrefix(PREFIX) to (it.value as? Int ?: 0) }
    }.distinctUntilChanged()

    suspend fun increment(mensaId: String) {
        store.edit { prefs ->
            val key = intPreferencesKey(PREFIX + mensaId)
            prefs[key] = (prefs[key] ?: 0) + 1
        }
    }

    suspend fun reset() {
        store.edit { it.clear() }
    }

    private companion object {
        const val PREFIX = "click/"
    }
}
