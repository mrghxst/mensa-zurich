// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.notifications

import io.github.mrghxst.ethmensa.domain.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class RemindersTest {
    private val zone = ZoneId.of("Europe/Zurich")
    private fun at(day: Int, hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone)

    @Test
    fun `later today on a weekday`() {
        // 2026-10-29 is a Thursday.
        assertEquals(at(29, 11), nextWeekdayAt(at(29, 9), LocalTime.of(11, 0)))
    }

    @Test
    fun `exactly now or past moves to the next weekday`() {
        assertEquals(at(30, 11), nextWeekdayAt(at(29, 11), LocalTime.of(11, 0)))
        assertEquals(at(30, 11), nextWeekdayAt(at(29, 12), LocalTime.of(11, 0)))
    }

    @Test
    fun `friday afternoon and weekends go to monday`() {
        val monday = ZonedDateTime.of(2026, 11, 2, 11, 0, 0, 0, zone)
        assertEquals(monday, nextWeekdayAt(at(30, 12), LocalTime.of(11, 0)))
        assertEquals(monday, nextWeekdayAt(at(31, 9), LocalTime.of(11, 0)))
        assertEquals(monday, nextWeekdayAt(ZonedDateTime.of(2026, 11, 1, 23, 0, 0, 0, zone), LocalTime.of(11, 0)))
    }

    @Test
    fun `daylight saving change keeps the wall time`() {
        // Europe/Zurich leaves DST on Sunday 2026-10-25; Friday 23rd evening → Monday 26th 11:00 CET.
        val friday = ZonedDateTime.of(2026, 10, 23, 18, 0, 0, 0, zone)
        assertEquals(LocalTime.of(11, 0), nextWeekdayAt(friday, LocalTime.of(11, 0)).toLocalTime())
        assertEquals(26, nextWeekdayAt(friday, LocalTime.of(11, 0)).dayOfMonth)
    }

    @Test
    fun `arming follows notifications and the campus flag`() {
        assertFalse(shouldArmReminder(Settings(notifications = false), insideCampus = true))
        assertTrue(shouldArmReminder(Settings(notifications = true), insideCampus = false))
        assertFalse(shouldArmReminder(Settings(notifications = true, locationNotifications = true), insideCampus = false))
        assertTrue(shouldArmReminder(Settings(notifications = true, locationNotifications = true), insideCampus = true))
    }
}
