// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.zfv

import kotlinx.serialization.Serializable

/** api.zfv.ch GraphQL response (ZFVMensaAnswer.swift), plus dish stats for nutrition. */
@Serializable
data class ZfvAnswer(val data: ZfvData? = null, val errors: List<ZfvError>? = null)

@Serializable
data class ZfvError(val message: String? = null)

@Serializable
data class ZfvData(val outlets: List<ZfvOutlet>? = null)

@Serializable
data class ZfvOutlet(
    val externalId: String? = null,
    val name: String? = null,
    val location: ZfvLocation? = null,
    val calendar: ZfvCalendar? = null,
)

@Serializable
data class ZfvLocation(val address: ZfvAddress? = null)

@Serializable
data class ZfvAddress(val addressLine1: String? = null, val zipCode: String? = null, val city: String? = null)

@Serializable
data class ZfvCalendar(val week: ZfvWeek? = null)

@Serializable
data class ZfvWeek(val daily: List<ZfvDay>? = null)

@Serializable
data class ZfvDay(val date: ZfvDate? = null, val menuCategories: List<ZfvMenuCategory>? = null)

@Serializable
data class ZfvDate(val dateLocal: String? = null, val weekdayNumber: Int? = null)

@Serializable
data class ZfvMenuCategory(val category: ZfvCategory? = null, val menuItems: List<ZfvMenuItem>? = null)

@Serializable
data class ZfvCategory(val name: String? = null, val slug: String? = null)

@Serializable
data class ZfvMenuItem(
    val dish: ZfvDish? = null,
    val category: ZfvCategory? = null,
    val prices: List<ZfvPrice>? = null,
)

@Serializable
data class ZfvDish(
    val name: String? = null,
    val description: String? = null,
    val media: List<ZfvMediaWrapper>? = null,
    val allergens: List<ZfvAllergenWrapper>? = null,
    val isVegan: Boolean? = null,
    val isVegetarian: Boolean? = null,
    val stats: ZfvStats? = null,
)

@Serializable
data class ZfvMediaWrapper(val media: ZfvMedia? = null)

@Serializable
data class ZfvMedia(val url: String? = null)

@Serializable
data class ZfvAllergenWrapper(val allergen: ZfvAllergen? = null)

@Serializable
data class ZfvAllergen(val externalId: String? = null, val name: String? = null)

@Serializable
data class ZfvPrice(val priceCategory: ZfvPriceCategory? = null, val amount: String? = null)

@Serializable
data class ZfvPriceCategory(val externalId: String? = null)

@Serializable
data class ZfvStats(
    val energy: ZfvAmount? = null,
    val fat: ZfvAmount? = null,
    val carbohydrates: ZfvAmount? = null,
    val sugar: ZfvAmount? = null,
    val fibers: ZfvAmount? = null,
    val protein: ZfvAmount? = null,
    val salt: ZfvAmount? = null,
)

@Serializable
data class ZfvAmount(val amount: Double? = null)

/** zfv_uzh_mapping.json entry (ZFVOutletInfo.swift). */
@Serializable
data class OutletInfo(
    val uzhID: Int? = null,
    val name: String? = null,
    val address: String? = null,
    val lunch: ServiceHours? = null,
    val dinner: ServiceHours? = null,
) {
    val imageUrl: String? get() = uzhID?.let { "https://ziuzhnowweb.uzh.ch/v3/mensa/$it/image" }
}

@Serializable
data class ServiceHours(val start: String? = null, val end: String? = null)
