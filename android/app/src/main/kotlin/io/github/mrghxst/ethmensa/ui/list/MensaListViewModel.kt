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
data class MensaRowUi(val mensa: Mensa, val state: OpeningState?, val hoursToday: String?)

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
        if (store.state.value.results.isEmpty() && !store.state.value.loading) store.refresh()
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
            ),
        )
        val weekday = now.dayOfWeek.value
        val listWeekday = filters.weekdayOverride ?: weekday
        val rows = list?.map { mensa ->
            MensaRowUi(
                mensa = mensa,
                state = if (load.allProvidersLoaded || mensa.mealTimes.isNotEmpty()) openingState(mensa, now) else null,
                hoursToday = openingHoursText(mensa.mealTimesOn(listWeekday)).ifEmpty { null },
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

    fun setCellType(type: MensaCellType) {
        viewModelScope.launch { graph.settings.update { it.copy(mensaCellType = type) } }
    }

    private fun applyFilters(filters: ListFilters) {
        graph.listSession.setWeekdayOverride(filters.weekdayOverride)
        graph.listSession.setAllergenFriendly(filters.allergenFriendly)
        viewModelScope.launch {
            graph.settings.update { it.copy(mensaShowType = filters.showType, campus = filters.campus, sortBy = filters.sortBy) }
        }
    }

}
