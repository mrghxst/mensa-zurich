// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain

import io.github.mrghxst.ethmensa.domain.model.Meal
import io.github.mrghxst.ethmensa.domain.model.MealTime
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.model.Provider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class ShareTextTest {
    private fun meal(name: String?, title: String? = null) =
        Meal(title, name, null, null, null, emptyList(), emptyList(), emptyList())

    private val mensa = Mensa(
        provider = Provider.ETH,
        facilityId = 9,
        name = "Polyterrasse",
        location = null,
        webUrl = null,
        imageUrl = null,
        mealTimes = listOf(
            MealTime(1, LocalTime.of(11, 0), LocalTime.of(13, 30), "Lunch", listOf(meal("Risotto"), meal(null, "Garden"))),
            MealTime(1, LocalTime.of(17, 30), LocalTime.of(19, 30), "Dinner", emptyList()),
            MealTime(2, LocalTime.of(11, 0), LocalTime.of(13, 30), "Lunch", listOf(meal("Curry"))),
        ),
    )

    @Test
    fun `lists the day's hours and dishes`() {
        assertEquals(
            "Polyterrasse\n\nLunch · 11:00–13:30\n– Risotto\n– Garden\n\nDinner · 17:30–19:30",
            mensa.shareText(1),
        )
    }

    @Test
    fun `a day without service is just the name`() {
        assertEquals("Polyterrasse", mensa.shareText(6))
    }
}
