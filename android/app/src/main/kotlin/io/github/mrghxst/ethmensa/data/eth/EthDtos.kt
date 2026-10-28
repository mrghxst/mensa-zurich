// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.eth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** glyph.ethz.ch/eth-ws/mensas/detail entry (ETHMensaAnswerLegacy.swift). */
@Serializable
data class EthDirectoryMensa(
    val mensaId: Int? = null,
    val name: String? = null,
    val address: String? = null,
    val web: String? = null,
    val imageUrl: String? = null,
)

/** idapps.ethz.ch weekly rotas (ETHMensaAnswer.swift). */
@Serializable
data class EthRotaAnswer(
    @SerialName("weekly-rota-array") val weeklyRotas: List<EthWeeklyRota>? = null,
)

@Serializable
data class EthWeeklyRota(
    @SerialName("facility-id") val facilityId: Int? = null,
    @SerialName("valid-from") val validFrom: String? = null,
    @SerialName("valid-to") val validTo: String? = null,
    @SerialName("day-of-week-array") val days: List<EthDay>? = null,
)

@Serializable
data class EthDay(
    @SerialName("day-of-week-code") val code: Int? = null,
    @SerialName("opening-hour-array") val openingHours: List<EthOpeningHour>? = null,
)

@Serializable
data class EthOpeningHour(
    @SerialName("meal-time-array") val mealTimes: List<EthMealTime>? = null,
)

@Serializable
data class EthMealTime(
    val name: String? = null,
    @SerialName("time-from") val timeFrom: String? = null,
    @SerialName("time-to") val timeTo: String? = null,
    @SerialName("line-array") val lines: List<EthLine>? = null,
)

@Serializable
data class EthLine(
    val name: String? = null,
    val meal: EthMeal? = null,
)

@Serializable
data class EthMeal(
    val name: String? = null,
    val description: String? = null,
    @SerialName("image-url") val imageUrl: String? = null,
    @SerialName("meal-price-array") val prices: List<EthPrice>? = null,
    @SerialName("meal-class-array") val classes: List<EthCodeDesc>? = null,
    @SerialName("meat-type-array") val meatTypes: List<EthCodeDesc>? = null,
    @SerialName("allergen-array") val allergens: List<EthCodeDesc>? = null,
    val energy: String? = null,
    @SerialName("energy-outdated") val energyKcal: String? = null,
    val protein: String? = null,
    val fat: String? = null,
    @SerialName("saturated-fatty-acids") val saturatedFat: String? = null,
    val carbohydrates: String? = null,
    val sugar: String? = null,
    val salt: String? = null,
)

@Serializable
data class EthPrice(val price: Double? = null)

@Serializable
data class EthCodeDesc(val code: Int? = null, val desc: String? = null)
