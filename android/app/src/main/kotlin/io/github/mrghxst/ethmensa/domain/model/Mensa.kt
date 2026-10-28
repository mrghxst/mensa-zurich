// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain.model

import androidx.compose.runtime.Immutable
import java.time.LocalTime

enum class Provider(val id: String) {
    ETH("eth"),
    UZH("uzh"),
    ;

    companion object {
        fun fromId(id: String): Provider? = entries.firstOrNull { it.id == id }
    }
}

@Immutable
data class Mensa(
    val provider: Provider,
    val facilityId: Int,
    val name: String,
    val location: String?,
    val webUrl: String?,
    val imageUrl: String?,
    val mealTimes: List<MealTime>,
) {
    /** For example "eth/9" or "uzh/1". */
    val id: String get() = "${provider.id}/$facilityId"
    val shareUrl: String get() = "https://ethmensa.ch/s/$id"

    fun mealTimesOn(weekday: Int): List<MealTime> = mealTimes.filter { it.weekday == weekday }
}

@Immutable
data class MealTime(
    /** ISO weekday, 1 = Monday … 7 = Sunday. */
    val weekday: Int,
    val start: LocalTime?,
    val end: LocalTime?,
    val type: String?,
    val meals: List<Meal>,
)

@Immutable
data class Meal(
    val title: String?,
    val name: String?,
    val description: String?,
    val imageUrl: String?,
    val price: Price?,
    val mealTypes: List<MealType>,
    val meatTypes: List<MeatType>,
    val allergens: List<Allergen>,
    val nutrition: Nutrition? = null,
) {
    /** Copy and share text: title, name and description, one per line. */
    val shareText: String get() = listOfNotNull(title, name, description).joinToString("\n")

    companion object {
        /** If a meal is both vegan and vegetarian, only vegan is kept (iOS rule). */
        fun normalizeMealTypes(types: List<MealType>): List<MealType> {
            val distinct = types.distinct()
            return if (MealType.VEGAN in distinct) distinct - MealType.VEGETARIAN else distinct
        }
    }
}

@Immutable
data class Price(val student: Double?, val staff: Double?, val external: Double?)

/** Nutrition values. ETH sends them per 100 g, ZFV per serving. */
@Immutable
data class Nutrition(
    val basis: Basis,
    val energyKcal: Double? = null,
    val energyKj: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val saturatedFat: Double? = null,
    val carbohydrates: Double? = null,
    val sugar: Double? = null,
    val fibers: Double? = null,
    val salt: Double? = null,
) {
    enum class Basis { PER_100G, PER_SERVING }

    val isEmpty: Boolean
        get() = listOf(energyKcal, energyKj, protein, fat, saturatedFat, carbohydrates, sugar, fibers, salt).all { it == null }
}

enum class MealType { VEGAN, VEGETARIAN, FISH }

enum class MeatType { BEEF, CHICKEN, DUCK, VEAL, TURKEY, PORK, LAMB, GAME }
