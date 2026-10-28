// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import io.github.mrghxst.ethmensa.domain.model.Settings
import io.github.mrghxst.ethmensa.ui.EthMensaApp
import io.github.mrghxst.ethmensa.ui.LocalAppGraph
import io.github.mrghxst.ethmensa.ui.theme.EthMensaTheme

// AppCompatActivity (a ComponentActivity) so per-app languages also apply on API 29-32.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val graph = (application as EthMensaApplication).graph
        // Keep the splash until settings are read, so onboarding never flashes for returning users.
        var settingsLoaded = false
        splash.setKeepOnScreenCondition { !settingsLoaded }
        lifecycleScope.launch {
            graph.settings.settings.first()
            settingsLoaded = true
        }
        setContent {
            val settings by graph.settings.settings.collectAsStateWithLifecycle(initialValue = Settings())
            CompositionLocalProvider(LocalAppGraph provides graph) {
                EthMensaTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                    EthMensaApp()
                }
            }
        }
    }
}
