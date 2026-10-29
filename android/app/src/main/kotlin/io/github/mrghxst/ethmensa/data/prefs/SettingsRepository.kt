// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.mrghxst.ethmensa.domain.model.Campus
import io.github.mrghxst.ethmensa.domain.model.KnownAllergen
import io.github.mrghxst.ethmensa.domain.model.NotificationSound
import io.github.mrghxst.ethmensa.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime

/** All settings from SettingsManager.swift (§6.10) in one DataStore. */
class SettingsRepository(private val store: DataStore<Preferences>) {
    val settings: Flow<Settings> = store.data.map { it.toSettings() }.distinctUntilChanged()

    suspend fun current(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        store.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    /** Resets every setting to its default. */
    suspend fun reset() {
        store.edit { it.clear() }
    }

    /** Settings that aren't part of [Settings], such as one-time hints. */
    suspend fun flag(name: String): Boolean = store.data.first()[booleanPreferencesKey("flag_$name")] ?: false

    fun flagFlow(name: String): Flow<Boolean> = store.data.map { it[booleanPreferencesKey("flag_$name")] ?: false }

    suspend fun setFlag(name: String, value: Boolean) {
        store.edit { it[booleanPreferencesKey("flag_$name")] = value }
    }

    private companion object Keys {
        val COMPLETED_ONBOARDING = booleanPreferencesKey("completedOnboarding")
        val HIDE_EMPTY = booleanPreferencesKey("hideMensaWithNoMenus")
        val CELL_TYPE = stringPreferencesKey("mensaCellType")
        val PRICE_TYPE = stringPreferencesKey("priceType")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val NOTIFICATION_SOUND = stringPreferencesKey("notificationSound")
        val NOTIFICATION_MINUTES = intPreferencesKey("notificationTimeMinutes")
        val LOCATION_NOTIFICATIONS = booleanPreferencesKey("locationNotifications")
        val SORT_BY = stringPreferencesKey("sortBy")
        val SHOW_TYPE = stringPreferencesKey("mensaShowType")
        val CAMPUS = stringPreferencesKey("campus")
        val SHOW_ALLERGENS = booleanPreferencesKey("showAllergens")
        val ALLERGENS = stringSetPreferencesKey("allergens")
        val THEME_MODE = stringPreferencesKey("themeMode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamicColor")
        val DIET = stringPreferencesKey("diet")
        val FAVORITES = stringSetPreferencesKey("favorites")
        val HIDDEN = stringSetPreferencesKey("hidden")

        inline fun <reified E : Enum<E>> Preferences.enumValue(key: Preferences.Key<String>, default: E): E =
            this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

        fun Preferences.toSettings(): Settings {
            val defaults = Settings()
            return Settings(
                completedOnboarding = this[COMPLETED_ONBOARDING] ?: defaults.completedOnboarding,
                hideMensaWithNoMenus = this[HIDE_EMPTY] ?: defaults.hideMensaWithNoMenus,
                mensaCellType = enumValue(CELL_TYPE, defaults.mensaCellType),
                priceType = enumValue(PRICE_TYPE, defaults.priceType),
                notifications = this[NOTIFICATIONS] ?: defaults.notifications,
                notificationSound = NotificationSound.fromId(this[NOTIFICATION_SOUND]),
                notificationTime = this[NOTIFICATION_MINUTES]
                    ?.let { LocalTime.of((it / 60).coerceIn(0, 23), (it % 60).coerceIn(0, 59)) }
                    ?: defaults.notificationTime,
                locationNotifications = this[LOCATION_NOTIFICATIONS] ?: defaults.locationNotifications,
                sortBy = enumValue(SORT_BY, defaults.sortBy),
                mensaShowType = enumValue(SHOW_TYPE, defaults.mensaShowType),
                campus = Campus.fromId(this[CAMPUS]),
                showAllergens = this[SHOW_ALLERGENS] ?: defaults.showAllergens,
                allergens = this[ALLERGENS].orEmpty().mapNotNull { KnownAllergen.fromId(it) }.toSet(),
                themeMode = enumValue(THEME_MODE, defaults.themeMode),
                dynamicColor = this[DYNAMIC_COLOR] ?: defaults.dynamicColor,
                diet = enumValue(DIET, defaults.diet),
                favorites = this[FAVORITES].orEmpty(),
                hidden = this[HIDDEN].orEmpty(),
            )
        }

        fun MutablePreferences.write(s: Settings) {
            this[COMPLETED_ONBOARDING] = s.completedOnboarding
            this[HIDE_EMPTY] = s.hideMensaWithNoMenus
            this[CELL_TYPE] = s.mensaCellType.name
            this[PRICE_TYPE] = s.priceType.name
            this[NOTIFICATIONS] = s.notifications
            this[NOTIFICATION_SOUND] = s.notificationSound.id
            this[NOTIFICATION_MINUTES] = s.notificationTime.hour * 60 + s.notificationTime.minute
            this[LOCATION_NOTIFICATIONS] = s.locationNotifications
            this[SORT_BY] = s.sortBy.name
            this[SHOW_TYPE] = s.mensaShowType.name
            if (s.campus != null) this[CAMPUS] = s.campus.id else remove(CAMPUS)
            this[SHOW_ALLERGENS] = s.showAllergens
            this[ALLERGENS] = s.allergens.map { it.id }.toSet()
            this[THEME_MODE] = s.themeMode.name
            this[DYNAMIC_COLOR] = s.dynamicColor
            this[DIET] = s.diet.name
            this[FAVORITES] = s.favorites
            this[HIDDEN] = s.hidden
        }
    }
}

