// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mrghxst.ethmensa.AppGraph
import io.github.mrghxst.ethmensa.data.LoadState
import io.github.mrghxst.ethmensa.data.SessionFilters
import io.github.mrghxst.ethmensa.domain.ActiveFilter
import io.github.mrghxst.ethmensa.domain.ListFilters
import io.github.mrghxst.ethmensa.domain.OpeningState
import io.github.mrghxst.ethmensa.domain.PipelineInput
import io.github.mrghxst.ethmensa.domain.mensaList
import io.github.mrghxst.ethmensa.domain.nowInZurich
import io.github.mrghxst.ethmensa.domain.openingHoursText
import io.github.mrghxst.ethmensa.domain.openingState
import io.github.mrghxst.ethmensa.domain.model.Campus
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.model.MensaCellType
import io.github.mrghxst.ethmensa.ui.components.MealLine
import io.github.mrghxst.ethmensa.domain.MealText
import io.github.mrghxst.ethmensa.domain.formatPrice
import io.github.mrghxst.ethmensa.domain.fits
import io.github.mrghxst.ethmensa.domain.model.DietFilter
import io.github.mrghxst.ethmensa.domain.model.MensaShowType
import io.github.mrghxst.ethmensa.domain.model.Settings
import io.github.mrghxst.ethmensa.domain.model.SortBy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers

@Immutable
data class MensaRowUi(
    val mensa: Mensa,
    val state: OpeningState?,
    val hoursToday: String?,
    val favorite: Boolean = false,
    /** The day's meals, filled only for the EXPANDED cell style. */
    val meals: List<MealLine> = emptyList(),
)


@Immutable
data class MensaListUiState(
    /** Null = still loading, show the skeleton. */
    val rows: List<MensaRowUi>? = null,
    val filters: ListFilters = ListFilters(),
    val cellType: MensaCellType = MensaCellType.STANDARD,
    val query: String = "",
    val today: Int = 1,
    val userHasAllergens: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    /** Something is shown, even if it's from the cache. */
    val hasData: Boolean = false,
    /** Nothing to show and nothing coming: show the full-screen offline/error state. */
    val showUnavailable: Boolean = false,
    val slow: Boolean = false,
)

class MensaListViewModel(private val graph: AppGraph) : ViewModel() {
    private val store = graph.mensaStore
    private val query = MutableStateFlow("")
    private val userRefreshing = MutableStateFlow(false)

    private val ticker: Flow<LocalDateTime> = flow {
        while (true) {
            emit(nowInZurich(graph.clock))
            delay(30_000)
        }
    }

    val events = store.events

    private data class Inputs(
        val load: LoadState,
        val settings: Settings,
        val clicks: Map<String, Int>,
        val session: SessionFilters,
        val query: String,
    )

    private val inputs = combine(store.state, graph.settings.settings, graph.clickCounts.counts, graph.listSession.state, query) { load, settings, clicks, session, q ->
        Inputs(load, settings, clicks, session, q)
    }

    val uiState: StateFlow<MensaListUiState> =
        combine(inputs, ticker, graph.connectivity.online, userRefreshing) { input, now, online, refreshing ->
            buildState(input, now, online, refreshing)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MensaListUiState())

    init {
        // The screen reloads on every ON_START, including the first one.
        viewModelScope.launch {
            store.state.collect { if (!it.loading) userRefreshing.value = false }
        }
        // Coming back online reloads when there is nothing to show yet.
        viewModelScope.launch {
            var wasOnline = true
            graph.connectivity.online.collect { online ->
                val state = store.state.value
                if (online && !wasOnline && (state.mensas.isNullOrEmpty() || state.failedProviders.isNotEmpty())) store.refresh()
                wasOnline = online
            }
        }
    }

