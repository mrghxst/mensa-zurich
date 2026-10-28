// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.mrghxst.ethmensa.ui.EthMensaApp
import io.github.mrghxst.ethmensa.ui.theme.EthMensaTheme

// AppCompatActivity (a ComponentActivity) so per-app languages also apply on API 29-32.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EthMensaTheme {
                EthMensaApp()
            }
        }
    }
}
