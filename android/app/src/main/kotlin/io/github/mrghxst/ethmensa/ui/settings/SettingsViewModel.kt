// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mrghxst.ethmensa.AppGraph
import io.github.mrghxst.ethmensa.domain.model.KnownAllergen
import io.github.mrghxst.ethmensa.domain.model.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val graph: AppGraph) : ViewModel() {
    val settings: StateFlow<Settings> = graph.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

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
