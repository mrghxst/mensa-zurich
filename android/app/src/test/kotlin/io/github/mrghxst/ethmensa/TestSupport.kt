// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa

import io.github.mrghxst.ethmensa.domain.ZURICH
import io.github.mrghxst.ethmensa.domain.model.Meal
import io.github.mrghxst.ethmensa.domain.model.MealTime
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.model.Provider
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The fixtures were recorded on Wednesday 2026-10-28. */
val FIXTURE_DATE: LocalDate = LocalDate.of(2026, 10, 28)

fun fixedClock(dateTime: LocalDateTime = FIXTURE_DATE.atTime(12, 0)): Clock =
    Clock.fixed(dateTime.atZone(ZURICH).toInstant(), ZURICH)

fun fixture(name: String): String =
    requireNotNull(object {}.javaClass.classLoader?.getResource("fixtures/$name")) { "Missing fixture $name" }.readText()

fun meal(
    name: String = "Meal",
    allergens: List<io.github.mrghxst.ethmensa.domain.model.Allergen> = emptyList(),
) = Meal(
    title = "Line",
    name = name,
    description = null,
    imageUrl = null,
    price = null,
    mealTypes = emptyList(),
    meatTypes = emptyList(),
    allergens = allergens,
)

fun mealTime(weekday: Int, start: String? = "11:00", end: String? = "14:00", meals: List<Meal> = listOf(meal())) =
    MealTime(weekday, start?.let(LocalTime::parse), end?.let(LocalTime::parse), "Lunch", meals)

fun mensa(id: Int, name: String = "Mensa $id", provider: Provider = Provider.ETH, mealTimes: List<MealTime> = emptyList()) =
    Mensa(provider, id, name, null, null, null, mealTimes)
