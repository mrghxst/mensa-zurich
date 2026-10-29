// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import io.github.mrghxst.ethmensa.domain.ListFilters
import io.github.mrghxst.ethmensa.domain.MealText
import io.github.mrghxst.ethmensa.domain.OpeningState
import io.github.mrghxst.ethmensa.domain.PipelineInput
import io.github.mrghxst.ethmensa.domain.formatPrice
import io.github.mrghxst.ethmensa.domain.mensaList
import io.github.mrghxst.ethmensa.domain.model.Campus
import io.github.mrghxst.ethmensa.domain.model.MensaShowType
import io.github.mrghxst.ethmensa.domain.model.PriceType
import io.github.mrghxst.ethmensa.domain.model.Settings
import io.github.mrghxst.ethmensa.domain.model.SortBy
import io.github.mrghxst.ethmensa.domain.nowInZurich
import io.github.mrghxst.ethmensa.domain.openingHoursText
import io.github.mrghxst.ethmensa.domain.openingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

private val OpenColor = Color(0xFF22C55E)
private val ClosedColor = Color(0xFFEF4444)

/** The clock, ticking every 30 s so the open state stays current. */
@Composable
private fun rememberNow(graph: WearGraph): LocalDateTime {
    val now by produceState(nowInZurich(graph.clock)) {
        while (true) {
            delay(30_000)
            value = nowInZurich(graph.clock)
        }
    }
    return now
}

/** Shared modifiers for an item in a TransformingLazyColumn: full width, morphing height, list padding. */
private fun Modifier.listItem(scope: TransformingLazyColumnItemScope, spec: TransformationSpec, padding: Dp): Modifier =
    with(scope) { this@listItem.fillMaxWidth().transformedHeight(scope, spec).minimumVerticalContentPadding(padding) }

