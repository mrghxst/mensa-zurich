// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.eth

import io.github.mrghxst.ethmensa.domain.MealText
import io.github.mrghxst.ethmensa.domain.model.Allergen
import io.github.mrghxst.ethmensa.domain.model.Meal
import io.github.mrghxst.ethmensa.domain.model.MealTime
import io.github.mrghxst.ethmensa.domain.model.MealType
import io.github.mrghxst.ethmensa.domain.model.MeatType
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.model.Nutrition
import io.github.mrghxst.ethmensa.domain.model.Price
import io.github.mrghxst.ethmensa.domain.model.Provider
import java.time.LocalDate
import java.time.LocalTime

/** ETHAPI.swift conversion plus the §5.7 data fixes. */
object EthMapper {
    private val FAR_FUTURE: LocalDate = LocalDate.of(2099, 12, 31)

    /**
     * Joins the rotas valid on [today] to the directory by facility id. Directory mensas
     * without a rota are listed without meal times. Rotas of the same facility are merged.
     */
    fun toMensas(directory: List<EthDirectoryMensa>, rotas: EthRotaAnswer, today: LocalDate, menuFallback: String): List<Mensa> {
        val mealTimesByFacility = linkedMapOf<Int, MutableList<MealTime>>()
        rotas.weeklyRotas.orEmpty()
            .filter { isValidOn(it, today) }
            .forEach { rota ->
                val facility = rota.facilityId ?: return@forEach
                mealTimesByFacility.getOrPut(facility) { mutableListOf() } += mealTimes(rota, menuFallback)
            }
        return directory.mapNotNull { entry ->
            val id = entry.mensaId ?: return@mapNotNull null
            val name = entry.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            Mensa(
                provider = Provider.ETH,
                facilityId = id,
                name = name,
                location = entry.address?.trim()?.takeIf { it.isNotEmpty() },
                webUrl = entry.web?.takeIf { it.isNotBlank() },
                imageUrl = entry.imageUrl?.takeIf { it.isNotBlank() },
                mealTimes = mealTimesByFacility[id].orEmpty().sortedBy { it.weekday },
            )
        }
    }

    /** valid-from <= today < valid-to + 1 day; a missing valid-to means 2099-12-31. */
    fun isValidOn(rota: EthWeeklyRota, today: LocalDate): Boolean {
        val from = rota.validFrom?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
        val to = rota.validTo?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: FAR_FUTURE
        return !today.isBefore(from) && today.isBefore(to.plusDays(1))
    }

    private fun mealTimes(rota: EthWeeklyRota, menuFallback: String): List<MealTime> = buildList {
        for (day in rota.days.orEmpty()) {
            val weekday = day.code?.takeIf { it in 1..7 } ?: continue
            for (hour in day.openingHours.orEmpty()) {
                for (mealTime in hour.mealTimes.orEmpty().sortedBy { it.timeFrom ?: "" }) {
                    val meals = mealTime.lines.orEmpty().mapNotNull { mapMeal(it, menuFallback) }
                    if (meals.isEmpty()) continue
                    add(
                        MealTime(
                            weekday = weekday,
                            start = parseTime(mealTime.timeFrom),
                            end = parseTime(mealTime.timeTo),
                            type = mealTime.name?.trim()?.takeIf { it.isNotEmpty() },
                            meals = meals,
                        ),
                    )
                }
            }
        }
    }

    fun mapMeal(line: EthLine, menuFallback: String): Meal? {
        val meal = line.meal ?: return null
        val name = MealText.cleanName(meal.name)
        val description = MealText.cleanEthDescription(meal.description)
        if (MealText.isPlaceholder(name, description)) return null
        val prices = meal.prices.orEmpty()
        return Meal(
            title = MealText.cleanName(line.name) ?: menuFallback,
            name = name,
            description = description,
            imageUrl = meal.imageUrl?.takeIf { it.isNotBlank() }?.let { "$it?client-id=ethz-wcms" },
            price = Price(
                student = prices.getOrNull(0)?.price,
                staff = prices.getOrNull(1)?.price,
                external = prices.getOrNull(2)?.price,
            ),
            mealTypes = Meal.normalizeMealTypes(meal.classes.orEmpty().mapNotNull { mealType(it) }),
            meatTypes = meal.meatTypes.orEmpty().mapNotNull { meatType(it) }.distinct(),
            allergens = meal.allergens.orEmpty().mapNotNull { Allergen.fromEth(it.code, it.desc) }.distinct(),
            nutrition = nutrition(meal),
        )
    }

    private fun mealType(entry: EthCodeDesc): MealType? = when (entry.code) {
        10 -> MealType.FISH
        16 -> MealType.VEGAN
        17 -> MealType.VEGETARIAN
        null -> when (entry.desc?.trim()?.lowercase()) {
            "vegan" -> MealType.VEGAN
            "vegetarian", "vegetarisch" -> MealType.VEGETARIAN
            "fish", "fisch" -> MealType.FISH
            else -> null
        }
        else -> null
    }

    private fun meatType(entry: EthCodeDesc): MeatType? = when (entry.code) {
        10 -> MeatType.BEEF
        11 -> MeatType.VEAL
        12 -> MeatType.PORK
        13 -> MeatType.LAMB
        14 -> MeatType.GAME
        15 -> MeatType.CHICKEN
        16 -> MeatType.TURKEY
        17 -> MeatType.DUCK
        else -> when (entry.desc?.trim()?.lowercase()) {
            "beef", "rind" -> MeatType.BEEF
            "veal", "kalb" -> MeatType.VEAL
            "pork", "schwein" -> MeatType.PORK
            "lamb", "lamm" -> MeatType.LAMB
            "game", "wild" -> MeatType.GAME
            "chicken", "huhn" -> MeatType.CHICKEN
            "turkey", "truthahn" -> MeatType.TURKEY
            "duck", "ente" -> MeatType.DUCK
            else -> null
        }
    }

    private fun nutrition(meal: EthMeal): Nutrition? {
        fun String?.num() = this?.trim()?.replace(',', '.')?.toDoubleOrNull()
        return Nutrition(
            basis = Nutrition.Basis.PER_100G,
            energyKj = meal.energy.num(),
            energyKcal = meal.energyKcal.num(),
            protein = meal.protein.num(),
            fat = meal.fat.num(),
            saturatedFat = meal.saturatedFat.num(),
            carbohydrates = meal.carbohydrates.num(),
            sugar = meal.sugar.num(),
            salt = meal.salt.num(),
        ).takeUnless { it.isEmpty }
    }

    fun parseTime(value: String?): LocalTime? {
        val parts = value?.trim()?.split(":") ?: return null
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return runCatching { LocalTime.of(hour, minute) }.getOrNull()
    }
}
