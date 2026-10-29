// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.zfv

import io.github.mrghxst.ethmensa.data.eth.EthMapper
import io.github.mrghxst.ethmensa.domain.MealText
import io.github.mrghxst.ethmensa.domain.model.Allergen
import io.github.mrghxst.ethmensa.domain.model.Meal
import io.github.mrghxst.ethmensa.domain.model.MealTime
import io.github.mrghxst.ethmensa.domain.model.MealType
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.model.Nutrition
import io.github.mrghxst.ethmensa.domain.model.Price
import io.github.mrghxst.ethmensa.domain.model.Provider

/** ZFVConverter.swift plus the §5.7 data fixes. */
object ZfvMapper {
    enum class ServiceTime {
        LUNCH,
        DINNER,
        ;

        companion object {
            fun fromSlug(slug: String?): ServiceTime? = when {
                slug == null -> null
                "abend" in slug || "dinner" in slug -> DINNER
                "mittag" in slug || "lunch" in slug -> LUNCH
                else -> null
            }

            /** Lunch first, dinner last, other categories in between. */
            fun rank(slug: String?): Int = when (fromSlug(slug)) {
                LUNCH -> 0
                null -> 1
                DINNER -> 2
            }
        }
    }

    /** Localized "Lunch" and "Dinner" labels. */
    data class Labels(val lunch: String, val dinner: String)

    fun toMensas(outlets: List<ZfvOutlet>, infos: Map<String, OutletInfo>, labels: Labels): List<Mensa> =
        outlets.mapNotNull { outlet -> toMensa(outlet, outlet.externalId?.let { infos[it] }, labels) }

    fun toMensa(outlet: ZfvOutlet, info: OutletInfo?, labels: Labels): Mensa? {
        val name = outlet.name ?: return null
        val externalId = outlet.externalId?.toIntOrNull() ?: return null
        val address = outlet.location?.address
        val cityLine = listOfNotNull(address?.zipCode, address?.city).joinToString(" ")
        val location = listOfNotNull(address?.addressLine1, cityLine.ifEmpty { null }).joinToString("\n")
        val mealTimes = outlet.calendar?.week?.daily.orEmpty().flatMap { mealTimes(it, info, labels) }
        // Outlets without dishes this week (cafés, shops) are left out.
        if (mealTimes.isEmpty()) return null
        return Mensa(
            provider = Provider.UZH,
            facilityId = info?.uzhID ?: externalId,
            name = info?.name ?: name.trim(),
            location = location.ifEmpty { null } ?: info?.address,
            webUrl = null,
            imageUrl = info?.imageUrl,
            mealTimes = mealTimes,
        )
    }

    private fun mealTimes(day: ZfvDay, info: OutletInfo?, labels: Labels): List<MealTime> {
        val weekday = day.date?.weekdayNumber?.takeIf { it in 1..7 } ?: return emptyList()
        return day.menuCategories.orEmpty()
            .sortedBy { ServiceTime.rank(it.category?.slug) }
            .mapNotNull { category ->
                val meals = category.menuItems.orEmpty().mapNotNull { mapMeal(it) }
                if (meals.isEmpty()) return@mapNotNull null
                val service = ServiceTime.fromSlug(category.category?.slug)
                val hours = when (service) {
                    ServiceTime.LUNCH -> info?.lunch
                    ServiceTime.DINNER -> info?.dinner
                    null -> null
                }
                MealTime(
                    weekday = weekday,
                    start = EthMapper.parseTime(hours?.start),
                    end = EthMapper.parseTime(hours?.end),
                    type = when (service) {
                        ServiceTime.LUNCH -> labels.lunch
                        ServiceTime.DINNER -> labels.dinner
                        null -> category.category?.name
                    },
                    meals = meals,
                )
            }
    }

    fun mapMeal(item: ZfvMenuItem): Meal? {
        val dish = item.dish ?: return null
        val name = MealText.cleanName(dish.name)
        val description = MealText.cleanDescription(dish.description)
        if (MealText.isPlaceholder(name, description)) return null
        val prices = item.prices.orEmpty()
        fun price(id: String) = prices.firstOrNull { it.priceCategory?.externalId == id }?.amount?.trim()?.toDoubleOrNull()
        val student = price("1")
        val staff = price("2")
        val external = price("3")
        val hasPrice = listOf(student, staff, external).any { (it ?: 0.0) > 0.0 }
        val mealTypes = when {
            dish.isVegan == true -> listOf(MealType.VEGAN)
            dish.isVegetarian == true -> listOf(MealType.VEGETARIAN)
            else -> emptyList()
        }
        val allergens = dish.allergens.orEmpty().flatMap { wrapper ->
            val id = wrapper.allergen?.externalId ?: return@flatMap emptyList<Allergen>()
            Allergen.fromZfv(id, wrapper.allergen.name)
        }.distinct()
        val stats = dish.stats
        val nutrition = stats?.let {
            Nutrition(
                basis = Nutrition.Basis.PER_SERVING,
                energyKcal = it.energy?.amount,
                protein = it.protein?.amount,
                fat = it.fat?.amount,
                carbohydrates = it.carbohydrates?.amount,
                sugar = it.sugar?.amount,
                fibers = it.fibers?.amount,
                salt = it.salt?.amount,
            )
        }?.takeUnless { it.isEmpty }
        return Meal(
            title = MealText.cleanName(item.category?.name),
            name = name,
            description = description,
            imageUrl = dish.media?.firstOrNull()?.media?.url?.takeIf { it.isNotBlank() },
            price = if (hasPrice) Price(student, staff, external) else null,
            mealTypes = mealTypes,
            meatTypes = emptyList(),
            allergens = allergens,
            nutrition = nutrition,
        )
    }
}
