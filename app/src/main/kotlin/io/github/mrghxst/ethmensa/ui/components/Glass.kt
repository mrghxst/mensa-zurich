// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.mrghxst.ethmensa.ui.theme.Eth

/**
 * Glass for floating chrome only (§9.4). Frosted = Haze blur (bars). Liquid = Backdrop lens
 * (dock and overlay buttons). API 33+ gets both, API 31–32 gets blur only, API 29–30 an opaque tint.
 */
enum class GlassStyle { FROSTED, LIQUID }

/** The content layers glass reads from. Haze and Backdrop each get their own source layer. */
@Stable
class GlassSources(val haze: HazeState, val backdrop: LayerBackdrop)

val LocalGlassSources = staticCompositionLocalOf<GlassSources?> { null }

@Composable
fun rememberGlassSources(): GlassSources {
    val haze = rememberHazeState()
    val backdrop = rememberLayerBackdrop()
    return remember(haze, backdrop) { GlassSources(haze, backdrop) }
}

/** Provides [sources] to glass drawn inside [content]. */
@Composable
fun ProvideGlass(sources: GlassSources, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGlassSources provides sources, content = content)
}

/** Marks the outer content layer for liquid glass. */
fun Modifier.liquidGlassSource(sources: GlassSources): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) layerBackdrop(sources.backdrop) else this

/** Marks the inner content layer for frosted glass. */
fun Modifier.frostedGlassSource(sources: GlassSources): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) hazeSource(sources.haze) else this

@Composable
fun Modifier.glass(
    style: GlassStyle,
    shape: Shape,
    tint: Color = Eth.colors.glassTint,
    shadow: Boolean = true,
): Modifier {
    val colors = Eth.colors
    val sources = LocalGlassSources.current
    val sdk = Build.VERSION.SDK_INT
    val shadowModifier = if (shadow && !colors.isDark) {
        Modifier.shadow(16.dp, shape, ambientColor = colors.glassShadow, spotColor = colors.glassShadow)
    } else {
        Modifier
    }
    val stroke = Modifier.border(1.dp, colors.glassStroke, shape)
    val effect = when {
        sources == null || sdk < Build.VERSION_CODES.S ->
            Modifier.clip(shape).background(tint.copy(alpha = 0.92f))
        style == GlassStyle.LIQUID && sdk >= Build.VERSION_CODES.TIRAMISU ->
            Modifier.drawBackdrop(
                backdrop = sources.backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(12.dp.toPx())
                    lens(10.dp.toPx(), 20.dp.toPx())
                },
                onDrawSurface = { drawRect(tint) },
            )
        else -> {
            val blurStyle = remember(tint) {
                HazeBlurStyle {
                    blurRadius(24.dp)
                    backgroundColor(tint.copy(alpha = 1f))
                    colorEffects(listOf(HazeColorEffect.tint(tint)))
                    noiseFactor(0f)
                }
            }
            Modifier.clip(shape).hazeBlur(input = HazeInput.Sources(sources.haze), style = blurStyle)
        }
    }
    return this.then(shadowModifier).then(effect).then(stroke)
}
