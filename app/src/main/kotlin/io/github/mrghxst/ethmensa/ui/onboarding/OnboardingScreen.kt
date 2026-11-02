// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.ui.isWideWindow
import io.github.mrghxst.ethmensa.ui.settings.PRIVACY_POLICY_URL
import io.github.mrghxst.ethmensa.ui.settings.openUrl
import io.github.mrghxst.ethmensa.ui.components.Icons
import io.github.mrghxst.ethmensa.ui.components.LucideIcon
import io.github.mrghxst.ethmensa.ui.components.PillButton
import io.github.mrghxst.ethmensa.ui.theme.Eth

/** First-launch welcome (§10.6). Two panes on tablets. */
@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    var privacy by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val wide = isWideWindow()
    Column(Modifier.fillMaxSize().background(Eth.colors.background).safeDrawingPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (wide) {
                Row(
                    Modifier.widthIn(max = 960.dp).fillMaxWidth().padding(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                ) {
                    Column(Modifier.weight(1f)) { Header() }
                    Column(Modifier.weight(1f)) { Features() }
                }
            } else {
                Column(
                    Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp),
                ) {
                    Spacer(Modifier.height(24.dp))
                    Header()
                    Spacer(Modifier.height(40.dp))
                    Features()
                }
            }
        }
        Column(
            Modifier.widthIn(max = 520.dp).fillMaxWidth().align(Alignment.CenterHorizontally).padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LucideIcon(Icons.shieldCheck, null, tint = Eth.colors.textSecondary, size = 20.dp)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.onboarding_privacy), style = Eth.type.caption.copy(fontStyle = null), color = Eth.colors.textSecondary)
            TextButton(onClick = { privacy = true }) {
                Text(stringResource(R.string.privacy_policy), style = Eth.type.label, color = Eth.colors.accent)
            }
            PillButton(stringResource(R.string.continue_label), onContinue, Modifier.fillMaxWidth())
        }
    }
    if (privacy) {
        AlertDialog(
            onDismissRequest = { privacy = false },
            title = { Text(stringResource(R.string.privacy_policy), style = Eth.type.screenTitle) },
            text = { Text(stringResource(R.string.privacy_statement), style = Eth.type.body, color = Eth.colors.textSecondary) },
            confirmButton = { TextButton(onClick = { privacy = false }) { Text(stringResource(R.string.close), color = Eth.colors.accent) } },
            dismissButton = {
                TextButton(onClick = { openUrl(context, PRIVACY_POLICY_URL) }) {
                    Text(stringResource(R.string.full_privacy_policy), color = Eth.colors.accent)
                }
            },
            containerColor = Eth.colors.surface,
        )
    }
}

@Composable
private fun Header() {
    Image(
        painterResource(R.drawable.app_icon),
        contentDescription = null,
        modifier = Modifier.size(78.dp).clip(RoundedCornerShape(22.dp)),
    )
    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.welcome_to), style = Eth.type.display, color = Eth.colors.textPrimary, modifier = Modifier.semantics { heading() })
    Text(stringResource(R.string.app_name), style = Eth.type.display, color = Eth.colors.accent)
}

@Composable
private fun Features() {
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Feature(Icons.utensils, stringResource(R.string.onboarding_feature_1_title), stringResource(R.string.onboarding_feature_1_description))
        Feature(Icons.idCard, stringResource(R.string.onboarding_feature_2_title), stringResource(R.string.onboarding_legi_description))
    }
}

@Composable
private fun Feature(@DrawableRes icon: Int, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            LucideIcon(icon, null, tint = Eth.colors.accent, size = 32.dp)
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = Eth.type.mealTitle, color = Eth.colors.textPrimary)
            Text(description, style = Eth.type.body, color = Eth.colors.textSecondary)
        }
    }
}
