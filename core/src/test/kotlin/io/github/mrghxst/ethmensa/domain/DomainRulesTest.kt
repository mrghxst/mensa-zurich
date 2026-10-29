// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain

import io.github.mrghxst.ethmensa.FIXTURE_DATE
import io.github.mrghxst.ethmensa.domain.model.Campus
import io.github.mrghxst.ethmensa.domain.model.LatLng
import io.github.mrghxst.ethmensa.domain.model.Meal
import io.github.mrghxst.ethmensa.domain.model.MealType
import io.github.mrghxst.ethmensa.domain.model.Price
import io.github.mrghxst.ethmensa.domain.model.PriceType
import io.github.mrghxst.ethmensa.fixedClock
import io.github.mrghxst.ethmensa.meal
import io.github.mrghxst.ethmensa.mealTime
import io.github.mrghxst.ethmensa.mensa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OpeningStateTest {
    private val wednesday = mensa(1, mealTimes = listOf(mealTime(3, "11:00", "14:00"), mealTime(3, "17:30", "19:30")))

    private fun at(time: String, date: LocalDate = FIXTURE_DATE) = LocalDateTime.of(date, java.time.LocalTime.parse(time))

    @Test
    fun boundaries() {
        assertEquals(OpeningState.CLOSED, openingState(wednesday, at("10:59")))
        assertEquals(OpeningState.OPEN, openingState(wednesday, at("11:00")))
        assertEquals(OpeningState.OPEN, openingState(wednesday, at("12:30")))
        assertEquals(OpeningState.OPEN, openingState(wednesday, at("14:00")))
        assertEquals(OpeningState.CLOSED, openingState(wednesday, at("14:01")))
        assertEquals(OpeningState.OPEN, openingState(wednesday, at("18:00")))
        assertEquals(OpeningState.CLOSED, openingState(wednesday, at("20:00")))
    }

    @Test
    fun `no meal times today is closed`() {
        assertEquals(OpeningState.CLOSED, openingState(wednesday, at("12:00", FIXTURE_DATE.plusDays(1))))
        assertEquals(OpeningState.CLOSED, openingState(mensa(2), at("12:00")))
    }

    @Test
    fun `missing times are unknown`() {
        val partial = mensa(3, mealTimes = listOf(mealTime(3, "11:00", "14:00"), mealTime(3, null, null)))
        assertEquals(OpeningState.UNKNOWN, openingState(partial, at("12:00")))
    }

    @Test
    fun `sunday uses iso weekday 7`() {
        val sunday = LocalDate.of(2026, 11, 1)
        val open = mensa(4, mealTimes = listOf(mealTime(7, "10:00", "15:00")))
        assertEquals(OpeningState.OPEN, openingState(open, at("12:00", sunday)))
    }

    @Test
    fun `opening hours text`() {
        assertEquals("11:00–14:00, 17:30–19:30", openingHoursText(wednesday.mealTimesOn(3)))
        assertEquals("", openingHoursText(listOf(mealTime(3, null, null))))
    }
}

class PriceFormatterTest {
    @Test
    fun all() {
        assertEquals("8.90/13.90/14.90 CHF", formatPrice(Price(8.9, 13.9, 14.9), PriceType.ALL))
        assertEquals("8.90/–/– CHF", formatPrice(Price(8.9, null, 0.0), PriceType.ALL))
        assertNull(formatPrice(Price(null, null, null), PriceType.ALL))
        assertNull(formatPrice(null, PriceType.ALL))
    }

    @Test
    fun single() {
        val p = Price(null, 13.9, 14.9)
        assertEquals("13.90 CHF", formatPrice(p, PriceType.STUDENT))
        assertEquals("13.90 CHF", formatPrice(p, PriceType.STAFF))
        assertEquals("14.90 CHF", formatPrice(p, PriceType.EXTERNAL))
        assertEquals("14.90 CHF", formatPrice(Price(null, null, 14.9), PriceType.STAFF))
        assertNull(formatPrice(Price(8.9, null, null), PriceType.EXTERNAL))
    }
}

class CampusTest {
    @Test
    fun `nearest within 1000 m`() {
        assertEquals(Campus.ZENTRUM, Campus.nearest(LatLng(47.376205, 8.546737)))
        assertEquals(Campus.HONGG, Campus.nearest(LatLng(47.408341, 8.508015)))
        assertEquals(Campus.IRCHEL, Campus.nearest(LatLng(47.396676, 8.548635)))
        assertNull(Campus.nearest(LatLng(47.562768, 7.580947)))
        // Oerlikon (Octavo) is about 1.5 km from Irchel.
        assertNull(Campus.nearest(LatLng(47.413225, 8.537237)))
    }
}

class MealTextTest {
    @Test
    fun placeholders() {
        assertTrue(MealText.isPlaceholder("novalue", null))
        assertTrue(MealText.isPlaceholder("Pasta", "We look forward to serving you this menu again soon!"))
        assertTrue(MealText.isPlaceholder("Heute GESCHLOSSEN", null))
        assertTrue(MealText.isPlaceholder(null, "Kein Abendessen"))
        assertFalse(MealText.isPlaceholder("Tofu Cashew Bowl", "Rice"))
    }

    @Test
    fun `display title capitalizes all-uppercase titles only`() {
        assertEquals("Wok Street", MealText.displayTitle("WOK STREET"))
        assertEquals("Home Style", MealText.displayTitle("Home Style"))
        assertEquals("garden", MealText.displayTitle("garden"))
    }

    @Test
    fun `search folding`() {
        assertEquals("honggerberg", "Hönggerberg".foldForSearch())
        assertEquals("cafe complet", "Café Complet".foldForSearch())
    }

    @Test
    fun `vegan and vegetarian dedup`() {
        assertEquals(listOf(MealType.VEGAN), Meal.normalizeMealTypes(listOf(MealType.VEGETARIAN, MealType.VEGAN)))
        assertEquals(listOf(MealType.FISH), Meal.normalizeMealTypes(listOf(MealType.FISH, MealType.FISH)))
    }

    @Test
    fun `share text drops nulls`() {
        assertEquals("Line\nMeal", meal().shareText)
    }
}

class WeekdaysTest {
    @Test
    fun weeks() {
        val clock = fixedClock()
        assertEquals(3, todayWeekday(clock))
        assertEquals(LocalDate.of(2026, 10, 26), mondayOf(FIXTURE_DATE))
        assertEquals(LocalDate.of(2026, 10, 26), mondayOf(LocalDate.of(2026, 11, 1)))
        assertEquals("2026-W44", isoWeekKey(FIXTURE_DATE))
        assertEquals("2026-W53", isoWeekKey(LocalDate.of(2027, 1, 1)))
    }

    @Test
    fun `today is computed in Zurich`() {
        // 23:30 UTC on Tuesday is already Wednesday in Zürich (UTC+1 in winter).
        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-27T23:30:00Z"), java.time.ZoneOffset.UTC)
        assertEquals(3, todayWeekday(clock))
    }
}
