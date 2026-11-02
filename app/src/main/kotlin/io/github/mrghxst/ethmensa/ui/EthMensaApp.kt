// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Stable
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation3.runtime.NavEntryDecorator
import io.github.mrghxst.ethmensa.ui.theme.ScreenMotion
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.mrghxst.ethmensa.domain.AppLink
import io.github.mrghxst.ethmensa.domain.ListFilters
import io.github.mrghxst.ethmensa.domain.model.MensaShowType
import io.github.mrghxst.ethmensa.ui.legi.LegiScreen
import io.github.mrghxst.ethmensa.ui.settings.MyMensasScreen
import io.github.mrghxst.ethmensa.ui.settings.NotificationsScreen
import io.github.mrghxst.ethmensa.ui.theme.Eth
import kotlinx.coroutines.flow.first

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

@Serializable
data object LegiRoute : NavKey

@Serializable
data object NotificationsRoute : NavKey

@Serializable
data object MyMensasRoute : NavKey

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Back to the list, closing everything on top of it. */
fun NavBackStack<NavKey>.popToList() {
    while (size > 1) removeLastOrNull()
}

/** Opens a mensa, replacing any open detail so the back stack stays List → Detail. */
fun NavBackStack<NavKey>.openMensa(id: String) {
    removeAll { it is DetailRoute || it is ImageRoute }
    add(DetailRoute(id))
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun EthMensaApp(pendingLink: AppLink?, onLinkHandled: () -> Unit) {
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
    val twoPane = isWideWindow()
    var linkNotFound by rememberSaveable { mutableStateOf(false) }

    // Links, app shortcuts and the Quick Settings tile (§8, §10.8).
    LaunchedEffect(pendingLink) {
        when (val link = pendingLink ?: return@LaunchedEffect) {
            is AppLink.OpenMensa -> {
                backStack.popToList()
                // Unknown ids only count as "not found" once every provider is up to date (iOS initializeLinkHandlers).
                val state = graph.mensaStore.state.first { it.mensa(link.mensaId) != null || it.allProvidersUpToDate }
                if (state.mensa(link.mensaId) != null) backStack.openMensa(link.mensaId) else linkNotFound = true
            }
            AppLink.Legi -> if (backStack.lastOrNull() != LegiRoute) backStack.add(LegiRoute)
            AppLink.NotificationSettings -> {
                backStack.popToList()
                backStack.add(SettingsRoute)
                backStack.add(NotificationsRoute)
            }
            AppLink.CurrentlyOpen -> {
                backStack.popToList()
                graph.listSession.apply(ListFilters(showType = MensaShowType.OPEN))
            }
            is AppLink.ShowCampus -> {
                backStack.popToList()
                graph.listSession.apply(ListFilters(campus = link.campus))
            }
        }
        onLinkHandled()
    }
    if (linkNotFound) {
        AlertDialog(
            onDismissRequest = { linkNotFound = false },
            title = { Text(stringResource(R.string.universal_link_no_mensa), style = Eth.type.screenTitle) },
            text = { Text(stringResource(R.string.universal_link_no_mensa_message), style = Eth.type.body, color = Eth.colors.textSecondary) },
            confirmButton = { TextButton(onClick = { linkNotFound = false }) { Text(stringResource(R.string.close), color = Eth.colors.accent) } },
            containerColor = Eth.colors.surface,
        )
    }

    val backGesture = rememberBackGestureLatch()
    SharedTransitionLayout(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this, LocalBackGesture provides backGesture) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                sceneStrategies = listOf(listDetail),
                sharedTransitionScope = this,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                    rememberCardDuringBackDecorator(),
                ),
                transitionSpec = { ScreenMotion.push() },
                popTransitionSpec = { ScreenMotion.pop() },
                predictivePopTransitionSpec = { edge -> ScreenMotion.predictivePop(edge) },
                entryProvider = entryProvider {
                    entry<ListRoute>(
                        metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { NoMensaSelected() }),
                    ) {
                        val vm: MensaListViewModel = viewModel(factory = viewModelFactory { initializer { MensaListViewModel(graph) } })
                        MensaListScreen(
                            viewModel = vm,
                            onOpenMensa = { backStack.openMensa(it.id) },
                            onOpenSettings = { backStack.add(SettingsRoute) },
                            onOpenLegi = { backStack.add(LegiRoute) },
                            imageModifier = { mensa -> if (twoPane) Modifier else Modifier.sharedImage(mensa.id) },
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
                            heroModifier = if (twoPane) Modifier else Modifier.sharedImage(route.mensaId),
                            chromeModifier = if (twoPane) Modifier else Modifier.aboveSharedImage(),
                        )
                    }
                    entry<ImageRoute>(metadata = overlayMotion) { route ->
                        ImageViewerScreen(route.url, onClose = { backStack.removeLastOrNull() })
                    }
                    entry<SettingsRoute> {
                        SettingsScreen(
                            viewModel = settingsViewModel(graph),
                            onBack = { backStack.removeLastOrNull() },
                            onOpenAllergens = { backStack.add(AllergensRoute) },
                            onOpenMyMensas = { backStack.add(MyMensasRoute) },
                            onOpenNotifications = { backStack.add(NotificationsRoute) },
                            onOpenText = { asset, title -> backStack.add(TextRoute(asset, title)) },
                        )
                    }
                    entry<AllergensRoute> {
                        AllergensScreen(settingsViewModel(graph), onBack = { backStack.removeLastOrNull() })
                    }
                    entry<TextRoute> { route ->
                        TextScreen(route.asset, route.title, onBack = { backStack.removeLastOrNull() })
                    }
                    entry<MyMensasRoute> {
                        MyMensasScreen(settingsViewModel(graph), onBack = { backStack.removeLastOrNull() })
                    }
                    entry<NotificationsRoute> {
                        NotificationsScreen(settingsViewModel(graph), onBack = { backStack.removeLastOrNull() })
                    }
                    entry<LegiRoute>(
                        metadata = NavDisplay.transitionSpec { ScreenMotion.sheetIn() } +
                            NavDisplay.popTransitionSpec { ScreenMotion.sheetOut() } +
                            NavDisplay.predictivePopTransitionSpec { ScreenMotion.overlayPredictiveOut() },
                    ) {
                        LegiScreen(onClose = { backStack.removeLastOrNull() })
                    }
                },
            )
        }
    }
}

