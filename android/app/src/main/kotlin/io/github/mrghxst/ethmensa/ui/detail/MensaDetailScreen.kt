// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.ui.components.BorderedCard
import io.github.mrghxst.ethmensa.ui.components.EmptyState
import io.github.mrghxst.ethmensa.ui.components.GlassIconButton
import io.github.mrghxst.ethmensa.ui.components.GlassStyle
import io.github.mrghxst.ethmensa.ui.components.Icons
import io.github.mrghxst.ethmensa.ui.components.LightStatusBarIcons
import io.github.mrghxst.ethmensa.ui.components.LucideIcon
import io.github.mrghxst.ethmensa.ui.components.MapThumbnail
import io.github.mrghxst.ethmensa.ui.components.MealCard
import io.github.mrghxst.ethmensa.ui.components.ProvideGlass
import io.github.mrghxst.ethmensa.ui.components.SectionLabel
import io.github.mrghxst.ethmensa.ui.components.WeekdayGroup
import io.github.mrghxst.ethmensa.ui.components.frostedGlassSource
import io.github.mrghxst.ethmensa.ui.components.glass
import io.github.mrghxst.ethmensa.ui.components.liquidGlassSource
import io.github.mrghxst.ethmensa.ui.components.rememberGlassSources
import io.github.mrghxst.ethmensa.ui.shareMensa
import io.github.mrghxst.ethmensa.ui.theme.Eth
import io.github.mrghxst.ethmensa.ui.theme.Spacing

private val HeroHeight = 240.dp
private val TopBarHeight = 56.dp

@Composable
fun MensaDetailScreen(
    viewModel: MensaDetailViewModel,
    onBack: (() -> Unit)?,
    onOpenImage: (String) -> Unit,
    modifier: Modifier = Modifier,
    heroModifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mensa = state.mensa
    val context = LocalContext.current
    val gutter = if (LocalConfiguration.current.screenWidthDp >= 600) Spacing.gutterWide else Spacing.gutter
    val glass = rememberGlassSources()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val hasHero = mensa?.imageUrl != null
    val collapse by remember(hasHero) {
        derivedStateOf {
            if (!hasHero) {
                1f
            } else if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                val heroPx = with(density) { (HeroHeight - TopBarHeight).toPx() }
                ((listState.firstVisibleItemScrollOffset - heroPx * 0.6f) / (heroPx * 0.4f)).coerceIn(0f, 1f)
            }
        }
    }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LightStatusBarIcons(lightIcons = hasHero && collapse < 0.5f)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    ProvideGlass(glass) {
        Box(modifier.fillMaxSize().background(Eth.colors.background)) {
            Box(Modifier.fillMaxSize().liquidGlassSource(glass)) {
                Box(Modifier.fillMaxSize().background(Eth.colors.background).frostedGlassSource(glass)) {
                    val pull = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = state.refreshing,
                        onRefresh = viewModel::refresh,
                        state = pull,
                        indicator = {
                            PullToRefreshDefaults.LoadingIndicator(
                                state = pull,
                                isRefreshing = state.refreshing,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = statusTop + TopBarHeight),
                            )
                        },
                    ) {
                        if (mensa == null) {
                            NotLoaded(state.notFound, statusTop + TopBarHeight)
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    top = if (hasHero) 0.dp else statusTop + TopBarHeight,
                                    bottom = navBottom + 32.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                            ) {
                                if (hasHero) {
                                    item(key = "hero") { Hero(mensa, gutter, heroModifier) }
                                } else {
                                    item(key = "title") {
                                        Text(
                                            mensa.name,
                                            style = Eth.type.largeTitle,
                                            color = Eth.colors.textPrimary,
                                            modifier = Modifier.padding(horizontal = gutter).semantics { heading() },
                                        )
                                    }
                                }
                                item(key = "info") { InfoCard(mensa, state, gutter) }
                                item(key = "days") {
                                    WeekdayGroup(
                                        selected = state.selectedWeekday,
                                        today = state.today,
                                        onSelect = viewModel::selectWeekday,
                                        enabled = !state.weekdayLocked,
                                        modifier = Modifier.padding(horizontal = gutter),
                                    )
                                }
                                if (state.allergenFriendly) {
                                    item(key = "friendly") {
                                        Row(Modifier.padding(horizontal = gutter), verticalAlignment = Alignment.CenterVertically) {
                                            LucideIcon(Icons.shieldCheck, null, tint = Eth.colors.accent, size = 16.dp)
                                            Spacer(Modifier.width(6.dp))
                                            Text(stringResource(R.string.allergen_friendly), style = Eth.type.label, color = Eth.colors.accent)
                                        }
                                    }
                                }
                                meals(state, gutter, onOpenImage)
                            }
                        }
                    }
                }
            }

            // Top chrome: frosted bar (fades in) + liquid-glass back and share buttons.
            Box(Modifier.fillMaxWidth()) {
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
                    if (onBack != null) {
                        GlassIconButton(Icons.arrowLeft, stringResource(R.string.back), onBack)
                    } else {
                        Spacer(Modifier.width(44.dp))
                    }
                    Text(
                        mensa?.name.orEmpty(),
                        style = Eth.type.screenTitle,
                        color = Eth.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp).graphicsLayer { alpha = if (hasHero) collapse else 0f },
                    )
                    if (mensa != null) GlassIconButton(Icons.share, stringResource(R.string.share), { shareMensa(context, mensa) })
                }
            }
        }
    }
}

