// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.mrghxst.ethmensa.AppGraph

val LocalAppGraph = staticCompositionLocalOf<AppGraph> { error("AppGraph not provided") }
