// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.domain.model.KnownAllergen
import io.github.mrghxst.ethmensa.ui.components.allergenNameRes
import io.github.mrghxst.ethmensa.ui.theme.Eth

@Composable
fun AllergensScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsPage(stringResource(R.string.allergens), onBack) {
        settingsGroup("show", { null }) {
            SettingsSwitch(
                title = stringResource(R.string.show_in_menus),
                checked = settings.showAllergens,
                onCheckedChange = { on -> viewModel.update { it.copy(showAllergens = on) } },
            )
        }
        settingsGroup("mine", { stringResource(R.string.my_allergens) }) {
            KnownAllergen.entries.forEachIndexed { index, allergen ->
                if (index > 0) RowDivider()
                val checked = allergen in settings.allergens
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(checked, role = Role.Checkbox) { viewModel.toggleAllergen(allergen, it) }
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(allergenNameRes(allergen)), style = Eth.type.body, color = Eth.colors.textPrimary, modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = Eth.colors.accent, uncheckedColor = Eth.colors.textTertiary),
                    )
                }
            }
        }
    }
}
