// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLng(val lat: Double, val lng: Double)

/** The three campuses from Campus.swift. */
enum class Campus(val id: String, val center: LatLng) {
    ZENTRUM("zentrum", LatLng(47.377399461702936, 8.548341273815552)),
    HONGG("hongg", LatLng(47.40862508665355, 8.50779324765029)),
    IRCHEL("irchel", LatLng(47.39747781967301, 8.549434449904433)),
    ;

    companion object {
        const val ASSIGN_RADIUS_METERS = 1000.0

        fun fromId(id: String?): Campus? = entries.firstOrNull { it.id == id }

        /** The nearest campus within 1000 m, or null. */
        fun nearest(point: LatLng): Campus? = entries
            .map { it to distanceMeters(point, it.center) }
            .filter { it.second <= ASSIGN_RADIUS_METERS }
            .minByOrNull { it.second }
            ?.first
    }
}

/** Haversine distance in meters. */
fun distanceMeters(a: LatLng, b: LatLng): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
    return 2 * r * asin(sqrt(h))
}
