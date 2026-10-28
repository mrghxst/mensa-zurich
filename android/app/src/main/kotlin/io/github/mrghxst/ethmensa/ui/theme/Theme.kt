// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.mrghxst.ethmensa.domain.model.ThemeMode

private fun EthColors.toScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = accent,
        secondary = textSecondary,
        onSecondary = surface,
        secondaryContainer = surfaceMuted,
        onSecondaryContainer = textPrimary,
        tertiary = accent,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceMuted,
        surfaceContainerHighest = surfaceMuted,
        surfaceBright = surface,
        surfaceDim = background,
        outline = border,
        outlineVariant = border,
        error = closed,
        scrim = Color.Black.copy(alpha = 0.32f),
        // Snackbars: inverted monochrome with the other mode's accent.
        inverseSurface = textPrimary,
        inverseOnSurface = background,
        inversePrimary = if (isDark) LightEthColors.accent else DarkEthColors.accent,
    )
}

/** Brand-colored dynamic variant keeps open/closed and diet colors (§9.1). */
private fun EthColors.withDynamic(scheme: ColorScheme): EthColors = copy(
    background = scheme.background,
    surface = scheme.surfaceContainerLow,
    surfaceMuted = scheme.surfaceContainerHigh,
    border = scheme.outlineVariant,
    textPrimary = scheme.onSurface,
    textSecondary = scheme.onSurfaceVariant,
    textTertiary = scheme.outline,
    accent = scheme.primary,
    onAccent = scheme.onPrimary,
    accentSoft = scheme.primaryContainer,
)

@Composable
fun EthMensaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val (ethColors, scheme) = remember(dark, useDynamic, context) {
        val brand = if (dark) DarkEthColors else LightEthColors
        if (useDynamic) {
            val dyn = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            brand.withDynamic(dyn) to dyn
        } else {
            brand to brand.toScheme()
        }
    }
    CompositionLocalProvider(
        LocalEthColors provides ethColors,
        LocalEthTypography provides EthTypography(),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            shapes = materialShapes,
            typography = remember { materialTypography() },
            content = content,
        )
    }
}

/** Shorthand accessors. */
object Eth {
    val colors: EthColors
        @Composable get() = LocalEthColors.current
    val type: EthTypography
        @Composable get() = LocalEthTypography.current
}
