// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.list

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.ui.isWideWindow
import io.github.mrghxst.ethmensa.data.StoreEvent
import io.github.mrghxst.ethmensa.domain.ActiveFilter
import io.github.mrghxst.ethmensa.domain.model.Campus
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.model.MensaCellType
import io.github.mrghxst.ethmensa.domain.model.SortBy
import io.github.mrghxst.ethmensa.ui.components.EmptyState
import io.github.mrghxst.ethmensa.ui.components.FloatingDock
import io.github.mrghxst.ethmensa.ui.components.GlassIconButton
import io.github.mrghxst.ethmensa.ui.components.GlassStyle
import io.github.mrghxst.ethmensa.ui.components.Icons
import io.github.mrghxst.ethmensa.ui.components.LucideIcon
import io.github.mrghxst.ethmensa.ui.components.MensaRow
import io.github.mrghxst.ethmensa.ui.components.ProvideGlass
import io.github.mrghxst.ethmensa.ui.components.SkeletonRow
import io.github.mrghxst.ethmensa.ui.components.frostedGlassSource
import io.github.mrghxst.ethmensa.ui.components.glass
import io.github.mrghxst.ethmensa.ui.components.weekdayName
import io.github.mrghxst.ethmensa.ui.components.liquidGlassSource
import io.github.mrghxst.ethmensa.ui.components.rememberGlassSources
import io.github.mrghxst.ethmensa.ui.components.rememberShimmerBrush
import io.github.mrghxst.ethmensa.ui.filter.FilterSheet
import io.github.mrghxst.ethmensa.ui.shareMensa
import io.github.mrghxst.ethmensa.ui.theme.Eth
import io.github.mrghxst.ethmensa.ui.theme.EthShapes
import io.github.mrghxst.ethmensa.ui.theme.Spacing
import java.time.format.TextStyle

private val TopBarHeight = 56.dp
private val DockHeight = 56.dp

@Composable
fun MensaListScreen(
    viewModel: MensaListViewModel,
    onOpenMensa: (Mensa) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLegi: () -> Unit,
    modifier: Modifier = Modifier,
    imageModifier: @Composable (Mensa) -> Modifier = { Modifier },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val wide = isWideWindow()
    val gutter = if (wide) Spacing.gutterWide else Spacing.gutter
    val cellType = if (wide && state.cellType == MensaCellType.LARGE) MensaCellType.STANDARD else state.cellType
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val glass = rememberGlassSources()

    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.refresh() }

    val updateFailed = stringResource(R.string.update_failed, "%s")
    val retry = stringResource(R.string.retry)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is StoreEvent.UpdateFailed && viewModel.uiState.value.hasData) {
                val names = event.providers.joinToString(" & ") { it.name }
                val result = snackbar.showSnackbar(updateFailed.format(names), retry, duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed) viewModel.refresh(fromUser = true)
            }
        }
    }
    val slowText = stringResource(R.string.slow_connection)
    val cancel = stringResource(R.string.cancel)
    LaunchedEffect(state.slow) {
        if (state.slow) {
            val result = snackbar.showSnackbar(slowText, cancel, duration = SnackbarDuration.Indefinite)
            if (result == SnackbarResult.ActionPerformed) viewModel.cancelRefresh()
        } else {
            snackbar.currentSnackbarData?.takeIf { it.visuals.message == slowText }?.dismiss()
        }
    }

    val density = LocalDensity.current
    val collapse by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / with(density) { 44.dp.toPx() }).coerceIn(0f, 1f)
        }
    }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    ProvideGlass(glass) {
        Box(modifier.fillMaxSize().background(Eth.colors.background)) {
            Box(Modifier.fillMaxSize().liquidGlassSource(glass)) {
                Box(Modifier.fillMaxSize().background(Eth.colors.background).frostedGlassSource(glass)) {
                    val pullState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.refresh(fromUser = true) },
                        state = pullState,
                        indicator = {
                            PullToRefreshDefaults.LoadingIndicator(
                                state = pullState,
                                isRefreshing = state.isRefreshing,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = statusTop + TopBarHeight),
                            )
                        },
                    ) {
                        if (state.showUnavailable) {
                            Unavailable(onRetry = { viewModel.refresh(fromUser = true) }, top = statusTop + TopBarHeight)
                        } else {
                            MensaList(
                                state = state,
                                cellType = cellType,
                                gutter = gutter,
                                listState = listState,
                                contentPadding = PaddingValues(top = statusTop + TopBarHeight, bottom = navBottom + DockHeight + 32.dp),
                                onOpenMensa = onOpenMensa,
                                onShare = { shareMensa(context, it) },
                                onRemoveFilter = viewModel::removeFilter,
                                onReset = viewModel::resetFiltersAndSearch,
                                imageModifier = imageModifier,
                            )
                        }
                    }
                }
            }

            TopBar(collapse = collapse, onOpenLegi = onOpenLegi, onOpenSettings = onOpenSettings, gutter = gutter)

            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = gutter, vertical = 12.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SnackbarHost(snackbar, Modifier.widthIn(max = 560.dp))
                    FloatingDock(
                        query = state.query,
                        onQueryChange = viewModel::onQueryChange,
                        filtersActive = state.filters.isAnyActive,
                        onFilterClick = { showFilters = true },
                        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            state = state,
            showLarge = !wide,
            onDismiss = { showFilters = false },
            onOpenOnly = viewModel::setOpenOnly,
            onCampus = viewModel::toggleCampus,
            onWeekday = viewModel::setWeekday,
            onAllergenFriendly = viewModel::setAllergenFriendly,
            onSort = viewModel::setSortBy,
            onCellType = viewModel::setCellType,
            onReset = viewModel::resetFilters,
        )
    }
}

