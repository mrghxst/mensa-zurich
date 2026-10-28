// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain

import io.github.mrghxst.ethmensa.domain.model.MealTime
import io.github.mrghxst.ethmensa.domain.model.Mensa
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class OpeningState { OPEN, CLOSED, UNKNOWN }

/**
 * Mensa+OpeningTimes.swift with ISO weekdays (the iOS version never matches on Sundays).
 * UNKNOWN if any of today's meal times lacks a start or end, OPEN if [now] is inside one
 * (inclusive), CLOSED otherwise, including when there are no meal times today.
 */
fun openingState(mensa: Mensa, now: LocalDateTime): OpeningState {
    val todays = mensa.mealTimesOn(now.dayOfWeek.value)
    if (todays.any { it.start == null || it.end == null }) return OpeningState.UNKNOWN
    val time = now.toLocalTime()
    val open = todays.any { time >= it.start!! && time <= it.end!! }
    return if (open) OpeningState.OPEN else OpeningState.CLOSED
}

private val HOURS = DateTimeFormatter.ofPattern("HH:mm")

fun LocalTime.formatHours(): String = format(HOURS)

/** "11:00–14:00, 17:30–19:30" for the meal times of one weekday that have both times. */
fun openingHoursText(mealTimes: List<MealTime>): String = mealTimes
    .filter { it.start != null && it.end != null }
    .joinToString(", ") { "${it.start!!.formatHours()}–${it.end!!.formatHours()}" }