@Composable
private fun Hero(mensa: Mensa, gutter: Dp, heroModifier: Modifier) {
    val bg = Eth.colors.background
    Box(Modifier.fillMaxWidth().height(HeroHeight)) {
        AsyncImage(
            model = mensa.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = heroModifier.fillMaxSize().background(Eth.colors.surfaceMuted),
        )
        // Fades the photo into the background and keeps the title legible.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.45f),
                    0.3f to Color.Transparent,
                    0.6f to bg.copy(alpha = 0.55f),
                    1f to bg,
                ),
            ),
        )
        Text(
            mensa.name,
            style = Eth.type.largeTitle,
            color = Eth.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = gutter).semantics { heading() },
        )
    }
}

@Composable
private fun InfoCard(mensa: Mensa, state: MensaDetailUiState, gutter: Dp) {
    BorderedCard(Modifier.padding(horizontal = gutter).fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            MapThumbnail(state.location, mensa.name)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                mensa.location?.let {
                    Text(
                        it.replace("​", ""),
                        style = Eth.type.body,
                        color = Eth.colors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.hours.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LucideIcon(Icons.clock, null, tint = Eth.colors.textTertiary, size = 14.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(state.hours, style = Eth.type.mono, color = Eth.colors.textPrimary)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.meals(
    state: MensaDetailUiState,
    gutter: Dp,
    onOpenImage: (String) -> Unit,
) {
    val sections = state.sections
    when {
        sections.isEmpty() -> item(key = "nomenu") {
            EmptyState(Icons.cookingPot, stringResource(R.string.no_menu), stringResource(R.string.no_menu_on_this_day))
        }
        sections.all { it.hiddenByAllergens } -> item(key = "nofriendly") {
            EmptyState(
                Icons.shieldCheck,
                stringResource(R.string.no_allergen_friendly_meal),
                stringResource(R.string.no_meal_without_your_allergens_on_this_day),
            )
        }
        else -> sections.forEachIndexed { index, section ->
            item(key = "label$index") {
                SectionLabel(section.title.orEmpty(), Modifier.padding(horizontal = gutter).padding(top = 12.dp))
            }
            if (section.hiddenByAllergens) {
                item(key = "hidden$index") {
                    Text(
                        stringResource(R.string.no_allergen_friendly_meal),
                        style = Eth.type.caption,
                        color = Eth.colors.textTertiary,
                        modifier = Modifier.padding(horizontal = gutter),
                    )
                }
            }
            itemsIndexed(section.meals, key = { i, _ -> "meal$index-$i" }) { _, meal ->
                MealCard(
                    meal = meal,
                    priceType = state.priceType,
                    showAllergens = state.showAllergens,
                    onImageClick = onOpenImage,
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }
        }
    }
}

@Composable
private fun NotLoaded(notFound: Boolean, top: Dp) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = top + 80.dp)) {
        item {
            if (notFound) {
                EmptyState(Icons.alert, stringResource(R.string.universal_link_no_mensa), stringResource(R.string.universal_link_no_mensa_message))
            } else {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator(color = Eth.colors.accent)
                }
            }
        }
    }
}

