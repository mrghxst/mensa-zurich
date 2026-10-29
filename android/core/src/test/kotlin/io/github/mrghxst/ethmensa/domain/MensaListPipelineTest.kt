// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain

import io.github.mrghxst.ethmensa.FIXTURE_DATE
import io.github.mrghxst.ethmensa.domain.model.Campus
import io.github.mrghxst.ethmensa.domain.model.KnownAllergen
import io.github.mrghxst.ethmensa.domain.model.MensaShowType
import io.github.mrghxst.ethmensa.domain.model.SortBy
import io.github.mrghxst.ethmensa.meal
import io.github.mrghxst.ethmensa.mealTime
import io.github.mrghxst.ethmensa.mensa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MensaListPipelineTest {
    private val noon = FIXTURE_DATE.atTime(12, 0) // Wednesday
    private val glutenOnly = listOf(meal("Bread", listOf(KnownAllergen.GLUTEN)))

    private val poly = mensa(9, "Mensa Polyterrasse", mealTimes = listOf(mealTime(3, "11:00", "13:30")))
    private val clausius = mensa(3, "Clausiusbar", mealTimes = listOf(mealTime(3, "11:00", "11:30", glutenOnly), mealTime(4)))
    private val hongg = mensa(15, "Bellavista Hönggerberg", mealTimes = listOf(mealTime(4)))
    private val empty = mensa(4, "Kiosk CLA")
    private val all = listOf(poly, clausius, hongg, empty)
    private val campuses = mapOf("eth/9" to Campus.ZENTRUM, "eth/3" to Campus.ZENTRUM, "eth/15" to Campus.HONGG, "eth/4" to Campus.ZENTRUM)

    private fun run(
        filters: ListFilters = ListFilters(),
        query: String = "",
        hideEmpty: Boolean = false,
        clicks: Map<String, Int> = emptyMap(),
        mensas: List<io.github.mrghxst.ethmensa.domain.model.Mensa>? = all,
        loaded: Boolean = true,
        resolved: Map<String, Campus?>? = campuses,
    ) = mensaList(
        PipelineInput(
            mensas = mensas,
            allProvidersLoaded = loaded,
            query = query,
            filters = filters,
            hideMensaWithNoMenus = hideEmpty,
            userAllergens = setOf(KnownAllergen.GLUTEN),
            clickCounts = clicks,
            campuses = resolved,
            now = noon,
        ),
    )?.map { it.facilityId }

    @Test
    fun `no filters keeps delivery order`() {
        assertEquals(listOf(9, 3, 15, 4), run())
    }

    @Test
    fun `hide empty uses the list weekday`() {
        assertEquals(listOf(9, 3), run(hideEmpty = true))
        assertEquals(listOf(3, 15), run(hideEmpty = true, filters = ListFilters(weekdayOverride = 4)))
    }

    @Test
    fun `search ignores case and diacritics`() {
        assertEquals(listOf(15), run(query = "honggerberg"))
        assertEquals(listOf(15), run(query = "HÖNGG"))
        assertEquals(listOf(9), run(query = " poly "))
    }

    @Test
    fun `allergen friendly`() {
        // Clausiusbar only serves gluten on Wednesday; mensas without meal times that day pass.
        assertEquals(listOf(9, 15, 4), run(filters = ListFilters(allergenFriendly = true)))
    }

    @Test
    fun `open only applies without weekday override`() {
        assertEquals(listOf(9), run(filters = ListFilters(showType = MensaShowType.OPEN)))
        assertEquals(listOf(9, 3, 15, 4), run(filters = ListFilters(showType = MensaShowType.OPEN, weekdayOverride = 4)))
    }

    @Test
    fun `campus filter waits for resolution`() {
        assertEquals(listOf(15), run(filters = ListFilters(campus = Campus.HONGG)))
        assertNull(run(filters = ListFilters(campus = Campus.HONGG), resolved = null))
        assertEquals(listOf(9, 3, 15, 4), run(resolved = null))
    }

    @Test
    fun `combined filters`() {
        assertEquals(
            listOf(9),
            run(filters = ListFilters(campus = Campus.ZENTRUM, allergenFriendly = true), hideEmpty = true),
        )
    }

    @Test
    fun `smart sort by clicks is stable and never-opened counts as -1`() {
        assertEquals(listOf(15, 3, 9, 4), run(clicks = mapOf("eth/15" to 5, "eth/3" to 2, "eth/9" to 0)))
        assertEquals(listOf(3, 9, 15, 4), run(clicks = mapOf("eth/3" to 1)))
    }

    @Test
    fun `name sort uses collation`() {
        assertEquals(listOf(15, 3, 4, 9), run(filters = ListFilters(sortBy = SortBy.NAME)))
    }

    @Test
    fun `loading states`() {
        assertNull(run(mensas = null))
        assertNull(run(query = "nothing", loaded = false))
        assertEquals(emptyList<Int>(), run(query = "nothing", loaded = true))
    }
}

class ListFiltersTest {
    @Test
    fun `chips in order and removal`() {
        val filters = ListFilters(MensaShowType.OPEN, Campus.IRCHEL, null, true, SortBy.NAME)
        assertEquals(listOf("open", "campus", "allergen", "sort"), filters.activeFilters.map { it.key })
        val withDay = filters.copy(weekdayOverride = 5)
        // Open only disappears while a weekday is selected.
        assertEquals(listOf("campus", "weekday", "allergen", "sort"), withDay.activeFilters.map { it.key })
        assertEquals(null, withDay.without(ActiveFilter.Weekday(5)).weekdayOverride)
        assertEquals(ListFilters(), filters.reset())
    }

    @Test
    fun `choosing today clears the override`() {
        assertEquals(null, ListFilters(weekdayOverride = 5).withWeekday(3, today = 3).weekdayOverride)
        assertEquals(5, ListFilters().withWeekday(5, today = 3).weekdayOverride)
    }
}
