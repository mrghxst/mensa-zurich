// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data

import io.github.mrghxst.ethmensa.data.prefs.SettingsRepository
import io.github.mrghxst.ethmensa.domain.ListFilters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Session-only filter state (§6.8): the weekday override and the allergen-friendly flag. */
data class SessionFilters(val weekdayOverride: Int? = null, val allergenFriendly: Boolean = false)

class ListSession(private val settings: SettingsRepository, scope: CoroutineScope) {
    private val _state = MutableStateFlow(SessionFilters())
    val state: StateFlow<SessionFilters> = _state.asStateFlow()

    init {
        // The flag starts on if the user has allergens and follows the list going empty or non-empty.
        scope.launch {
            settings.settings.map { it.allergens.isNotEmpty() }.distinctUntilChanged().collect { hasAllergens ->
                _state.update { it.copy(allergenFriendly = hasAllergens) }
            }
        }
    }

    fun setWeekdayOverride(weekday: Int?) = _state.update { it.copy(weekdayOverride = weekday) }

    fun setAllergenFriendly(on: Boolean) = _state.update { it.copy(allergenFriendly = on) }

    fun reset() = _state.update { SessionFilters() }

    /** Applies list filters: the session parts here, the persisted parts in the settings. */
    suspend fun apply(filters: ListFilters) {
        _state.update { SessionFilters(filters.weekdayOverride, filters.allergenFriendly) }
        settings.update { it.copy(mensaShowType = filters.showType, campus = filters.campus, sortBy = filters.sortBy, diet = filters.diet) }
    }
}
