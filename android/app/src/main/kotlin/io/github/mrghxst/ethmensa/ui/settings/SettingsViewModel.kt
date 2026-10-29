// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mrghxst.ethmensa.AppGraph
import io.github.mrghxst.ethmensa.domain.model.KnownAllergen
import io.github.mrghxst.ethmensa.domain.model.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val graph: AppGraph) : ViewModel() {
    val settings: StateFlow<Settings> = graph.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    /** Mensa names by id, for the favorites and hidden lists. */
    val mensaNames: StateFlow<Map<String, String>> = graph.mensaStore.state
        .map { state -> state.mensas.orEmpty().associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setFavorite(mensaId: String, on: Boolean) = update {
        it.copy(favorites = if (on) it.favorites + mensaId else it.favorites - mensaId)
    }

    fun setHidden(mensaId: String, on: Boolean) = update {
        it.copy(hidden = if (on) it.hidden + mensaId else it.hidden - mensaId)
    }

    fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { graph.settings.update(transform) }
    }

    fun toggleAllergen(allergen: KnownAllergen, on: Boolean) = update {
        it.copy(allergens = if (on) it.allergens + allergen else it.allergens - allergen)
    }

    fun resetSmartSorting() {
        viewModelScope.launch { graph.clickCounts.reset() }
    }

    fun resetMapsCache() {
        viewModelScope.launch {
            graph.locator.clearCache()
            graph.mensaStore.invalidateCampuses()
        }
    }

    /** Back to defaults; onboarding stays completed. */
    fun resetSettings() {
        viewModelScope.launch {
            graph.settings.reset()
            graph.settings.update { it.copy(completedOnboarding = true) }
            graph.listSession.reset()
        }
    }

    /** Settings, smart sorting, the maps cache and the cached menus. */
    fun resetSettingsAndData() {
        viewModelScope.launch {
            resetSettings()
            graph.clickCounts.reset()
            graph.locator.clearCache()
            graph.mensaRepository.clearCaches()
            graph.mensaStore.refresh()
        }
    }
}
