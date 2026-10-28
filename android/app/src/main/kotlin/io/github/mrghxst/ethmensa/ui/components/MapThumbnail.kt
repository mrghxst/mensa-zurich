// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.domain.model.LatLng
import io.github.mrghxst.ethmensa.ui.theme.Eth
import io.github.mrghxst.ethmensa.ui.theme.EthShapes

/**
 * 96 dp map tile (§9.6). Tap opens a maps app via a chooser at the mensa's coordinates.
 */
@Composable
fun MapThumbnail(point: LatLng?, name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = Eth.colors
    val label = stringResource(R.string.open_in_maps)
    Box(
        modifier
            .size(96.dp)
            .clip(EthShapes.map)
            .background(colors.surfaceMuted)
            .border(1.dp, colors.border, EthShapes.map),
        contentAlignment = Alignment.Center,
    ) {
        // MapLibre Compose 0.20 fails to load its native library on Android, so this is the
        // static fallback from §9.6: a monochrome pin, and the tap opens the maps chooser.
        LucideIcon(Icons.mapPin, null, tint = if (point != null) colors.accent else colors.textTertiary, size = 26.dp)
        Box(
            Modifier
                .fillMaxSize()
                .clickable(role = Role.Button, onClickLabel = label) { openInMaps(context, point, name) },
        )
    }
}

fun openInMaps(context: Context, point: LatLng?, name: String) {
    val uri = if (point != null) {
        Uri.parse("geo:${point.lat},${point.lng}?q=${point.lat},${point.lng}(${Uri.encode(name)})")
    } else {
        Uri.parse("geo:0,0?q=${Uri.encode(name)}")
    }
    runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), name)) }
}
