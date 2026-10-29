// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.mrghxst.ethmensa.ui.theme.Eth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shows a bundled text asset (licenses). */
@Composable
fun TextScreen(asset: String, title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    LaunchedEffect(asset) {
        text = withContext(Dispatchers.IO) {
            runCatching { context.assets.open(asset).bufferedReader().use { it.readText() } }.getOrDefault("")
        }
    }
    val paragraphs = remember(text) { paragraphs(text) }
    SettingsPage(title, onBack) {
        // One item per paragraph so long licenses stay lazy.
        items(paragraphs.size) { index ->
            val (heading, body) = paragraphs[index]
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (heading != null) Text(heading, style = Eth.type.rowTitle, color = Eth.colors.textPrimary)
                if (body.isNotEmpty()) Text(body, style = Eth.type.body, color = Eth.colors.textSecondary)
            }
        }
    }
}

/**
 * Splits plain text at blank lines and unwraps the hard line breaks, so it reflows on any width.
 * A flush first line followed by indented lines is an entry title (THIRD_PARTY.txt).
 */
private fun paragraphs(text: String): List<Pair<String?, String>> =
    text.replace("\r\n", "\n").split(Regex("\n\\s*\n")).mapNotNull { block ->
        val lines = block.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return@mapNotNull null
        val titled = lines.size > 1 && !lines[0].startsWith(" ") && lines[1].startsWith(" ")
        val heading = if (titled) lines[0].trim() else null
        val body = (if (titled) lines.drop(1) else lines).joinToString(" ") { it.trim() }
        heading to body
    }
