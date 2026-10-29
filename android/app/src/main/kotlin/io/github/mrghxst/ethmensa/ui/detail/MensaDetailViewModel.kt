// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.detail

import androidx.compose.runtime.Immutable
import io.github.mrghxst.ethmensa.domain.fits
import io.github.mrghxst.ethmensa.domain.model.DietFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mrghxst.ethmensa.AppGraph
import io.github.mrghxst.ethmensa.domain.allergenCompatibleMeals
import io.github.mrghxst.ethmensa.domain.openingHoursText
import io.github.mrghxst.ethmensa.domain.todayWeekday
import io.github.mrghxst.ethmensa.domain.model.LatLng
import io.github.mrghxst.ethmensa.domain.model.Meal
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.model.PriceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class MealSection(
    val title: String?,
    val meals: List<Meal>,
    val hiddenByAllergens: Boolean,
    /** Every allergen-friendly meal was left out by the diet filter. */
    val hiddenByDiet: Boolean = false,
)

@Immutable
data class MensaDetailUiState(
    val mensa: Mensa? = null,
    /** All providers are up to date and the id is still unknown. */
    val notFound: Boolean = false,
    val today: Int = 1,
    val selectedWeekday: Int = 1,
    /** The list has a weekday override, so the picker is locked (iOS behavior). */
    val weekdayLocked: Boolean = false,
    val allergenFriendly: Boolean = false,
    val diet: DietFilter = DietFilter.NONE,
    val hours: String = "",
    val sections: List<MealSection> = emptyList(),
    val location: LatLng? = null,
    val priceType: PriceType = PriceType.ALL,
    val showAllergens: Boolean = false,
    val refreshing: Boolean = false,
)

class MensaDetailViewModel(private val graph: AppGraph, private val mensaId: String) : ViewModel() {
    private val today = todayWeekday(graph.clock)

    /** Opening a detail resets the day to today unless the list has an override. */
    private val selectedDay = MutableStateFlow(graph.listSession.state.value.weekdayOverride ?: today)
    private val location = MutableStateFlow<LatLng?>(null)
    private val userRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<MensaDetailUiState> = combine(
        graph.mensaStore.state,
        graph.settings.settings,
        graph.listSession.state,
        selectedDay,
        combine(location, userRefreshing) { a, b -> a to b },
    ) { load, settings, session, day, (point, refreshing) ->
        val mensa = load.mensa(mensaId)
        val weekday = session.weekdayOverride ?: day
        val friendly = session.allergenFriendly && settings.allergens.isNotEmpty()
        val mealTimes = mensa?.mealTimesOn(weekday).orEmpty()
        MensaDetailUiState(
            mensa = mensa,
            notFound = mensa == null && load.allProvidersUpToDate,
            today = today,
            selectedWeekday = weekday,
            weekdayLocked = session.weekdayOverride != null,
            allergenFriendly = friendly,
            diet = settings.diet,
            hours = openingHoursText(mealTimes),
            sections = mealTimes.map { mt ->
                val safe = if (friendly) allergenCompatibleMeals(mt, settings.allergens) else mt.meals
                val meals = safe.filter { it.fits(settings.diet) }
                MealSection(
                    mt.type,
                    meals,
                    hiddenByAllergens = friendly && safe.isEmpty(),
                    hiddenByDiet = safe.isNotEmpty() && meals.isEmpty(),
                )
            },
            location = point,
            priceType = settings.priceType,
            showAllergens = settings.showAllergens,
            refreshing = refreshing && load.loading,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MensaDetailUiState(today = today, selectedWeekday = selectedDay.value))

    init {
        viewModelScope.launch { graph.clickCounts.increment(mensaId) }
        viewModelScope.launch {
            graph.mensaStore.state.collect { load ->
                if (location.value == null) load.mensa(mensaId)?.let { location.value = graph.locator.locate(it) }
                if (!load.loading) userRefreshing.value = false
            }
        }
    }

    fun selectWeekday(day: Int) {
        selectedDay.value = day
    }

    fun refresh() {
        userRefreshing.value = true
        graph.mensaStore.refresh()
    }
}
