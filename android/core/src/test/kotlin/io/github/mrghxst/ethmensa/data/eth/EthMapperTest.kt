// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.eth

import io.github.mrghxst.ethmensa.FIXTURE_DATE
import io.github.mrghxst.ethmensa.data.net.ApiJson
import io.github.mrghxst.ethmensa.domain.model.Allergen
import io.github.mrghxst.ethmensa.domain.model.KnownAllergen
import io.github.mrghxst.ethmensa.domain.model.MealType
import io.github.mrghxst.ethmensa.domain.model.MeatType
import io.github.mrghxst.ethmensa.domain.model.Nutrition
import io.github.mrghxst.ethmensa.domain.model.Price
import io.github.mrghxst.ethmensa.fixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EthMapperTest {
    private fun mensas(lang: String, today: LocalDate = FIXTURE_DATE) = EthMapper.toMensas(
        ApiJson.decodeFromString<List<EthDirectoryMensa>>(fixture("eth_directory_$lang.json")),
        ApiJson.decodeFromString<EthRotaAnswer>(fixture("eth_rotas_$lang.json")),
        today,
        "Menu",
    )

    private val allMeals by lazy { mensas("en").flatMap { m -> m.mealTimes.flatMap { it.meals } } }

    @Test
    fun `fixtures parse into every directory mensa`() {
        val en = mensas("en")
        assertEquals(22, en.size)
        assertEquals(22, mensas("de").size)
        assertTrue(en.count { it.mealTimes.isNotEmpty() } >= 10)
        val poly = en.single { it.id == "eth/9" }
        assertEquals("Mensa Polyterrasse", poly.name)
        assertTrue(poly.location!!.contains("\n"))
    }

    @Test
    fun `directory mensas without a rota are still listed`() {
        val withoutMenu = mensas("en").filter { it.mealTimes.isEmpty() }
        assertTrue(withoutMenu.isNotEmpty())
    }

    @Test
    fun `only rotas valid today are used`() {
        // Next week's rotas (from 2026-11-02) must not add a second set of meal times.
        val poly = mensas("en").single { it.id == "eth/9" }
        val wednesdays = poly.mealTimesOn(3)
        assertEquals(wednesdays.map { it.type }.distinct().size, wednesdays.size)
        val nextWeek = mensas("en", LocalDate.of(2026, 11, 3)).single { it.id == "eth/9" }
        assertTrue(nextWeek.mealTimes.isNotEmpty())
    }

    @Test
    fun `rota validity includes the valid-to day and defaults to 2099`() {
        val rota = EthWeeklyRota(validFrom = "2026-10-26", validTo = "2026-11-01")
        assertTrue(EthMapper.isValidOn(rota, LocalDate.of(2026, 10, 26)))
        assertTrue(EthMapper.isValidOn(rota, LocalDate.of(2026, 11, 1)))
        assertFalse(EthMapper.isValidOn(rota, LocalDate.of(2026, 11, 2)))
        assertFalse(EthMapper.isValidOn(rota, LocalDate.of(2026, 10, 25)))
        assertTrue(EthMapper.isValidOn(EthWeeklyRota(validFrom = "2026-10-26"), LocalDate.of(2090, 1, 1)))
    }

    @Test
    fun `placeholder meals are dropped`() {
        assertTrue(allMeals.isNotEmpty())
        assertTrue(allMeals.none { (it.name + it.description).lowercase().contains("novalue") })
    }

    @Test
    fun `meal times are sorted by start and carry times`() {
        val poly = mensas("en").single { it.id == "eth/9" }
        poly.mealTimes.groupBy { it.weekday }.values.forEach { day ->
            assertEquals(day.map { it.start }, day.sortedBy { it.start }.map { it.start })
        }
        assertTrue(poly.mealTimes.all { it.start != null && it.end != null })
    }

    @Test
    fun `names are trimmed and descriptions have no line breaks`() {
        assertTrue(allMeals.all { it.name == null || it.name == it.name.trim() })
        assertTrue(allMeals.all { it.description?.contains('\n') != true })
        assertTrue(allMeals.all { it.description?.contains("  ") != true })
    }

    @Test
    fun `description pipe rules`() {
        val meal = EthMapper.mapMeal(
            EthLine("Line", EthMeal(name = " Bowl ", description = "Rice|\nTofu\nSauce   hot")),
            "Menu",
        )!!
        assertEquals("Bowl", meal.name)
        assertEquals("Rice| Tofu | Sauce hot", meal.description)
    }

    @Test
    fun `image url gets the client id and a missing line name falls back to Menu`() {
        val meal = EthMapper.mapMeal(EthLine(null, EthMeal(name = "A", imageUrl = "https://x/img")), "Menu")!!
        assertEquals("https://x/img?client-id=ethz-wcms", meal.imageUrl)
        assertEquals("Menu", meal.title)
        assertTrue(allMeals.filter { it.imageUrl != null }.all { it.imageUrl!!.endsWith("?client-id=ethz-wcms") })
    }

    @Test
    fun `price ordering with one two and three entries`() {
        fun price(vararg p: Double) = EthMapper.mapMeal(EthLine("L", EthMeal(name = "A", prices = p.map { EthPrice(it) })), "M")!!.price
        assertEquals(Price(8.9, null, null), price(8.9))
        assertEquals(Price(8.9, 13.9, null), price(8.9, 13.9))
        assertEquals(Price(8.9, 13.9, 14.9), price(8.9, 13.9, 14.9))
    }

    @Test
    fun `vegan wins over vegetarian and fish is kept`() {
        val meal = EthMapper.mapMeal(
            EthLine("L", EthMeal(name = "A", classes = listOf(EthCodeDesc(17, "Vegetarian"), EthCodeDesc(16, "Vegan"), EthCodeDesc(14, "Salad")))),
            "M",
        )!!
        assertEquals(listOf(MealType.VEGAN), meal.mealTypes)
        val german = EthMapper.mapMeal(EthLine("L", EthMeal(name = "A", classes = listOf(EthCodeDesc(null, "Vegetarisch"), EthCodeDesc(null, "Fisch")))), "M")!!
        assertEquals(listOf(MealType.VEGETARIAN, MealType.FISH), german.mealTypes)
    }

    @Test
    fun `meat types include lamb and game`() {
        val meal = EthMapper.mapMeal(
            EthLine("L", EthMeal(name = "A", meatTypes = listOf(EthCodeDesc(13, "lamb"), EthCodeDesc(null, "Wild"), EthCodeDesc(15, "chicken")))),
            "M",
        )!!
        assertEquals(listOf(MeatType.LAMB, MeatType.GAME, MeatType.CHICKEN), meal.meatTypes)
    }

    @Test
    fun `milk lactose and german allergens map to known allergens`() {
        assertTrue(allMeals.any { KnownAllergen.LACTOSE in it.allergens })
        assertTrue(allMeals.flatMap { it.allergens }.none { it is Allergen.Other })
        val german = mensas("de").flatMap { m -> m.mealTimes.flatMap { it.meals } }.flatMap { it.allergens }
        assertTrue(german.isNotEmpty())
        assertTrue(german.none { it is Allergen.Other })
        assertEquals(KnownAllergen.LACTOSE, Allergen.fromEth(null, "milk, lactose"))
        assertEquals(KnownAllergen.LACTOSE, Allergen.fromEth(null, "Milch, Laktose"))
        assertEquals(KnownAllergen.NUTS, Allergen.fromEth(null, "Schalenfrüchte"))
        assertEquals(Allergen.Other("Something"), Allergen.fromEth(null, "Something"))
    }

    @Test
    fun `nutrition values per 100 g`() {
        val meal = EthMapper.mapMeal(
            EthLine("L", EthMeal(name = "A", energy = "352", energyKcal = "84", protein = "4.7", salt = "0,74")),
            "M",
        )!!
        val n = meal.nutrition!!
        assertEquals(Nutrition.Basis.PER_100G, n.basis)
        assertEquals(352.0, n.energyKj!!, 0.0)
        assertEquals(84.0, n.energyKcal!!, 0.0)
        assertEquals(0.74, n.salt!!, 0.0)
        assertNull(EthMapper.mapMeal(EthLine("L", EthMeal(name = "A")), "M")!!.nutrition)
    }

    @Test
    fun `time parsing`() {
        assertEquals(LocalTime.of(11, 0), EthMapper.parseTime("11:00"))
        assertEquals(LocalTime.of(7, 30), EthMapper.parseTime(" 07:30 "))
        assertNull(EthMapper.parseTime(""))
        assertNull(EthMapper.parseTime("25:00"))
    }
}
