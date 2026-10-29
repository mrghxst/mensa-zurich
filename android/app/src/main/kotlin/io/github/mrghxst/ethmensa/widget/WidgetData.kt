// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.widget

import io.github.mrghxst.ethmensa.AppGraph
import io.github.mrghxst.ethmensa.domain.MealText
import io.github.mrghxst.ethmensa.domain.OpeningState
import io.github.mrghxst.ethmensa.domain.fits
import io.github.mrghxst.ethmensa.domain.formatPrice
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.model.Settings
import io.github.mrghxst.ethmensa.domain.nowInZurich
import io.github.mrghxst.ethmensa.domain.openingHoursText
import io.github.mrghxst.ethmensa.domain.openingState
import io.github.mrghxst.ethmensa.ui.components.MealLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDateTime

/** What the "Today's menu" widget shows (Phase 9). */
data class WidgetData(
    val mensaId: String?,
    val name: String,
    val state: OpeningState?,
    val hours: String,
    val meals: List<MealLine>,
)

/** The mensa for the widget: a favorite, else the most opened one, else Polyterrasse (eth/9). */
fun widgetMensa(mensas: List<Mensa>, settings: Settings, clicks: Map<String, Int>): Mensa? {
    val visible = mensas.filter { it.id !in settings.hidden }
    val byClicks = compareByDescending<Mensa> { clicks[it.id] ?: 0 }
    return visible.filter { it.id in settings.favorites }.minWithOrNull(byClicks)
        ?: visible.filter { (clicks[it.id] ?: 0) > 0 }.minWithOrNull(byClicks)
        ?: visible.firstOrNull { it.id == "eth/9" }
        ?: visible.firstOrNull()
}

/** Today's content for [mensas], or null while nothing has loaded. */
fun buildWidgetData(mensas: List<Mensa>?, settings: Settings, clicks: Map<String, Int>, now: LocalDateTime): WidgetData? {
    val mensa = widgetMensa(mensas ?: return null, settings, clicks) ?: return null
    val mealTimes = mensa.mealTimesOn(now.dayOfWeek.value)
    val meals = mealTimes.flatMap { it.meals }
        .filter { it.fits(settings.diet) }
        .mapNotNull { meal ->
            val name = meal.name ?: meal.title?.let(MealText::displayTitle) ?: return@mapNotNull null
            MealLine(name, formatPrice(meal.price, settings.priceType))
        }
        .distinct()
    return WidgetData(
        mensaId = mensa.id,
        name = mensa.name,
        state = openingState(mensa, now),
        hours = openingHoursText(mealTimes),
        meals = meals,
    )
}

/** The widget content as it changes: app data, settings and open counts (the clock is read on each change). */
fun widgetData(graph: AppGraph): Flow<WidgetData?> =
    combine(graph.mensaStore.state, graph.settings.settings, graph.clickCounts.counts) { load, settings, clicks ->
        buildWidgetData(load.mensas, settings, clicks, nowInZurich(graph.clock))
    }.distinctUntilChanged()
