// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/** Menus are for Zürich, so "today" and opening times are computed there, not in the device zone. */
val ZURICH: ZoneId = ZoneId.of("Europe/Zurich")

fun zurichClock(): Clock = Clock.system(ZURICH)

fun today(clock: Clock): LocalDate = LocalDate.now(clock.withZone(ZURICH))

fun nowInZurich(clock: Clock): LocalDateTime = LocalDateTime.now(clock.withZone(ZURICH))

/** ISO weekday of today, 1 = Monday … 7 = Sunday. */
fun todayWeekday(clock: Clock): Int = today(clock).dayOfWeek.value

/** Monday of the ISO week containing [date]. */
fun mondayOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** ISO week key such as "2026-W44", used to invalidate caches. */
fun isoWeekKey(date: LocalDate): String =
    "%d-W%02d".format(date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
