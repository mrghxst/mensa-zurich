// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.map

import io.github.mrghxst.ethmensa.domain.model.Campus
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream

class VectorTileTest {
    @Test
    fun decodesPolygonsAndLinesWithTheirClass() {
        val tile = VectorTile.decode(sampleTile())
        val building = tile.layers.getValue("building")
        assertEquals(4096, building.extent)
        val square = building.features.single()
        assertEquals(VectorTile.GeometryType.POLYGON, square.type)
        assertNull(square.kind)
        // ClosePath repeats the first point.
        assertArrayEquals(intArrayOf(10, 10, 20, 10, 20, 20, 10, 10), square.parts.single())

        val road = tile.layers.getValue("transportation").features.single()
        assertEquals(VectorTile.GeometryType.LINE, road.type)
        assertEquals("primary", road.kind)
        // A second MoveTo starts a second line.
        assertEquals(2, road.parts.size)
        assertArrayEquals(intArrayOf(0, 0, 100, 0), road.parts[0])
        assertArrayEquals(intArrayOf(50, 50, 50, 150), road.parts[1])
    }

    @Test
    fun skipsLayersNotAskedFor() {
        val tile = VectorTile.decode(sampleTile(), only = setOf("transportation"))
        assertEquals(setOf("transportation"), tile.layers.keys)
    }

    @Test
    fun truncatedTileIsRejected() {
        val bytes = sampleTile()
        assertThrows(IllegalArgumentException::class.java) { VectorTile.decode(bytes.copyOf(bytes.size - 3)) }
    }

    @Test
    fun tileMathMatchesWebMercator() {
        val (x, y) = mercatorTile(Campus.HONGG.center, 14)
        assertEquals(8579.199, x, 0.001)
        assertEquals(5735.309, y, 0.001)
        assertEquals(listOf(TileId(14, 8579, 5735)), tilesCovering(x, y, 0.05, 14))
        // Near a corner, the square touches four tiles.
        assertEquals(4, tilesCovering(8579.98, 5735.98, 0.05, 14).size)
    }

    @Test
    fun repositoryReadsTheTileUrlFromTileJsonAndKeepsDecodedTiles() = runTest {
        val server = MockWebServer()
        val requests = mutableListOf<String>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                synchronized(requests) { requests += path }
                return when (path) {
                    "/planet" -> MockResponse.Builder()
                        .body("""{"tiles":["${server.url("/tiles/{z}/{x}/{y}.pbf")}"]}""".replace("%7B", "{").replace("%7D", "}"))
                        .build()
                    "/tiles/14/1/2.pbf" -> MockResponse.Builder().body(Buffer().write(sampleTile())).build()
                    else -> MockResponse.Builder().code(404).build()
                }
            }
        }
        server.start()
        try {
            val repository = MapTileRepository(OkHttpClient(), server.url("/planet").toString())
            val tile = repository.tile(TileId(14, 1, 2))
            repository.tile(TileId(14, 1, 2))
            assertEquals(setOf("building", "transportation"), tile.layers.keys)
            assertEquals(listOf("/planet", "/tiles/14/1/2.pbf"), requests)
        } finally {
            server.close()
        }
    }

    /** A tile with a square building and a two-part primary road. */
    private fun sampleTile(): ByteArray {
        val square = intArrayOf(command(1, 1), zz(10), zz(10), command(2, 2), zz(10), zz(0), zz(0), zz(10), command(7, 1))
        val building = message {
            string(1, "building")
            bytes(2, message { varint(3, 3); packed(4, square) })
            varint(5, 4096)
        }
        val lines = intArrayOf(
            command(1, 1), zz(0), zz(0), command(2, 1), zz(100), zz(0),
            command(1, 1), zz(-50), zz(50), command(2, 1), zz(0), zz(100),
        )
        val road = message {
            string(1, "transportation")
            bytes(2, message { packed(2, intArrayOf(0, 0)); varint(3, 2); packed(4, lines) })
            string(3, "class")
            bytes(4, message { string(1, "primary") })
            varint(5, 4096)
        }
        return message {
            bytes(3, building)
            bytes(3, road)
        }
    }

    private fun command(id: Int, count: Int) = (count shl 3) or id

    private fun zz(n: Int) = (n shl 1) xor (n shr 31)

    private class Writer {
        val out = ByteArrayOutputStream()

        fun raw(v: Long) {
            var value = v
            while (value and 0x7F.inv().toLong() != 0L) {
                out.write(((value and 0x7F) or 0x80).toInt())
                value = value ushr 7
            }
            out.write(value.toInt())
        }

        fun varint(field: Int, v: Int) {
            raw((field shl 3).toLong())
            raw(v.toLong())
        }

        fun bytes(field: Int, data: ByteArray) {
            raw(((field shl 3) or 2).toLong())
            raw(data.size.toLong())
            out.write(data)
        }

        fun string(field: Int, s: String) = bytes(field, s.toByteArray())

        fun packed(field: Int, values: IntArray) = bytes(field, Writer().apply { values.forEach { raw(it.toLong() and 0xFFFFFFFFL) } }.out.toByteArray())
    }

    private fun message(block: Writer.() -> Unit): ByteArray = Writer().apply(block).out.toByteArray()
}
