// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.ui.theme.Eth

/** Settings > Favorites and hidden (Android extra from MensaZH). */
@Composable
fun MyMensasScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val names by viewModel.mensaNames.collectAsStateWithLifecycle()
    // Ids of mensas that no longer exist are shown by id, so they can still be removed.
    fun nameOf(id: String) = names[id] ?: id

    SettingsPage(stringResource(R.string.my_mensas), onBack) {
        settingsGroup(
            "favorites",
            { stringResource(R.string.favorites) },
            footerText = { stringResource(R.string.my_mensas_footer) },
        ) {
            val favorites = settings.favorites.sortedBy(::nameOf)
            if (favorites.isEmpty()) EmptyRow(stringResource(R.string.no_favorites))
            favorites.forEachIndexed { index, id ->
                if (index > 0) RowDivider()
                MensaActionRow(nameOf(id), stringResource(R.string.unfavorite)) { viewModel.setFavorite(id, false) }
            }
        }
        settingsGroup("hidden", { stringResource(R.string.hidden_mensas) }) {
            val hidden = settings.hidden.sortedBy(::nameOf)
            if (hidden.isEmpty()) EmptyRow(stringResource(R.string.no_hidden))
            hidden.forEachIndexed { index, id ->
                if (index > 0) RowDivider()
                MensaActionRow(nameOf(id), stringResource(R.string.show_again)) { viewModel.setHidden(id, false) }
            }
        }
    }
}

@Composable
private fun MensaActionRow(name: String, action: String, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = Eth.type.body, color = Eth.colors.textPrimary, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) { Text(action, style = Eth.type.label, color = Eth.colors.accent) }
    }
}

@Composable
private fun EmptyRow(text: String) {
    Text(
        text,
        style = Eth.type.body,
        color = Eth.colors.textTertiary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
    )
}
