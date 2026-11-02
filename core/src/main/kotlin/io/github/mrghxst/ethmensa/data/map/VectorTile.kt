// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.map

/**
 * A decoded Mapbox Vector Tile (https://github.com/mapbox/vector-tile-spec, v2), reduced to what
 * the map thumbnail draws: each feature's geometry and its `class` tag.
 */
class VectorTile(val layers: Map<String, Layer>) {
    class Layer(val name: String, val extent: Int, val features: List<Feature>)

    /**
     * [parts] are the lines of a line string, or the rings of a polygon (outer rings clockwise,
     * holes counter-clockwise in screen coordinates), as interleaved x, y in tile units 0…extent.
     */
    class Feature(val type: GeometryType, val parts: List<IntArray>, val kind: String?)

    enum class GeometryType { UNKNOWN, POINT, LINE, POLYGON }

    companion object {
        /**
         * Decodes an uncompressed tile. Only the layers named in [only] are decoded, or all of them
         * when it's null. Throws [IllegalArgumentException] for malformed data.
         */
        fun decode(bytes: ByteArray, only: Set<String>? = null): VectorTile {
            val layers = LinkedHashMap<String, Layer>()
            try {
                val pbf = Pbf(bytes, 0, bytes.size)
                while (pbf.hasMore()) {
                    val (field, wire) = pbf.tag()
                    if (field == 3 && wire == WIRE_BYTES) {
                        decodeLayer(pbf.message(), only)?.let { layers[it.name] = it }
                    } else {
                        pbf.skip(wire)
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                throw IllegalArgumentException("Truncated vector tile", e)
            }
            return VectorTile(layers)
        }

        private fun decodeLayer(pbf: Pbf, only: Set<String>?): Layer? {
            var name = ""
            var extent = 4096
            val keys = ArrayList<String>()
            val values = ArrayList<String?>()
            val rawFeatures = ArrayList<Pbf>()
            while (pbf.hasMore()) {
                val (field, wire) = pbf.tag()
                when {
                    field == 1 && wire == WIRE_BYTES -> {
                        name = pbf.string()
                        // The name comes first in practice, so skip unwanted layers early.
                        if (only != null && name !in only) return null
                    }
                    field == 2 && wire == WIRE_BYTES -> rawFeatures += pbf.message()
                    field == 3 && wire == WIRE_BYTES -> keys += pbf.string()
                    field == 4 && wire == WIRE_BYTES -> values += decodeValue(pbf.message())
                    field == 5 && wire == WIRE_VARINT -> extent = pbf.varint().toInt()
                    else -> pbf.skip(wire)
                }
            }
            if (only != null && name !in only) return null
            val classKey = keys.indexOf("class")
            return Layer(name, extent, rawFeatures.map { decodeFeature(it, classKey, values) })
        }

        private fun decodeValue(pbf: Pbf): String? {
            var value: String? = null
            while (pbf.hasMore()) {
                val (field, wire) = pbf.tag()
                if (field == 1 && wire == WIRE_BYTES) value = pbf.string() else pbf.skip(wire)
            }
            return value
        }

        private fun decodeFeature(pbf: Pbf, classKey: Int, values: List<String?>): Feature {
            var type = GeometryType.UNKNOWN
            var tags = IntArray(0)
            var geometry = IntArray(0)
            while (pbf.hasMore()) {
                val (field, wire) = pbf.tag()
                when {
                    field == 2 && wire == WIRE_BYTES -> tags = pbf.packedVarints()
                    field == 3 && wire == WIRE_VARINT -> type = GeometryType.entries.getOrElse(pbf.varint().toInt()) { GeometryType.UNKNOWN }
                    field == 4 && wire == WIRE_BYTES -> geometry = pbf.packedVarints()
                    else -> pbf.skip(wire)
                }
            }
            var kind: String? = null
            if (classKey >= 0) {
                for (i in 0 until tags.size - 1 step 2) {
                    if (tags[i] == classKey) kind = values.getOrNull(tags[i + 1])
                }
            }
            return Feature(type, decodeGeometry(geometry), kind)
        }

        /** Turns the MoveTo / LineTo / ClosePath command stream into absolute parts. */
        private fun decodeGeometry(commands: IntArray): List<IntArray> {
            val parts = ArrayList<IntArray>()
            var current = PartBuilder()
            var x = 0
            var y = 0
            var i = 0
            while (i < commands.size) {
                val command = commands[i] and 0x7
                val count = commands[i] ushr 3
                i++
                when (command) {
                    MOVE_TO, LINE_TO -> repeat(count) {
                        if (i + 1 >= commands.size) return parts.also { current.addTo(it) }
                        if (command == MOVE_TO) {
                            current.addTo(parts)
                            current = PartBuilder()
                        }
                        x += zigzag(commands[i])
                        y += zigzag(commands[i + 1])
                        i += 2
                        current.add(x, y)
                    }
                    CLOSE_PATH -> current.close()
                    else -> return parts.also { current.addTo(it) }
                }
            }
            current.addTo(parts)
            return parts
        }

        private fun zigzag(n: Int) = (n ushr 1) xor -(n and 1)

        private const val WIRE_VARINT = 0
        private const val WIRE_BYTES = 2
        private const val MOVE_TO = 1
        private const val LINE_TO = 2
        private const val CLOSE_PATH = 7
    }
}

private class PartBuilder {
    private var data = IntArray(16)
    private var size = 0

    fun add(x: Int, y: Int) {
        if (size + 2 > data.size) data = data.copyOf(data.size * 2)
        data[size++] = x
        data[size++] = y
    }

    /** Repeats the first point, so a ring is explicitly closed. */
    fun close() {
        if (size >= 2) add(data[0], data[1])
    }

    fun addTo(parts: MutableList<IntArray>) {
        if (size >= 2) parts += data.copyOf(size)
    }
}

/** Minimal protobuf reader over [buf] from [pos] to [end]. */
private class Pbf(private val buf: ByteArray, private var pos: Int, private val end: Int) {
    fun hasMore() = pos < end

    fun varint(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            if (pos >= end) throw IndexOutOfBoundsException()
            val b = buf[pos++].toInt() and 0xFF
            result = result or ((b and 0x7F).toLong() shl shift)
            if (b < 0x80) return result
            shift += 7
            require(shift < 64) { "Varint too long" }
        }
    }

    fun tag(): Pair<Int, Int> {
        val v = varint().toInt()
        return (v ushr 3) to (v and 0x7)
    }

    private fun length(): Int {
        val length = varint().toInt()
        if (length < 0 || pos + length > end) throw IndexOutOfBoundsException()
        return length
    }

    fun message(): Pbf {
        val length = length()
        return Pbf(buf, pos, pos + length).also { pos += length }
    }

    fun string(): String {
        val length = length()
        return String(buf, pos, length, Charsets.UTF_8).also { pos += length }
    }

    fun packedVarints(): IntArray {
        val inner = message()
        val out = IntList()
        while (inner.hasMore()) out.add(inner.varint().toInt())
        return out.toArray()
    }

    fun skip(wire: Int) {
        when (wire) {
            0 -> varint()
            1 -> pos += 8
            2 -> pos += length()
            5 -> pos += 4
            else -> throw IllegalArgumentException("Unsupported wire type $wire")
        }
        if (pos > end) throw IndexOutOfBoundsException()
    }
}

private class IntList {
    private var data = IntArray(32)
    private var size = 0

    fun add(v: Int) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = v
    }

    fun toArray(): IntArray = data.copyOf(size)
}