/** Mensas with today's state and hours (watchOS MainView), filtered and sorted like the phone list. */
@Composable
fun MensaListScreen(graph: WearGraph, onOpen: (String) -> Unit, onFilter: () -> Unit, onSettings: () -> Unit) {
    LifecycleEventEffect(Lifecycle.Event.ON_START) { graph.mensaStore.refresh() }
    val load by graph.mensaStore.state.collectAsStateWithLifecycle()
    val settings by graph.settings.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val clicks by graph.clickCounts.counts.collectAsStateWithLifecycle(initialValue = emptyMap())
    val now = rememberNow(graph)
    val rows = remember(load, settings, clicks, now) {
        mensaList(
            PipelineInput(
                mensas = load.mensas,
                allProvidersLoaded = load.allProvidersLoaded,
                filters = ListFilters(showType = settings.mensaShowType, campus = settings.campus, sortBy = settings.sortBy),
                clickCounts = clicks,
                campuses = load.campuses,
                now = now,
            ),
        )
    }
    val state = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val scope = rememberCoroutineScope()
    ScreenScaffold(scrollState = state) { contentPadding ->
        TransformingLazyColumn(state = state, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier.listItem(this, spec, ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(spec),
                ) { Text(stringResource(R.string.app_name)) }
            }
            when {
                rows == null -> item { CircularProgressIndicator(Modifier.size(32.dp)) }
                rows.isEmpty() -> item {
                    Text(
                        stringResource(R.string.no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.listItem(this, spec, ButtonDefaults.minimumVerticalListContentPadding),
                    )
                }
                // No item keys: providers arrive one after the other, and a keyed anchor would scroll the
                // list away from the top when ETH's mensas are inserted above UZH's.
                else -> items(rows) { mensa ->
                    val state = openingState(mensa, now)
                    val label = when (state) {
                        OpeningState.OPEN -> stringResource(R.string.a11y_open_now, mensa.name)
                        OpeningState.CLOSED -> stringResource(R.string.a11y_closed, mensa.name)
                        else -> mensa.name
                    }
                    val hours = openingHoursText(mensa.mealTimesOn(now.dayOfWeek.value))
                    FilledTonalButton(
                        onClick = {
                            scope.launch { graph.clickCounts.increment(mensa.id) }
                            onOpen(mensa.id)
                        },
                        modifier = Modifier.listItem(this, spec, ButtonDefaults.minimumVerticalListContentPadding)
                            .semantics { contentDescription = label },
                        transformation = SurfaceTransformation(spec),
                        icon = { StatusDot(state) },
                        label = { Text(mensa.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        secondaryLabel = if (hours.isNotEmpty()) {
                            { Text(hours, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        } else {
                            null
                        },
                    )
                }
            }
            item {
                Button(
                    onClick = onFilter,
                    modifier = Modifier.listItem(this, spec, ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(spec),
                    label = { Text(stringResource(R.string.filter_options)) },
                )
            }
            item {
                Button(
                    onClick = onSettings,
                    modifier = Modifier.listItem(this, spec, ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(spec),
                    label = { Text(stringResource(R.string.settings)) },
                )
            }
        }
    }
}

@Composable
private fun StatusDot(state: OpeningState) {
    val color = when (state) {
        OpeningState.OPEN -> OpenColor
        OpeningState.CLOSED -> ClosedColor
        else -> MaterialTheme.colorScheme.outline
    }
    Box(Modifier.size(10.dp).background(color, CircleShape))
}

/** Today's menu of one mensa (watchOS DetailView). */
@Composable
fun MensaDetailScreen(graph: WearGraph, mensaId: String) {
    val load by graph.mensaStore.state.collectAsStateWithLifecycle()
    val settings by graph.settings.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val now = rememberNow(graph)
    val mensa = load.mensa(mensaId)
    val mealTimes = mensa?.mealTimesOn(now.dayOfWeek.value).orEmpty()
    val state = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = state) { contentPadding ->
        TransformingLazyColumn(state = state, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier.listItem(this, spec, ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(spec),
                ) { Text(mensa?.name.orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            if (mealTimes.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_menu_on_this_day),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.listItem(this, spec, CardDefaults.minimumVerticalListContentPadding),
                    )
                }
            }
            mealTimes.forEachIndexed { index, mealTime ->
                item(key = "time$index") {
                    ListSubHeader(modifier = Modifier.listItem(this, spec, ListHeaderDefaults.minimumTopListContentPadding)) {
                        Text(listOfNotNull(mealTime.type, openingHoursText(listOf(mealTime)).ifEmpty { null }).joinToString(" · "))
                    }
                }
                mealTime.meals.forEachIndexed { mealIndex, meal ->
                    item(key = "meal$index-$mealIndex") {
                        TitleCard(
                            onClick = {},
                            modifier = Modifier.listItem(this, spec, CardDefaults.minimumVerticalListContentPadding),
                            transformation = SurfaceTransformation(spec),
                            title = { Text(meal.title?.let(MealText::displayTitle).orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            subtitle = formatPrice(meal.price, settings.priceType)?.let { price -> { Text(price) } },
                        ) {
                            Text(meal.name.orEmpty())
                        }
                    }
                }
            }
        }
    }
}

/** Status, location and sorting (watchOS FilterView). Stored in the watch's own settings. */
@Composable
fun FilterScreen(graph: WearGraph) {
    val settings by graph.settings.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val scope = rememberCoroutineScope()
    fun update(transform: (Settings) -> Settings) {
        scope.launch { graph.settings.update(transform) }
    }
    val state = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = state) { contentPadding ->
        TransformingLazyColumn(state = state, contentPadding = contentPadding) {
            item {
                ListHeader(modifier = Modifier.listItem(this, spec, ListHeaderDefaults.minimumTopListContentPadding)) {
                    Text(stringResource(R.string.filter_options))
                }
            }
            item { ListSubHeader { Text(stringResource(R.string.current_status)) } }
            item {
                Choice(stringResource(R.string.all), settings.mensaShowType == MensaShowType.ALL, spec) {
                    update { it.copy(mensaShowType = MensaShowType.ALL) }
                }
            }
            item {
                Choice(stringResource(R.string.currently_open), settings.mensaShowType == MensaShowType.OPEN, spec) {
                    update { it.copy(mensaShowType = MensaShowType.OPEN) }
                }
            }
            item { ListSubHeader { Text(stringResource(R.string.location)) } }
            item {
                Choice(stringResource(R.string.all_locations), settings.campus == null, spec) { update { it.copy(campus = null) } }
            }
            Campus.entries.forEach { campus ->
                item(key = campus.id) {
                    Choice(stringResource(campusName(campus)), settings.campus == campus, spec) {
                        update { it.copy(campus = campus) }
                    }
                }
            }
            item { ListSubHeader { Text(stringResource(R.string.sort_by)) } }
            item {
                Choice(stringResource(R.string.sort_smart), settings.sortBy == SortBy.SMART, spec) { update { it.copy(sortBy = SortBy.SMART) } }
            }
            item {
                Choice(stringResource(R.string.sort_name), settings.sortBy == SortBy.NAME, spec) { update { it.copy(sortBy = SortBy.NAME) } }
            }
        }
    }
}

/** Price to show (watchOS SettingsAppearanceView). */
@Composable
fun SettingsScreen(graph: WearGraph) {
    val settings by graph.settings.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val scope = rememberCoroutineScope()
    val state = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val prices = listOf(
        PriceType.ALL to R.string.all,
        PriceType.STUDENT to R.string.student,
        PriceType.STAFF to R.string.staff,
        PriceType.EXTERNAL to R.string.external,
    )
    ScreenScaffold(scrollState = state) { contentPadding ->
        TransformingLazyColumn(state = state, contentPadding = contentPadding) {
            item {
                ListHeader(modifier = Modifier.listItem(this, spec, ListHeaderDefaults.minimumTopListContentPadding)) {
                    Text(stringResource(R.string.settings))
                }
            }
            item { ListSubHeader { Text(stringResource(R.string.price_to_show)) } }
            prices.forEach { (type, label) ->
                item(key = type.name) {
                    Choice(stringResource(label), settings.priceType == type, spec) {
                        scope.launch { graph.settings.update { it.copy(priceType = type) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.Choice(label: String, selected: Boolean, spec: TransformationSpec, onSelect: () -> Unit) {
    RadioButton(
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.listItem(this, spec, ButtonDefaults.minimumVerticalListContentPadding),
        transformation = SurfaceTransformation(spec),
        label = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    )
}

private fun campusName(campus: Campus): Int = when (campus) {
    Campus.ZENTRUM -> R.string.zentrum
    Campus.HONGG -> R.string.honggerberg
    Campus.IRCHEL -> R.string.irchel
}
