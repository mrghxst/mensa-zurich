// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.zfv

import io.github.mrghxst.ethmensa.FIXTURE_DATE
import io.github.mrghxst.ethmensa.data.net.ApiJson
import io.github.mrghxst.ethmensa.domain.model.Allergen
import io.github.mrghxst.ethmensa.domain.model.KnownAllergen
import io.github.mrghxst.ethmensa.domain.model.MealType
import io.github.mrghxst.ethmensa.domain.model.Nutrition
import io.github.mrghxst.ethmensa.domain.model.Price
import io.github.mrghxst.ethmensa.domain.model.Provider
import io.github.mrghxst.ethmensa.fixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.OffsetDateTime

class ZfvMapperTest {
    private val labels = ZfvMapper.Labels("Lunch", "Dinner")
    private val infos = ApiJson.decodeFromString<Map<String, OutletInfo>>(fixture("zfv_uzh_mapping.json"))
    private val answer = ApiJson.decodeFromString<ZfvAnswer>(fixture("zfv_en.json"))
    private val outlets = answer.data!!.outlets!!
    private val mensas = ZfvMapper.toMensas(outlets, infos, labels)

    @Test
    fun `fixture parses without errors`() {
        assertNull(answer.errors)
        assertEquals(26, outlets.size)
        assertTrue(mensas.isNotEmpty())
        assertTrue(mensas.all { it.provider == Provider.UZH })
    }

    @Test
    fun `weekday 1 is Monday`() {
        outlets.flatMap { it.calendar?.week?.daily.orEmpty() }.forEach { day ->
            val date = OffsetDateTime.parse(day.date!!.dateLocal!!).toLocalDate()
            assertEquals(date.dayOfWeek.value, day.date.weekdayNumber)
        }
        assertEquals(3, FIXTURE_DATE.dayOfWeek.value)
    }

    @Test
    fun `outlets without meal times are skipped`() {
        val withMeals = outlets.count { o ->
            o.calendar?.week?.daily.orEmpty().any { d -> d.menuCategories.orEmpty().any { c -> c.menuItems.orEmpty().any { it.dish != null } } }
        }
        assertTrue(mensas.size <= withMeals)
        assertTrue(mensas.all { it.mealTimes.isNotEmpty() })
    }

    @Test
    fun `mapping file supplies uzh id, name, image and hours`() {
        val lower = mensas.single { it.id == "uzh/1" }
        assertEquals("Untere Mensa", lower.name)
        assertEquals("https://ziuzhnowweb.uzh.ch/v3/mensa/1/image", lower.imageUrl)
        val lunch = lower.mealTimes.first { it.type == "Lunch" }
        assertEquals(LocalTime.of(11, 0), lunch.start)
        assertEquals(LocalTime.of(14, 30), lunch.end)
        assertEquals("Künstlergasse 10\n8001 Zürich", lower.location)
    }

    @Test
    fun `lunch sorts before other categories and dinner last`() {
        mensas.forEach { mensa ->
            mensa.mealTimes.groupBy { it.weekday }.values.forEach { day ->
                val ranks = day.map { when (it.type) { "Lunch" -> 0; "Dinner" -> 2; else -> 1 } }
                assertEquals(ranks.sorted(), ranks)
            }
        }
        assertEquals(ZfvMapper.ServiceTime.DINNER, ZfvMapper.ServiceTime.fromSlug("abendessen"))
        assertEquals(ZfvMapper.ServiceTime.LUNCH, ZfvMapper.ServiceTime.fromSlug("mittagsverpflegung"))
        assertNull(ZfvMapper.ServiceTime.fromSlug("snacks"))
    }

    @Test
    fun `meal mapping`() {
        val item = ZfvMenuItem(
            dish = ZfvDish(
                name = "PAELLA",
                description = "",
                media = listOf(ZfvMediaWrapper(ZfvMedia("https://img"))),
                allergens = listOf(
                    ZfvAllergenWrapper(ZfvAllergen("wheat", "Wheat")),
                    ZfvAllergenWrapper(ZfvAllergen("cerealsContainingGluten", "Gluten")),
                    ZfvAllergenWrapper(ZfvAllergen("celery", "Celery")),
                    ZfvAllergenWrapper(ZfvAllergen("mystery", "Mystery")),
                ),
                isVegan = true,
                isVegetarian = true,
                stats = ZfvStats(energy = ZfvAmount(509.0), protein = ZfvAmount(12.7)),
            ),
            category = ZfvCategory("garden"),
            prices = listOf(
                ZfvPrice(ZfvPriceCategory("3"), "13"),
                ZfvPrice(ZfvPriceCategory("1"), "6.1"),
                ZfvPrice(ZfvPriceCategory("2"), "9"),
            ),
        )
        val meal = ZfvMapper.mapMeal(item)!!
        assertEquals("garden", meal.title)
        assertEquals("PAELLA", meal.name)
        assertNull(meal.description)
        assertEquals("https://img", meal.imageUrl)
        assertEquals(Price(6.1, 9.0, 13.0), meal.price)
        assertEquals(listOf(MealType.VEGAN), meal.mealTypes)
        assertEquals(
            listOf(KnownAllergen.GLUTEN, KnownAllergen.WHEAT, KnownAllergen.CELERY, Allergen.Other("Mystery")),
            meal.allergens,
        )
        assertEquals(Nutrition.Basis.PER_SERVING, meal.nutrition!!.basis)
        assertEquals(509.0, meal.nutrition.energyKcal!!, 0.0)
    }

    @Test
    fun `zero prices mean no price and placeholders are dropped`() {
        val free = ZfvMapper.mapMeal(ZfvMenuItem(ZfvDish(name = "Pasta"), prices = listOf(ZfvPrice(ZfvPriceCategory("1"), "0"))))!!
        assertNull(free.price)
        assertNull(ZfvMapper.mapMeal(ZfvMenuItem(ZfvDish(name = "Geschlossen"))))
        assertNull(ZfvMapper.mapMeal(ZfvMenuItem(dish = null)))
    }

    @Test
    fun `zfv allergen ids`() {
        assertEquals(listOf(KnownAllergen.NUTS), Allergen.fromZfv("pistachio", null))
        assertEquals(listOf(KnownAllergen.SOYA), Allergen.fromZfv("soybeans", null))
        assertEquals(listOf(KnownAllergen.LACTOSE), Allergen.fromZfv("milk", "Milk"))
        assertEquals(listOf(KnownAllergen.SULPHUR_DIOXIDE), Allergen.fromZfv("sulphites", null))
        assertEquals(listOf(Allergen.Other("odd")), Allergen.fromZfv("odd", null))
    }
}
