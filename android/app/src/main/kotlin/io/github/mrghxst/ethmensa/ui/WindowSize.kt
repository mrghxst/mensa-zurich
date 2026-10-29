// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** At least 600 dp wide: tablets, unfolded foldables, landscape (two panes, wider gutters). */
@Composable
fun isWideWindow(): Boolean {
    val width = LocalWindowInfo.current.containerSize.width
    return with(LocalDensity.current) { width.toDp() } >= 600.dp
}
