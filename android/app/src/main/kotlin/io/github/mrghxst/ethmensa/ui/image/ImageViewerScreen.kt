// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.ui.components.GlassIconButton
import io.github.mrghxst.ethmensa.ui.components.Icons
import io.github.mrghxst.ethmensa.ui.components.LightStatusBarIcons
import me.saket.telephoto.ExperimentalTelephotoApi
import me.saket.telephoto.flick.FlickToDismiss
import me.saket.telephoto.flick.FlickToDismissState
import me.saket.telephoto.flick.rememberFlickToDismissState
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

/** Full-screen meal photo: pinch, double-tap and pan to zoom, flick down to close. */
@OptIn(ExperimentalTelephotoApi::class)
@Composable
fun ImageViewerScreen(url: String, onClose: () -> Unit) {
    LightStatusBarIcons(lightIcons = true)
    val flick = rememberFlickToDismissState()
    LaunchedEffect(flick.gestureState) {
        if (flick.gestureState is FlickToDismissState.GestureState.Dismissed) onClose()
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        FlickToDismiss(state = flick) {
            ZoomableAsyncImage(
                model = url,
                contentDescription = stringResource(R.string.meal_photo),
                state = rememberZoomableImageState(rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 3f))),
                modifier = Modifier.fillMaxSize(),
            )
        }
        GlassIconButton(
            Icons.close,
            stringResource(R.string.close),
            onClose,
            Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),
        )
    }
}
