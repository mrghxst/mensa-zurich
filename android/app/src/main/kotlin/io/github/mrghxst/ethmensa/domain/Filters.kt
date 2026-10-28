// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain

import io.github.mrghxst.ethmensa.domain.model.Campus
import io.github.mrghxst.ethmensa.domain.model.MensaShowType
import io.github.mrghxst.ethmensa.domain.model.SortBy

/** Filter state from MensaFilter.swift. showType, campus and sortBy are persisted, the rest is session state. */
data class ListFilters(
    val showType: MensaShowType = MensaShowType.ALL,
    val campus: Campus? = null,
    /** ISO weekday override, null = today. */
    val weekdayOverride: Int? = null,
    val allergenFriendly: Boolean = false,
    val sortBy: SortBy = SortBy.SMART,
) {
    /** "Open only" applies only while there is no weekday override. */
    val openOnly: Boolean get() = showType == MensaShowType.OPEN && weekdayOverride == null

    val activeFilters: List<ActiveFilter>
        get() = buildList {
            if (openOnly) add(ActiveFilter.OpenOnly)
            if (campus != null) add(ActiveFilter.CampusFilter(campus))
            if (weekdayOverride != null) add(ActiveFilter.Weekday(weekdayOverride))
            if (allergenFriendly) add(ActiveFilter.AllergenFriendly)
            if (sortBy != SortBy.SMART) add(ActiveFilter.Sorting(sortBy))
        }

    val isAnyActive: Boolean get() = activeFilters.isNotEmpty()

    fun without(filter: ActiveFilter): ListFilters = when (filter) {
        ActiveFilter.OpenOnly -> copy(showType = MensaShowType.ALL)
        is ActiveFilter.CampusFilter -> copy(campus = null)
        is ActiveFilter.Weekday -> copy(weekdayOverride = null)
        ActiveFilter.AllergenFriendly -> copy(allergenFriendly = false)
        is ActiveFilter.Sorting -> copy(sortBy = SortBy.SMART)
    }

    fun reset(): ListFilters = ListFilters()

    /** Choosing today clears the override. */
    fun withWeekday(weekday: Int?, today: Int): ListFilters =
        copy(weekdayOverride = weekday?.takeIf { it != today })
}

/** A removable chip, in display order. */
sealed interface ActiveFilter {
    val key: String

    data object OpenOnly : ActiveFilter {
        override val key = "open"
    }

    data class CampusFilter(val campus: Campus) : ActiveFilter {
        override val key = "campus"
    }

    data class Weekday(val weekday: Int) : ActiveFilter {
        override val key = "weekday"
    }

    data object AllergenFriendly : ActiveFilter {
        override val key = "allergen"
    }

    data class Sorting(val sortBy: SortBy) : ActiveFilter {
        override val key = "sort"
    }
}
