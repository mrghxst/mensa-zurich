// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.map

import io.github.mrghxst.ethmensa.data.net.ApiJson
import io.github.mrghxst.ethmensa.data.net.InvalidResponseException
import io.github.mrghxst.ethmensa.data.net.fetchBytes
import io.github.mrghxst.ethmensa.domain.model.LatLng
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/** A tile in the Web Mercator grid. */
data class TileId(val z: Int, val x: Int, val y: Int)

/** Web Mercator position of [point] at zoom [z], in tiles: the integer part is the tile. */
fun mercatorTile(point: LatLng, z: Int): Pair<Double, Double> {
    val scale = (1 shl z).toDouble()
    val lat = Math.toRadians(point.lat)
    val x = (point.lng + 180.0) / 360.0 * scale
    val y = (1.0 - ln(tan(lat) + 1.0 / cos(lat)) / PI) / 2.0 * scale
    return x to y
}

/** The tiles at zoom [z] that touch the square of half-side [halfSpan] tiles around ([x], [y]). */
fun tilesCovering(x: Double, y: Double, halfSpan: Double, z: Int): List<TileId> {
    val xs = floor(x - halfSpan).toInt()..floor(x + halfSpan).toInt()
    val ys = floor(y - halfSpan).toInt()..floor(y + halfSpan).toInt()
    return ys.flatMap { ty -> xs.map { tx -> TileId(z, tx, ty) } }
}

/**
 * Vector tiles from OpenFreeMap (free, no key, https://openfreemap.org) for the map thumbnails.
 * Tiles go through the shared HTTP cache, which keeps them for as long as the server allows
 * (years), and fall back to it when offline. Decoded tiles stay in memory for the session.
 */
class MapTileRepository(
    private val http: OkHttpClient,
    private val tileJsonUrl: String = "https://tiles.openfreemap.org/planet",
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()
    private var template: String? = null
    private val decoded = object : LinkedHashMap<TileId, VectorTile>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<TileId, VectorTile>) = size > MAX_TILES_IN_MEMORY
    }

    /** Throws [IOException] when the tile can't be loaded from the network or the cache. */
    suspend fun tile(id: TileId): VectorTile {
        mutex.withLock { decoded[id] }?.let { return it }
        val url = template().replace("{z}", "${id.z}").replace("{x}", "${id.x}").replace("{y}", "${id.y}")
        val tile = withContext(dispatcher) {
            val bytes = fetchPreferringNetwork(url)
            try {
                VectorTile.decode(bytes, LAYERS)
            } catch (e: IllegalArgumentException) {
                throw InvalidResponseException("Bad vector tile $id: ${e.message}")
            }
        }
        mutex.withLock { decoded[id] = tile }
        return tile
    }

    private suspend fun template(): String {
        mutex.withLock { template }?.let { return it }
        val json = String(fetchPreferringNetwork(tileJsonUrl), Charsets.UTF_8)
        val url = runCatching {
            ApiJson.parseToJsonElement(json).jsonObject["tiles"]!!.jsonArray.first().jsonPrimitive.content
        }.getOrNull() ?: throw InvalidResponseException("No tile URL in $tileJsonUrl")
        mutex.withLock { template = url }
        return url
    }

    private suspend fun fetchPreferringNetwork(url: String): ByteArray = try {
        http.fetchBytes(Request.Builder().url(url).build())
    } catch (e: IOException) {
        try {
            http.fetchBytes(Request.Builder().url(url).cacheControl(CacheControl.FORCE_CACHE).build())
        } catch (_: IOException) {
            throw e
        }
    }

    companion object {
        /** OpenFreeMap serves the OpenMapTiles schema up to zoom 14 and overzooms beyond it. */
        const val MAX_ZOOM = 14

        /** The OpenMapTiles layers the thumbnail draws. */
        val LAYERS = setOf("water", "park", "landcover", "building", "transportation")

        private const val MAX_TILES_IN_MEMORY = 8
    }
}