@Composable
private fun settingsViewModel(graph: AppGraph): SettingsViewModel =
    viewModel(factory = viewModelFactory { initializer { SettingsViewModel(graph) } })

private val overlayMotion = NavDisplay.transitionSpec { ScreenMotion.overlayIn() } +
    NavDisplay.popTransitionSpec { ScreenMotion.overlayOut() } +
    NavDisplay.predictivePopTransitionSpec { ScreenMotion.overlayPredictiveOut() }

/**
 * Rounds the corners of a screen while it leaves, so the back gesture shrinks it into a card
 * (see [ScreenMotion.predictivePop]). Push and pop fade the leaving screen out before the corners show.
 */
@Composable
private fun rememberCardDuringBackDecorator(): NavEntryDecorator<NavKey> = remember {
    NavEntryDecorator { entry ->
        val transition = LocalNavAnimatedContentScope.current.transition
        val corner by transition.animateDp(
            transitionSpec = {
                keyframes {
                    durationMillis = ScreenMotion.PredictiveMillis
                    ScreenMotion.PredictiveCorner at ScreenMotion.PredictiveMillis / 4
                }
            },
            label = "screen-corner",
        ) { if (it == EnterExitState.PostExit) ScreenMotion.PredictiveCorner else 0.dp }
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                shape = RoundedCornerShape(corner)
                clip = corner > 0.dp
            },
        ) { entry.Content() }
    }
}

/** True from the start of a back gesture until the screens it moved have settled. */
@Stable
private class BackGestureLatch {
    var active by mutableStateOf(false)
}

private val LocalBackGesture = staticCompositionLocalOf { BackGestureLatch() }

@Composable
private fun rememberBackGestureLatch(): BackGestureLatch {
    val latch = remember { BackGestureLatch() }
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    LaunchedEffect(dispatcher) {
        dispatcher?.transitionState?.collectLatest { state ->
            if (state is NavigationEventTransitionState.InProgress) {
                latch.active = true
            } else {
                // Let the commit or cancel animation finish before shared elements come back.
                delay(ScreenMotion.PredictiveMillis + 100L)
                latch.active = false
            }
        }
    }
    return latch
}

/**
 * Shared element between the list thumbnail and the detail hero. It's off during the back
 * gesture: the photo then shrinks with the rest of the detail instead of leaving the card.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.sharedImage(mensaId: String): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedContentScope.current
    val backGesture = LocalBackGesture.current
    return with(shared) {
        val config = remember(backGesture) {
            object : SharedTransitionScope.SharedContentConfig {
                override val SharedTransitionScope.SharedContentState.isEnabled: Boolean
                    get() = !backGesture.active
            }
        }
        // sharedElement, not sharedBounds: both ends show the same cropped photo, so one image
        // morphs between the two sizes instead of two copies crossfading.
        this@sharedImage.sharedElement(
            rememberSharedContentState("image-$mensaId", config),
            animated,
            boundsTransform = ScreenMotion.sharedBounds,
        )
    }
}

/**
 * Draws the detail's hero gradient, title and top bar above the flying photo, fading them in
 * as it lands. Without this, the photo covers them until the transition ends and they pop in.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.aboveSharedImage(): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedContentScope.current
    // During the back gesture the whole screen shrinks and fades as one card.
    val exit = if (LocalBackGesture.current.active) ExitTransition.None else fadeOut(tween(90))
    return with(shared) {
        with(animated) {
            this@aboveSharedImage
                .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
                .animateEnterExit(enter = fadeIn(tween(200, delayMillis = 60)), exit = exit)
        }
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

