// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.geo

import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.mrghxst.ethmensa.data.net.ApiJson
import io.github.mrghxst.ethmensa.domain.model.Campus
import io.github.mrghxst.ethmensa.domain.model.LatLng
import io.github.mrghxst.ethmensa.domain.model.Mensa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlin.coroutines.resume

/** Looks up coordinates for addresses that aren't bundled. */
fun interface AddressGeocoder {
    suspend fun geocode(address: String): LatLng?
}

/**
 * Mensa coordinates: bundled `mensa_coordinates.json` first, then the address cache in
 * DataStore, then the platform [AddressGeocoder] (cached afterwards). "Reset maps cache"
 * clears only the DataStore cache.
 */
class MensaLocator(
    private val bundledJson: () -> String?,
    private val cache: DataStore<Preferences>,
    private val geocoder: AddressGeocoder?,
) {
    @Serializable
    private data class Point(val lat: Double, val lng: Double)

    @Volatile
    private var bundledPoints: Map<String, LatLng>? = null

    // Read off the main thread: callers include MensaStore on the main-thread app scope.
    private suspend fun bundled(): Map<String, LatLng> = bundledPoints ?: withContext(Dispatchers.IO) {
        bundledJson()
            ?.let { runCatching { ApiJson.decodeFromString<Map<String, Point>>(it) }.getOrNull() }
            ?.mapValues { LatLng(it.value.lat, it.value.lng) }
            .orEmpty()
    }.also { bundledPoints = it }

    suspend fun locate(mensa: Mensa): LatLng? {
        bundled()[mensa.id]?.let { return it }
        val address = mensa.location?.geocodingQuery() ?: return null
        val key = stringPreferencesKey(address)
        cache.data.first()[key]?.let { cached ->
            val (lat, lng) = cached.split(",").map { it.toDouble() }
            return LatLng(lat, lng)
        }
        val found = geocoder?.geocode(address) ?: return null
        cache.edit { it[key] = "${found.lat},${found.lng}" }
        return found
    }

    /** Campus per mensa id (nearest campus within 1000 m). */
    suspend fun campuses(mensas: List<Mensa>): Map<String, Campus?> =
        mensas.associate { mensa -> mensa.id to locate(mensa)?.let(Campus::nearest) }

    suspend fun clearCache() {
        cache.edit { it.clear() }
    }
}

/** Line breaks and zero-width spaces out, so the address reads as one line. */
fun String.geocodingQuery(): String =
    replace("​", "").split("\n").map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")

/** Android [Geocoder], using the async API on 33+. */
class PlatformGeocoder(private val geocoder: Geocoder) : AddressGeocoder {
    override suspend fun geocode(address: String): LatLng? {
        if (!Geocoder.isPresent()) return null
        val result: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(
                        address,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses.firstOrNull())

                            override fun onError(errorMessage: String?) = cont.resume(null)
                        },
                    )
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(address, 1)?.firstOrNull()
                }.onFailure { Log.w("PlatformGeocoder", "Geocoding failed", it) }.getOrNull()
            }
        }
        return result?.let { LatLng(it.latitude, it.longitude) }
    }
}