    private fun buildState(input: Inputs, now: LocalDateTime, online: Boolean, refreshing: Boolean): MensaListUiState {
        val (load, settings, clicks, session, q) = input
        val filters = ListFilters(
            showType = settings.mensaShowType,
            campus = settings.campus,
            weekdayOverride = session.weekdayOverride,
            allergenFriendly = session.allergenFriendly && settings.allergens.isNotEmpty(),
            sortBy = settings.sortBy,
            diet = settings.diet,
        )
        val list = mensaList(
            PipelineInput(
                mensas = load.mensas,
                allProvidersLoaded = load.allProvidersLoaded,
                query = q,
                filters = filters,
                hideMensaWithNoMenus = settings.hideMensaWithNoMenus,
                userAllergens = settings.allergens,
                clickCounts = clicks,
                campuses = load.campuses,
                now = now,
                favorites = settings.favorites,
                hidden = settings.hidden,
            ),
        )
        val weekday = now.dayOfWeek.value
        val listWeekday = filters.weekdayOverride ?: weekday
        val rows = list?.map { mensa ->
            MensaRowUi(
                mensa = mensa,
                state = if (load.allProvidersLoaded || mensa.mealTimes.isNotEmpty()) openingState(mensa, now) else null,
                hoursToday = openingHoursText(mensa.mealTimesOn(listWeekday)).ifEmpty { null },
                favorite = mensa.id in settings.favorites,
                meals = if (settings.mensaCellType == MensaCellType.EXPANDED) mealLines(mensa, listWeekday, settings) else emptyList(),
            )
        }
        val hasData = !load.mensas.isNullOrEmpty()
        return MensaListUiState(
            rows = rows,
            filters = filters,
            cellType = settings.mensaCellType,
            query = q,
            today = weekday,
            userHasAllergens = settings.allergens.isNotEmpty(),
            isRefreshing = refreshing,
            isOffline = !online,
            hasData = hasData,
            showUnavailable = !hasData && load.allProvidersUpToDate,
            slow = load.slow && hasData,
        )
    }

    fun onQueryChange(value: String) = query.update { value }

    fun refresh(fromUser: Boolean = false) {
        if (fromUser) userRefreshing.value = true
        store.refresh()
    }

    fun cancelRefresh() = store.cancel()

    fun removeFilter(filter: ActiveFilter) = applyFilters(uiState.value.filters.without(filter))

    fun resetFilters() = applyFilters(ListFilters())

    fun resetFiltersAndSearch() {
        query.value = ""
        resetFilters()
    }

    fun setOpenOnly(on: Boolean) = applyFilters(uiState.value.filters.copy(showType = if (on) MensaShowType.OPEN else MensaShowType.ALL))

    /** Single choice; choosing the selected campus again clears it. */
    fun toggleCampus(campus: Campus) {
        val current = uiState.value.filters
        applyFilters(current.copy(campus = if (current.campus == campus) null else campus))
    }

    fun setWeekday(weekday: Int) {
        val state = uiState.value
        applyFilters(state.filters.withWeekday(weekday, state.today))
    }

    fun setAllergenFriendly(on: Boolean) = applyFilters(uiState.value.filters.copy(allergenFriendly = on))

    fun setSortBy(sortBy: SortBy) = applyFilters(uiState.value.filters.copy(sortBy = sortBy))

    fun setDiet(diet: DietFilter) = applyFilters(uiState.value.filters.copy(diet = diet))

    fun toggleFavorite(mensaId: String) {
        viewModelScope.launch {
            graph.settings.update { s -> s.copy(favorites = if (mensaId in s.favorites) s.favorites - mensaId else s.favorites + mensaId) }
        }
    }

    fun setHidden(mensaId: String, hidden: Boolean) {
        viewModelScope.launch {
            graph.settings.update { s -> s.copy(hidden = if (hidden) s.hidden + mensaId else s.hidden - mensaId) }
        }
    }

    fun setCellType(type: MensaCellType) {
        viewModelScope.launch { graph.settings.update { it.copy(mensaCellType = type) } }
    }

    private fun applyFilters(filters: ListFilters) {
        viewModelScope.launch { graph.listSession.apply(filters) }
    }

}

/** The meals of [weekday] that fit the diet, for the EXPANDED cell style. */
private fun mealLines(mensa: Mensa, weekday: Int, settings: Settings): List<MealLine> =
    mensa.mealTimesOn(weekday).flatMap { it.meals }
        .filter { it.fits(settings.diet) }
        .mapNotNull { meal ->
            val name = meal.name ?: meal.title?.let(MealText::displayTitle) ?: return@mapNotNull null
            MealLine(name, formatPrice(meal.price, settings.priceType))
        }
        // The same dish at lunch and dinner is listed once.
        .distinct()

