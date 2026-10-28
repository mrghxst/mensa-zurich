// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Design tokens from ANDROID_PORT_PROMPT.md §9.1. */
@Immutable
data class EthColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val open: Color,
    val closed: Color,
    val veg: Color,
    val fish: Color,
    val glassTint: Color,
    val glassStroke: Color,
    val glassShadow: Color,
    val isDark: Boolean,
)

val LightEthColors = EthColors(
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF4F4F5),
    border = Color(0xFFEAEAEA),
    textPrimary = Color(0xFF0A0A0A),
    textSecondary = Color(0xFF666666),
    textTertiary = Color(0xFF8F8F8F),
    accent = Color(0xFF205CAE),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFE8F0FB),
    open = Color(0xFF16A34A),
    closed = Color(0xFFDC2626),
    veg = Color(0xFF16A34A),
    fish = Color(0xFF2563EB),
    glassTint = Color(0xFFFFFFFF).copy(alpha = 0.70f),
    glassStroke = Color(0xFF000000).copy(alpha = 0.06f),
    glassShadow = Color(0xFF000000).copy(alpha = 0.08f),
    isDark = false,
)

val DarkEthColors = EthColors(
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF111111),
    surfaceMuted = Color(0xFF1A1A1A),
    border = Color(0xFF262626),
    textPrimary = Color(0xFFEDEDED),
    textSecondary = Color(0xFFA1A1A1),
    textTertiary = Color(0xFF737373),
    accent = Color(0xFF4C8DF6),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFF14243D),
    open = Color(0xFF22C55E),
    closed = Color(0xFFEF4444),
    veg = Color(0xFF22C55E),
    fish = Color(0xFF60A5FA),
    glassTint = Color(0xFF141414).copy(alpha = 0.62f),
    glassStroke = Color(0xFFFFFFFF).copy(alpha = 0.10f),
    glassShadow = Color.Transparent,
    isDark = true,
)

/** Meat icon tints from MeatType.swift. */
object MeatColors {
    val beef = Color(0xFF8B5E3C)
    val chicken = Color(0xFFEAB308)
    val duck = Color(0xFFF97316)
    val veal = Color(0xFF9CA3AF)
    val turkey = Color(0xFFA16207)
    val pork = Color(0xFFEC4899)
    val other = Color(0xFF78716C)
}

val LocalEthColors = staticCompositionLocalOf { LightEthColors }
