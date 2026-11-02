// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.data.map.MapTileRepository
import io.github.mrghxst.ethmensa.data.map.TileId
import io.github.mrghxst.ethmensa.data.map.VectorTile
import io.github.mrghxst.ethmensa.data.map.mercatorTile
import io.github.mrghxst.ethmensa.data.map.tilesCovering
import io.github.mrghxst.ethmensa.domain.model.LatLng
import io.github.mrghxst.ethmensa.ui.LocalAppGraph
import io.github.mrghxst.ethmensa.ui.theme.Eth
import io.github.mrghxst.ethmensa.ui.theme.EthShapes
import kotlinx.coroutines.CancellationException

private val ThumbnailSize = 96.dp

/** Drawn size of one zoom-14 tile: the 96 dp thumbnail then spans about 200 m around the mensa. */
private val TileExtentDp = 768.dp

/**
 * 96 dp map tile (§9.6): OpenFreeMap vector tiles drawn in the app's grays, without labels,
 * centered on the mensa. Tap opens a maps app via a chooser at the mensa's coordinates.
 *
 * MapLibre Compose 0.20 fails to load its native library on Android, so the thumbnail decodes
 * the tiles and draws water, green areas, buildings and roads itself. Attribution is in
 * Settings → Open-source licenses. Without coordinates, or offline with nothing cached, it shows a pin.
 */
@Composable
fun MapThumbnail(point: LatLng?, name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = Eth.colors
    val label = stringResource(R.string.open_in_maps)
    val map = if (point != null) rememberMap(point) else null
    Box(
        modifier
            .size(ThumbnailSize)
            .clip(EthShapes.map)
            .background(colors.surfaceMuted)
            .border(1.dp, colors.border, EthShapes.map),
        contentAlignment = Alignment.Center,
    ) {
        when {
            point == null || map?.failed == true -> LucideIcon(Icons.mapPin, null, tint = colors.textTertiary, size = 26.dp)
            else -> {
                val alpha by animateFloatAsState(if (map?.tiles != null) 1f else 0f, tween(220), label = "map-alpha")
                map?.tiles?.let { tiles ->
                    VectorMap(point, tiles, if (colors.isDark) DarkMapPalette else LightMapPalette, Modifier.graphicsLayer { this.alpha = alpha })
                }
                // Marker: a dot in the text color with a ring in the surface color.
                Box(
                    Modifier
                        .size(14.dp)
                        .shadow(3.dp, CircleShape)
                        .background(colors.surface, CircleShape)
                        .padding(3.dp)
                        .background(colors.textPrimary, CircleShape),
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .clickable(role = Role.Button, onClickLabel = label) { openInMaps(context, point, name) }
                .semantics { contentDescription = label },
        )
    }
}

/** Tiles around the mensa, or null while loading. [failed] when they can't be loaded. */
private class MapState(val tiles: List<Pair<TileId, VectorTile>>?, val failed: Boolean)

@Composable
private fun rememberMap(point: LatLng): MapState {
    val repository = LocalAppGraph.current.mapTiles
    val state by produceState(MapState(null, failed = false), point) {
        val (x, y) = mercatorTile(point, MapTileRepository.MAX_ZOOM)
        val halfSpan = (ThumbnailSize / 2) / TileExtentDp
        value = try {
            MapState(tilesCovering(x, y, halfSpan.toDouble(), MapTileRepository.MAX_ZOOM).map { it to repository.tile(it) }, failed = false)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            MapState(null, failed = true)
        }
    }
    return state
}

@Immutable
private data class MapPalette(
    val green: Color,
    val water: Color,
    val building: Color,
    val road: Color,
    val path: Color,
    val rail: Color,
)

private val LightMapPalette = MapPalette(
    green = Color(0xFFE7E9E6),
    water = Color(0xFFD5D8DC),
    building = Color(0xFFDADADE),
    road = Color(0xFFFFFFFF),
    path = Color(0xFFFFFFFF),
    rail = Color(0xFFD4D4D8),
)

private val DarkMapPalette = MapPalette(
    green = Color(0xFF1E1F1E),
    water = Color(0xFF101012),
    building = Color(0xFF2A2A2A),
    road = Color(0xFF343434),
    path = Color(0xFF2B2B2B),
    rail = Color(0xFF2E2E2E),
)

/** Stroke width of a road by its OpenMapTiles class, or null to skip it. */
private fun roadWidth(kind: String?): Dp? = when (kind) {
    "motorway", "trunk" -> 6.dp
    "primary" -> 5.dp
    "secondary" -> 4.5.dp
    "tertiary" -> 4.dp
    "minor" -> 3.dp
    "service", "busway" -> 2.dp
    "track", "path" -> 1.2.dp
    "rail", "transit" -> 1.dp
    else -> null
}

@Composable
private fun VectorMap(point: LatLng, tiles: List<Pair<TileId, VectorTile>>, palette: MapPalette, modifier: Modifier) {
    Box(
        modifier.fillMaxSize().drawWithCache {
            val (x, y) = mercatorTile(point, MapTileRepository.MAX_ZOOM)
            val scale = TileExtentDp.toPx()
            val centerX = size.width / 2
            val centerY = size.height / 2

            // Non-zero fill: outer rings and holes wind in opposite directions, and overlapping
            // polygons of the same layer merge instead of cancelling out.
            val green = Path()
            val water = Path()
            val buildings = Path()
            val roads = sortedMapOf<Float, Path>()
            val paths = sortedMapOf<Float, Path>()
            val rails = Path()

            for ((id, tile) in tiles) {
                for (layer in tile.layers.values) {
                    val unit = scale / layer.extent
                    val left = ((id.x - x) * scale + centerX).toFloat()
                    val top = ((id.y - y) * scale + centerY).toFloat()
                    for (feature in layer.features) {
                        val target: Path = when (layer.name) {
                            "water" -> water
                            "park", "landcover" -> green
                            "building" -> buildings
                            "transportation" -> {
                                if (feature.type != VectorTile.GeometryType.LINE) continue
                                val width = roadWidth(feature.kind)?.toPx() ?: continue
                                when (feature.kind) {
                                    "rail", "transit" -> rails
                                    "track", "path" -> paths.getOrPut(width) { Path() }
                                    else -> roads.getOrPut(width) { Path() }
                                }
                            }
                            else -> continue
                        }
                        if (layer.name != "transportation" && feature.type != VectorTile.GeometryType.POLYGON) continue
                        for (part in feature.parts) {
                            target.moveTo(left + part[0] * unit, top + part[1] * unit)
                            for (i in 2 until part.size step 2) target.lineTo(left + part[i] * unit, top + part[i + 1] * unit)
                        }
                    }
                }
            }
            val railStroke = Stroke(1.dp.toPx(), cap = StrokeCap.Butt)
            onDrawBehind {
                drawPath(green, palette.green)
                drawPath(water, palette.water)
                for ((width, path) in paths) drawPath(path, palette.path, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(rails, palette.rail, style = railStroke)
                // Narrow roads first, so wide ones stay continuous at junctions.
                for ((width, path) in roads) drawPath(path, palette.road, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(buildings, palette.building)
            }
        },
    )
}

fun openInMaps(context: Context, point: LatLng?, name: String) {
    val uri = if (point != null) {
        "geo:${point.lat},${point.lng}?q=${point.lat},${point.lng}(${Uri.encode(name)})".toUri()
    } else {
        "geo:0,0?q=${Uri.encode(name)}".toUri()
    }
    runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), name)) }
}
