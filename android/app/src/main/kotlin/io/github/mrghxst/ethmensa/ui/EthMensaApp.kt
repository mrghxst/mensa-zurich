// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.ui.theme.Eth

@Composable
fun EthMensaApp() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Eth.colors.background)
            .safeDrawingPadding(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 56.dp)) {
            Text(stringResource(R.string.app_name), style = Eth.type.largeTitle, color = Eth.colors.textPrimary)
            Text(stringResource(R.string.lunch).uppercase(), style = Eth.type.sectionLabel, color = Eth.colors.textSecondary)
            Text("13.20/14.20/16.50 CHF", style = Eth.type.mono, color = Eth.colors.accent)
        }
    }
}
