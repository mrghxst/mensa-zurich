// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import kotlinx.serialization.Serializable

@Serializable
data object ListKey : NavKey

@Serializable
data class DetailKey(val mensaId: String) : NavKey

@Serializable
data object FilterKey : NavKey

@Serializable
data object SettingsKey : NavKey

/** ETH blue on the watch's black background. */
private val WearColors = ColorScheme(
    primary = Color(0xFF4C8DF6),
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFF173A6E),
    onPrimaryContainer = Color(0xFFDCE7FB),
)

class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = (application as WearApplication).graph
        setContent {
            MaterialTheme(colorScheme = WearColors) {
                WearApp(graph)
            }
        }
    }
}

@Composable
private fun WearApp(graph: WearGraph) {
    val backStack = rememberNavBackStack(ListKey)
    AppScaffold {
        NavDisplay(
            backStack = backStack,
            sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
            entryProvider = entryProvider {
                entry<ListKey> {
                    MensaListScreen(
                        graph = graph,
                        onOpen = { backStack.add(DetailKey(it)) },
                        onFilter = { backStack.add(FilterKey) },
                        onSettings = { backStack.add(SettingsKey) },
                    )
                }
                entry<DetailKey> { key -> MensaDetailScreen(graph, key.mensaId) }
                entry<FilterKey> { FilterScreen(graph) }
                entry<SettingsKey> { SettingsScreen(graph) }
            },
        )
    }
}
