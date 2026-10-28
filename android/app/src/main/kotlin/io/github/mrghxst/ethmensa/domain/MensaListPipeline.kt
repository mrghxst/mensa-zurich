// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain

import io.github.mrghxst.ethmensa.domain.model.Campus
import io.github.mrghxst.ethmensa.domain.model.KnownAllergen
import io.github.mrghxst.ethmensa.domain.model.Meal
import io.github.mrghxst.ethmensa.domain.model.MealTime
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.model.SortBy
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale

/** Inputs to the list pipeline (MensaDataManager.mensaList). */
data class PipelineInput(
    /** Null while nothing has loaded yet. */
    val mensas: List<Mensa>?,
    val allProvidersLoaded: Boolean,
    val query: String = "",
    val filters: ListFilters = ListFilters(),
    val hideMensaWithNoMenus: Boolean = false,
    val userAllergens: Set<KnownAllergen> = emptySet(),
    val clickCounts: Map<String, Int> = emptyMap(),
    /** Campus per mensa id. Null while campus resolution hasn't finished. */
    val campuses: Map<String, Campus?>? = emptyMap(),
    val now: LocalDateTime,
)

private val nameCollator: Collator = Collator.getInstance(Locale.forLanguageTag("de-CH")).apply {
    strength = Collator.SECONDARY
}

/** The meals of a meal time that contain none of [userAllergens]. */
fun allergenCompatibleMeals(mealTime: MealTime, userAllergens: Set<KnownAllergen>): List<Meal> =
    mealTime.meals.filter { meal -> meal.allergens.none { it in userAllergens } }

/** False only if every meal of every meal time on [weekday] contains a user allergen. */
fun hasAllergenFriendlyMeal(mensa: Mensa, weekday: Int, userAllergens: Set<KnownAllergen>): Boolean {
    val mealTimes = mensa.mealTimesOn(weekday)
    if (mealTimes.isEmpty()) return true
    return mealTimes.any { allergenCompatibleMeals(it, userAllergens).isNotEmpty() }
}

/** True if the mensa has at least one meal on [weekday]. */
fun hasMenuOn(mensa: Mensa, weekday: Int): Boolean = mensa.mealTimesOn(weekday).any { it.meals.isNotEmpty() }

/**
 * Hide empty → search → filters → sort (§6.7). Returns null for "still loading" (skeleton):
 * while nothing has loaded, while the campus filter waits for campus resolution, or while the
 * result is empty but some provider hasn't finished.
 */
fun mensaList(input: PipelineInput): List<Mensa>? {
    val mensas = input.mensas ?: return null
    val filters = input.filters
    val weekday = filters.weekdayOverride ?: input.now.dayOfWeek.value

    var result = mensas.asSequence()
    if (input.hideMensaWithNoMenus) {
        result = result.filter { it.mealTimes.isNotEmpty() && hasMenuOn(it, weekday) }
    }
    val query = input.query.trim().foldForSearch()
    if (query.isNotEmpty()) {
        result = result.filter { query in it.name.foldForSearch() }
    }
    if (filters.allergenFriendly) {
        result = result.filter { hasAllergenFriendlyMeal(it, weekday, input.userAllergens) }
    }
    if (filters.openOnly) {
        result = result.filter { openingState(it, input.now) == OpeningState.OPEN }
    }
    if (filters.campus != null) {
        val campuses = input.campuses ?: return null
        result = result.filter { campuses[it.id] == filters.campus }
    }
    val list = result.toList()
    val sorted = when (filters.sortBy) {
        SortBy.SMART -> list.sortedByDescending { input.clickCounts[it.id] ?: -1 }
        SortBy.NAME -> list.sortedWith { a, b -> nameCollator.compare(a.name, b.name) }
    }
    if (sorted.isEmpty() && !input.allProvidersLoaded) return null
    return sorted
}
