// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import io.github.mrghxst.ethmensa.AppGraph
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.ui.onboarding.OnboardingScreen
import io.github.mrghxst.ethmensa.ui.settings.AllergensScreen
import io.github.mrghxst.ethmensa.ui.settings.SettingsScreen
import io.github.mrghxst.ethmensa.ui.settings.SettingsViewModel
import io.github.mrghxst.ethmensa.ui.settings.TextScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import io.github.mrghxst.ethmensa.ui.components.EmptyState
import io.github.mrghxst.ethmensa.ui.components.Icons
import io.github.mrghxst.ethmensa.ui.detail.MensaDetailScreen
import io.github.mrghxst.ethmensa.ui.detail.MensaDetailViewModel
import io.github.mrghxst.ethmensa.ui.image.ImageViewerScreen
import io.github.mrghxst.ethmensa.ui.list.MensaListScreen
import io.github.mrghxst.ethmensa.ui.list.MensaListViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ListRoute : NavKey

@Serializable
data class DetailRoute(val mensaId: String) : NavKey

@Serializable
data class ImageRoute(val url: String) : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object AllergensRoute : NavKey

@Serializable
data class TextRoute(val asset: String, val title: String) : NavKey

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Opens a mensa, replacing any open detail so the back stack stays List → Detail. */
fun NavBackStack<NavKey>.openMensa(id: String) {
    removeAll { it is DetailRoute || it is ImageRoute }
    add(DetailRoute(id))
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun EthMensaApp() {
    val graph = LocalAppGraph.current
    val settings by graph.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    when (settings?.completedOnboarding) {
        null -> return
        false -> {
            OnboardingScreen(onContinue = { scope.launch { graph.settings.update { it.copy(completedOnboarding = true) } } })
            return
        }
        true -> Unit
    }
    val backStack = rememberNavBackStack(ListRoute)
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(adaptiveInfo) {
        calculatePaneScaffoldDirective(adaptiveInfo).copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetail = rememberListDetailSceneStrategy<NavKey>(directive = directive)
    val twoPane = LocalConfiguration.current.screenWidthDp >= 600

    SharedTransitionLayout(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                sceneStrategies = listOf(listDetail),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<ListRoute>(
                        metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { NoMensaSelected() }),
                    ) {
                        val vm: MensaListViewModel = viewModel(factory = viewModelFactory { initializer { MensaListViewModel(graph) } })
                        MensaListScreen(
                            viewModel = vm,
                            onOpenMensa = { backStack.openMensa(it.id) },
                            onOpenSettings = { backStack.add(SettingsRoute) },
                            onOpenLegi = {},
                            imageModifier = { mensa -> if (twoPane) Modifier else sharedImage(mensa.id) },
                        )
                    }
                    entry<DetailRoute>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
                        val vm: MensaDetailViewModel = viewModel(
                            key = route.mensaId,
                            factory = viewModelFactory { initializer { MensaDetailViewModel(graph, route.mensaId) } },
                        )
                        MensaDetailScreen(
                            viewModel = vm,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenImage = { backStack.add(ImageRoute(it)) },
                            heroModifier = if (twoPane) Modifier else sharedImage(route.mensaId),
                        )
                    }
                    entry<ImageRoute> { route ->
                        ImageViewerScreen(route.url, onClose = { backStack.removeLastOrNull() })
                    }
                    entry<SettingsRoute> {
                        SettingsScreen(
                            viewModel = settingsViewModel(graph),
                            onBack = { backStack.removeLastOrNull() },
                            onOpenAllergens = { backStack.add(AllergensRoute) },
                            onOpenNotifications = {},
                            onOpenText = { asset, title -> backStack.add(TextRoute(asset, title)) },
                        )
                    }
                    entry<AllergensRoute> {
                        AllergensScreen(settingsViewModel(graph), onBack = { backStack.removeLastOrNull() })
                    }
                    entry<TextRoute> { route ->
                        TextScreen(route.asset, route.title, onBack = { backStack.removeLastOrNull() })
                    }
                },
            )
        }
    }
}

@Composable
private fun settingsViewModel(graph: AppGraph): SettingsViewModel =
    viewModel(factory = viewModelFactory { initializer { SettingsViewModel(graph) } })

/** Shared element between the list thumbnail and the detail hero. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun sharedImage(mensaId: String): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return Modifier
    val animated = LocalNavAnimatedContentScope.current
    return with(shared) {
        Modifier.sharedBounds(rememberSharedContentState("image-$mensaId"), animated)
    }
}

@Composable
private fun NoMensaSelected() {
    Box(Modifier.fillMaxSize()) {
        EmptyState(
            icon = Icons.utensils,
            title = stringResource(R.string.no_mensa_selected),
            message = stringResource(R.string.no_mensa_selected_message),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

