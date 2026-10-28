// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data

import io.github.mrghxst.ethmensa.data.geo.MensaLocator
import io.github.mrghxst.ethmensa.domain.model.Campus
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.model.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything loaded so far. The previous provider list stays until the new one arrives (§5.6). */
data class LoadState(
    val results: Map<Provider, ProviderResult> = emptyMap(),
    val loading: Boolean = false,
    /** A reload has run longer than 8 s. */
    val slow: Boolean = false,
    /** Campus per mensa id, null until resolved for the current mensas. */
    val campuses: Map<String, Campus?>? = null,
) {
    /** ETH first, then UZH. Null while nothing at all has arrived. */
    val mensas: List<Mensa>?
        get() = if (results.isEmpty()) null else Provider.entries.flatMap { results[it]?.mensas.orEmpty() }

    /** Every provider has delivered something, cached or fresh. */
    val allProvidersLoaded: Boolean get() = Provider.entries.all { it in results }

    /** Every provider has finished its last reload. */
    val allProvidersUpToDate: Boolean get() = !loading && Provider.entries.all { results[it]?.isFinal == true }

    val failedProviders: List<Provider> get() = Provider.entries.filter { results[it]?.error != null }

    fun mensa(id: String): Mensa? = mensas?.firstOrNull { it.id == id }
}

sealed interface StoreEvent {
    /** Fresh data couldn't be loaded for these providers. */
    data class UpdateFailed(val providers: List<Provider>) : StoreEvent
}

/** App-wide mensa data, like MensaDataManager.swift. Reloads cancel the previous one. */
class MensaStore(
    private val repository: MensaRepository,
    private val locator: MensaLocator,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(LoadState())
    val state: StateFlow<LoadState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<StoreEvent>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<StoreEvent> = _events

    private var job: Job? = null
    private var campusJob: Job? = null

    fun refresh() {
        job?.cancel()
        job = scope.launch {
            _state.update { it.copy(loading = true, slow = false) }
            val slowWatch = launch {
                delay(SLOW_AFTER_MS)
                _state.update { it.copy(slow = true) }
            }
            try {
                repository.observeMensas().collect { result ->
                    _state.update { it.copy(results = it.results + (result.provider to result)) }
                    resolveCampuses()
                }
            } finally {
                slowWatch.cancel()
                _state.update { it.copy(loading = false, slow = false) }
            }
            val failed = _state.value.failedProviders
            if (failed.isNotEmpty()) _events.tryEmit(StoreEvent.UpdateFailed(failed))
        }
    }

    /** Stops a slow reload and keeps what is shown. */
    fun cancel() {
        job?.cancel()
    }

    private fun resolveCampuses() {
        val mensas = _state.value.mensas ?: return
        campusJob?.cancel()
        campusJob = scope.launch {
            val campuses = locator.campuses(mensas)
            _state.update { if (it.mensas == mensas) it.copy(campuses = campuses) else it }
        }
    }

    /** Clears the campus results so they are computed again (after "Reset maps cache"). */
    fun invalidateCampuses() {
        _state.update { it.copy(campuses = null) }
        resolveCampuses()
    }

    private companion object {
        const val SLOW_AFTER_MS = 8_000L
    }
}
