// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.mrghxst.ethmensa.ui.theme.Eth

/**
 * Forces light status-bar icons while [lightIcons] is true (over photos or black), and restores
 * the theme default when it turns false or the screen leaves.
 */
@Composable
fun LightStatusBarIcons(lightIcons: Boolean) {
    val activity = LocalActivity.current ?: return
    val view = LocalView.current
    val darkTheme = Eth.colors.isDark
    DisposableEffect(lightIcons, darkTheme) {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        controller.isAppearanceLightStatusBars = if (lightIcons) false else !darkTheme
        onDispose { controller.isAppearanceLightStatusBars = !darkTheme }
    }
}
