// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain

import io.github.mrghxst.ethmensa.domain.model.Price
import io.github.mrghxst.ethmensa.domain.model.PriceType
import java.util.Locale

private fun Double.chf(): String = String.format(Locale.ROOT, "%.2f", this)

/**
 * Price.getString from iOS. ALL prints "student/staff/external CHF" with "–" for a missing or
 * zero value (iOS prints NaN). Returns null when there is nothing to show.
 */
fun formatPrice(price: Price?, type: PriceType): String? {
    if (price == null) return null
    val value = when (type) {
        PriceType.ALL -> {
            val parts = listOf(price.student, price.staff, price.external)
            if (parts.all { it == null || it == 0.0 }) return null
            return parts.joinToString("/") { p -> if (p == null || p == 0.0) "–" else p.chf() } + " CHF"
        }
        PriceType.STUDENT -> price.student ?: price.staff ?: price.external
        PriceType.STAFF -> price.staff ?: price.external
        PriceType.EXTERNAL -> price.external
    }
    return value?.let { "${it.chf()} CHF" }
}
