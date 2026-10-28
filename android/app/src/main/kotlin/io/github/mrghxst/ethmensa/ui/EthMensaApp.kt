// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.mrghxst.ethmensa.EthMensaApplication
import io.github.mrghxst.ethmensa.data.ProviderResult
import io.github.mrghxst.ethmensa.domain.OpeningState
import io.github.mrghxst.ethmensa.domain.model.Provider
import io.github.mrghxst.ethmensa.domain.nowInZurich
import io.github.mrghxst.ethmensa.domain.openingState
import io.github.mrghxst.ethmensa.domain.todayWeekday
import io.github.mrghxst.ethmensa.ui.theme.Eth

/** Temporary Phase 2 debug screen: the live merged mensa list. Replaced in Phase 3. */
@Composable
fun EthMensaApp() {
    val graph = (LocalContext.current.applicationContext as EthMensaApplication).graph
    val results = remember { mutableStateMapOf<Provider, ProviderResult>() }
    var campuses by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    LaunchedEffect(Unit) {
        graph.mensaRepository.observeMensas().collect { result ->
            results[result.provider] = result
            Log.i("DebugList", "${result.provider}: ${result.mensas.size} mensas cached=${result.isCached} final=${result.isFinal} error=${result.error}")
            if (result.isFinal) {
                val all = results.values.flatMap { it.mensas }
                campuses = graph.locator.campuses(all)
                all.forEach { m -> Log.i("DebugList", "${m.id} ${m.name} mealTimes=${m.mealTimes.size} campus=${campuses[m.id]}") }
            }
        }
    }
    val now = nowInZurich(graph.clock)
    val weekday = todayWeekday(graph.clock)
    val mensas = listOfNotNull(results[Provider.ETH], results[Provider.UZH]).flatMap { it.mensas }
    LazyColumn(
        Modifier.fillMaxSize().background(Eth.colors.background).safeDrawingPadding(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            val status = results.values.joinToString { "${it.provider}: ${it.mensas.size}${if (it.isCached) " (cached)" else ""}${it.error?.let { e -> " $e" } ?: ""}" }
            Text("Debug · $status", style = Eth.type.caption, color = Eth.colors.textSecondary)
        }
        items(mensas, key = { it.id }) { mensa ->
            val state = openingState(mensa, now)
            val meals = mensa.mealTimesOn(weekday).sumOf { it.meals.size }
            val dot = when (state) { OpeningState.OPEN -> "●"; OpeningState.CLOSED -> "○"; OpeningState.UNKNOWN -> "·" }
            Text(
                "$dot ${mensa.id}  ${mensa.name} — $meals meals today, ${campuses[mensa.id] ?: "-"}",
                style = Eth.type.body,
                color = Eth.colors.textPrimary,
            )
        }
    }
}
