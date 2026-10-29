// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain

import io.github.mrghxst.ethmensa.FIXTURE_DATE
import io.github.mrghxst.ethmensa.domain.model.DietFilter
import io.github.mrghxst.ethmensa.domain.model.MealType
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.model.SortBy
import io.github.mrghxst.ethmensa.meal
import io.github.mrghxst.ethmensa.mealTime
import io.github.mrghxst.ethmensa.mensa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Android extras from MensaZH: favorites, hidden mensas and the diet filter. */
class PipelineExtrasTest {
    private val noon = FIXTURE_DATE.atTime(12, 0) // Wednesday = 3
    private val vegan = meal("Tofu").copy(mealTypes = listOf(MealType.VEGAN))
    private val veggie = meal("Rösti").copy(mealTypes = listOf(MealType.VEGETARIAN))
    private val meat = meal("Steak")

    private val a = mensa(1, "Archimedes", mealTimes = listOf(mealTime(3, meals = listOf(meat))))
    private val b = mensa(2, "Bellavista", mealTimes = listOf(mealTime(3, meals = listOf(veggie, meat))))
    private val c = mensa(3, "Clausiusbar", mealTimes = listOf(mealTime(3, meals = listOf(vegan)), mealTime(4, meals = listOf(meat))))
    private val all = listOf(a, b, c)

    private fun run(
        filters: ListFilters = ListFilters(sortBy = SortBy.NAME),
        favorites: Set<String> = emptySet(),
        hidden: Set<String> = emptySet(),
        mensas: List<Mensa> = all,
    ) = mensaList(
        PipelineInput(
            mensas = mensas,
            allProvidersLoaded = true,
            filters = filters,
            now = noon,
            favorites = favorites,
            hidden = hidden,
        ),
    )?.map { it.facilityId }

    @Test
    fun `vegan meals count as vegetarian`() {
        assertTrue(vegan.fits(DietFilter.VEGETARIAN))
        assertTrue(vegan.fits(DietFilter.VEGAN))
        assertTrue(veggie.fits(DietFilter.VEGETARIAN))
        assertFalse(veggie.fits(DietFilter.VEGAN))
        assertFalse(meat.fits(DietFilter.VEGETARIAN))
        assertTrue(meat.fits(DietFilter.NONE))
    }

    @Test
    fun `diet filter keeps mensas with a fitting meal on the list day`() {
        assertEquals(listOf(2, 3), run(ListFilters(sortBy = SortBy.NAME, diet = DietFilter.VEGETARIAN)))
        assertEquals(listOf(3), run(ListFilters(sortBy = SortBy.NAME, diet = DietFilter.VEGAN)))
        // Thursday: Clausiusbar only has meat.
        assertEquals(emptyList<Int>(), run(ListFilters(sortBy = SortBy.NAME, weekdayOverride = 4, diet = DietFilter.VEGAN)))
    }

    @Test
    fun `hidden mensas are left out`() {
        assertEquals(listOf(1, 3), run(hidden = setOf("eth/2")))
    }

    @Test
    fun `favorites come first and keep the chosen order among themselves`() {
        assertEquals(listOf(2, 3, 1), run(favorites = setOf("eth/3", "eth/2")))
        assertEquals(listOf(3, 1, 2), run(favorites = setOf("eth/3")))
    }

    @Test
    fun `diet chip can be removed`() {
        val filters = ListFilters(diet = DietFilter.VEGAN)
        assertEquals(listOf(ActiveFilter.Diet(DietFilter.VEGAN)), filters.activeFilters)
        assertEquals(DietFilter.NONE, filters.without(ActiveFilter.Diet(DietFilter.VEGAN)).diet)
    }
}