@Composable
private fun TopBar(collapse: Float, onOpenLegi: () -> Unit, onOpenSettings: () -> Unit, gutter: Dp) {
    Box(Modifier.fillMaxWidth()) {
        // Frosted bar fades in once the content scrolls under it.
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = collapse }
                .glass(GlassStyle.FROSTED, RectangleShape, shadow = false),
        )
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(TopBarHeight).padding(horizontal = gutter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(Icons.idCard, stringResource(R.string.legi_card), onOpenLegi)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.app_name),
                    style = Eth.type.screenTitle,
                    color = Eth.colors.textPrimary,
                    modifier = Modifier.graphicsLayer { alpha = collapse },
                )
            }
            GlassIconButton(Icons.settings, stringResource(R.string.settings), onOpenSettings)
        }
    }
}

@Composable
private fun MensaList(
    state: MensaListUiState,
    cellType: MensaCellType,
    gutter: Dp,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onOpenMensa: (Mensa) -> Unit,
    onShare: (Mensa) -> Unit,
    onRemoveFilter: (ActiveFilter) -> Unit,
    onReset: () -> Unit,
    imageModifier: @Composable (Mensa) -> Modifier,
) {
    val shimmer = rememberShimmerBrush()
    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "title", contentType = "title") {
            Text(
                stringResource(R.string.app_name),
                style = Eth.type.largeTitle,
                color = Eth.colors.textPrimary,
                modifier = Modifier.padding(horizontal = gutter).padding(top = 4.dp).semantics { heading() },
            )
        }
        if (state.isOffline && state.hasData) {
            item(key = "offline", contentType = "banner") {
                OfflineBanner(Modifier.animateItem().padding(horizontal = gutter))
            }
        }
        val chips = state.filters.activeFilters
        if (chips.isNotEmpty()) {
            item(key = "chips", contentType = "chips") {
                FilterChipRow(chips, gutter, onRemoveFilter, Modifier.animateItem())
            }
        }
        val rows = state.rows
        when {
            rows == null -> items(10, key = { "skeleton$it" }, contentType = { "skeleton" }) {
                SkeletonRow(cellType, shimmer, Modifier.padding(horizontal = gutter))
            }
            rows.isEmpty() -> item(key = "empty", contentType = "empty") {
                EmptyState(
                    icon = Icons.cookingPot,
                    title = stringResource(R.string.no_results),
                    message = stringResource(R.string.no_results_for_selected_filters),
                    action = stringResource(R.string.reset_filters_and_search),
                    onAction = onReset,
                    modifier = Modifier.animateItem(),
                )
            }
            else -> items(rows, key = { it.mensa.id }, contentType = { cellType }) { row ->
                MensaRow(
                    mensa = row.mensa,
                    state = row.state,
                    hoursToday = row.hoursToday,
                    cellType = cellType,
                    onOpen = { onOpenMensa(row.mensa) },
                    onShare = { onShare(row.mensa) },
                    modifier = Modifier.animateItem().padding(horizontal = gutter),
                    imageModifier = imageModifier(row.mensa),
                )
            }
        }
    }
}

@Composable
private fun FilterChipRow(chips: List<ActiveFilter>, gutter: Dp, onRemove: (ActiveFilter) -> Unit, modifier: Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(chips, key = { it.key }) { chip ->
            val label = chipLabel(chip)
            val removes = stringResource(R.string.removes_this_filter)
            Row(
                Modifier
                    .animateItem(fadeInSpec = spring(stiffness = 380f), fadeOutSpec = spring(stiffness = 380f))
                    .clip(EthShapes.pill)
                    .background(Eth.colors.accentSoft)
                    .clickable(onClickLabel = removes) { onRemove(chip) }
                    .semantics { contentDescription = "$label, $removes" }
                    .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = Eth.type.label, color = Eth.colors.accent)
                Spacer(Modifier.width(6.dp))
                LucideIcon(Icons.close, null, tint = Eth.colors.accent, size = 14.dp)
            }
        }
    }
}

@Composable
fun chipLabel(chip: ActiveFilter): String = when (chip) {
    ActiveFilter.OpenOnly -> stringResource(R.string.open_only)
    is ActiveFilter.CampusFilter -> campusLabel(chip.campus)
    is ActiveFilter.Weekday -> weekdayName(chip.weekday, TextStyle.FULL)
    ActiveFilter.AllergenFriendly -> stringResource(R.string.allergen_friendly)
    is ActiveFilter.Sorting -> when (chip.sortBy) {
        SortBy.NAME -> stringResource(R.string.sorting_by_name)
        SortBy.SMART -> stringResource(R.string.smart_sorting)
    }
}

@Composable
fun campusLabel(campus: Campus): String = stringResource(
    when (campus) {
        Campus.ZENTRUM -> R.string.zentrum
        Campus.HONGG -> R.string.honggerberg
        Campus.IRCHEL -> R.string.irchel
    },
)

@Composable
private fun OfflineBanner(modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .glass(GlassStyle.FROSTED, EthShapes.pill, shadow = false)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(Icons.wifiOff, null, tint = Eth.colors.textSecondary, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.offline_banner), style = Eth.type.label, color = Eth.colors.textSecondary)
    }
}

@Composable
private fun Unavailable(onRetry: () -> Unit, top: Dp) {
    // Scrollable so pull-to-refresh also works here.
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = top + 80.dp)) {
        item {
            EmptyState(
                icon = Icons.wifiOff,
                title = stringResource(R.string.internet_connection_required),
                message = stringResource(R.string.internet_connection_required_message),
                action = stringResource(R.string.retry),
                onAction = onRetry,
            )
        }
    }
}

