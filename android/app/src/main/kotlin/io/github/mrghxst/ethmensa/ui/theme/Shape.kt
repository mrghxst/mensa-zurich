// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radii and spacing from ANDROID_PORT_PROMPT.md §9.3. */
object EthShapes {
    val card = RoundedCornerShape(20.dp)
    val thumbnail = RoundedCornerShape(14.dp)
    val map = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(percent = 50)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val dock = RoundedCornerShape(32.dp)
}

object Spacing {
    val gutter = 16.dp
    val gutterWide = 24.dp
    val cardPadding = 16.dp
    val cardGap = 12.dp
}

val materialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
