// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.core.net.toUri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mrghxst.ethmensa.BuildConfig
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.data.AppLanguage
import io.github.mrghxst.ethmensa.domain.model.PriceType
import io.github.mrghxst.ethmensa.domain.model.ThemeMode
import io.github.mrghxst.ethmensa.ui.components.Icons
import io.github.mrghxst.ethmensa.ui.theme.Eth
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenAllergens: () -> Unit,
    onOpenMyMensas: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenText: (asset: String, title: String) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var languageDialog by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf<Int?>(null) }
    val language = AppLanguage.override()

    SettingsPage(stringResource(R.string.settings), onBack) {
        settingsGroup("general", { stringResource(R.string.general) }) {
            SettingsRow(
                title = stringResource(R.string.language),
                icon = Icons.globe,
                value = language?.let { languageName(it) } ?: stringResource(R.string.language_system),
                onClick = { languageDialog = true },
            )
            RowDivider()
            SettingsRow(stringResource(R.string.notifications), icon = Icons.bell, onClick = onOpenNotifications)
            RowDivider()
            SettingsRow(
                title = stringResource(R.string.allergens),
                icon = Icons.alert,
                value = if (settings.allergens.isEmpty()) {
                    stringResource(R.string.no_allergens_selected)
                } else {
                    pluralStringResource(R.plurals.lld_allergens_selected, settings.allergens.size, settings.allergens.size)
                },
                onClick = onOpenAllergens,
            )
            RowDivider()
            SettingsRow(
                title = stringResource(R.string.my_mensas),
                icon = Icons.star,
                value = (settings.favorites.size + settings.hidden.size).takeIf { it > 0 }?.toString(),
                onClick = onOpenMyMensas,
            )
            RowDivider()
            SettingsRow(
                title = stringResource(R.string.open_links),
                subtitle = stringResource(R.string.open_links_footer),
                icon = Icons.share,
                onClick = { openLinkSettings(context) },
            )
        }

        settingsGroup("appearance", { stringResource(R.string.appearance) }, footerText = { stringResource(R.string.price_to_show_footer) }) {
            SettingsSwitch(
                title = stringResource(R.string.hide_mensas_without_menus),
                checked = settings.hideMensaWithNoMenus,
                onCheckedChange = { on -> viewModel.update { it.copy(hideMensaWithNoMenus = on) } },
            )
            RowDivider()
            LabeledSegments(
                label = stringResource(R.string.theme),
                options = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.DARK to stringResource(R.string.theme_dark),
                ),
                selected = settings.themeMode,
                onSelect = { mode -> viewModel.update { it.copy(themeMode = mode) } },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RowDivider()
                SettingsSwitch(
                    title = stringResource(R.string.wallpaper_colors),
                    subtitle = stringResource(R.string.wallpaper_colors_footer),
                    checked = settings.dynamicColor,
                    onCheckedChange = { on -> viewModel.update { it.copy(dynamicColor = on) } },
                )
            }
            RowDivider()
            LabeledSegments(
                label = stringResource(R.string.price_to_show),
                options = listOf(
                    PriceType.ALL to stringResource(R.string.all),
                    PriceType.STUDENT to stringResource(R.string.student),
                    PriceType.STAFF to stringResource(R.string.staff),
                    PriceType.EXTERNAL to stringResource(R.string.external),
                ),
                selected = settings.priceType,
                onSelect = { type -> viewModel.update { it.copy(priceType = type) } },
            )
        }

        settingsGroup("backup", { stringResource(R.string.backup) }, footerText = { stringResource(R.string.backup_footer) }) {
            SettingsRow(stringResource(R.string.sync_settings), icon = Icons.check, onClick = null, value = "Android")
        }

        settingsGroup("advanced", { stringResource(R.string.advanced_settings) }, footerText = { stringResource(R.string.advanced_settings_footer_message) + " " + stringResource(R.string.irreversible) }, destructiveFooter = true) {
            listOf(R.string.reset_smart_sorting, R.string.reset_maps_cache, R.string.reset_settings, R.string.reset_settings_and_data)
                .forEachIndexed { index, res ->
                    if (index > 0) RowDivider()
                    SettingsRow(stringResource(res), onClick = { confirm = res }, titleColor = Eth.colors.closed, chevron = false)
                }
        }

        settingsGroup("about", { stringResource(R.string.about_section) }) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.app_icon), null, Modifier.size(56.dp).clip(io.github.mrghxst.ethmensa.ui.theme.EthShapes.thumbnail))
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.about_app), style = Eth.type.rowTitle, color = Eth.colors.textPrimary)
                    Text(
                        stringResource(R.string.version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                        style = Eth.type.mono,
                        color = Eth.colors.textSecondary,
                    )
                }
            }
            RowDivider()
            SettingsRow(
                title = stringResource(R.string.based_on),
                subtitle = stringResource(R.string.unofficial),
                onClick = { openUrl(context, "https://github.com/alexandrereol/ethmensa") },
            )
            RowDivider()
            SettingsRow(
                title = stringResource(R.string.source_code),
                subtitle = stringResource(R.string.source_code_private),
                onClick = { openUrl(context, "https://github.com/mrghxst/mensa-zurich") },
            )
            RowDivider()
            val licenseTitle = stringResource(R.string.license)
            SettingsRow(
                title = licenseTitle,
                value = "GPLv3",
                onClick = { onOpenText("licenses/LICENSE_GPL3.txt", licenseTitle) },
            )
            RowDivider()
            val ossTitle = stringResource(R.string.open_source_licenses)
            SettingsRow(title = ossTitle, onClick = { onOpenText("licenses/THIRD_PARTY.txt", ossTitle) })
            RowDivider()
            val fontTitle = "Geist (SIL OFL 1.1)"
            SettingsRow(title = fontTitle, onClick = { onOpenText("licenses/geist_OFL.txt", fontTitle) })
        }

        settingsGroup("privacy", { stringResource(R.string.privacy) }, footerText = { stringResource(R.string.privacy_statement) }) {
            SettingsRow(stringResource(R.string.onboarding_privacy), icon = Icons.shieldCheck, onClick = null)
            RowDivider()
            SettingsRow(stringResource(R.string.privacy_policy), onClick = { openUrl(context, PRIVACY_POLICY_URL) })
            RowDivider()
            SettingsRow(stringResource(R.string.terms_of_use), onClick = { openUrl(context, TERMS_URL) })
        }
    }

    if (languageDialog) {
        LanguageDialog(current = language, onSelect = { AppLanguage.set(it); languageDialog = false }, onDismiss = { languageDialog = false })
    }
    confirm?.let { res ->
        val title = stringResource(res)
        ConfirmDialog(
            title = stringResource(R.string.reset_confirm_title, title),
            message = stringResource(R.string.irreversible),
            confirm = title,
            onConfirm = {
                when (res) {
                    R.string.reset_smart_sorting -> viewModel.resetSmartSorting()
                    R.string.reset_maps_cache -> viewModel.resetMapsCache()
                    R.string.reset_settings -> viewModel.resetSettings()
                    R.string.reset_settings_and_data -> viewModel.resetSettingsAndData()
                }
            },
            onDismiss = { confirm = null },
        )
    }
}

@Composable
private fun <T> LabeledSegments(label: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, style = Eth.type.body, color = Eth.colors.textPrimary)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            options.forEachIndexed { index, (value, text) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    icon = {},
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Eth.colors.accentSoft,
                        activeContentColor = Eth.colors.accent,
                        activeBorderColor = Eth.colors.border,
                        inactiveContainerColor = Eth.colors.surface,
                        inactiveContentColor = Eth.colors.textPrimary,
                        inactiveBorderColor = Eth.colors.border,
                    ),
                    label = { Text(text, style = Eth.type.label, maxLines = 1) },
                )
            }
        }
    }
}

@Composable
private fun LanguageDialog(current: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    val options = listOf<Pair<String?, String>>(null to stringResource(R.string.language_system)) +
        AppLanguage.SUPPORTED.map { it to languageName(it) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language), style = Eth.type.screenTitle) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { (code, name) ->
                    Row(
                        Modifier.fillMaxWidth().selectable(code == current, role = Role.RadioButton) { onSelect(code) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(code == current, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Eth.colors.accent))
                        Spacer(Modifier.width(12.dp))
                        Text(name, style = Eth.type.body, color = Eth.colors.textPrimary)
                    }
                }
                Text(stringResource(R.string.language_applies_now), style = Eth.type.caption, color = Eth.colors.textTertiary, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = Eth.colors.accent) } },
        containerColor = Eth.colors.surface,
    )
}

/** The language's own name, e.g. "Deutsch". */
private fun languageName(code: String): String {
    val locale = Locale.forLanguageTag(code)
    return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
}

/** The privacy policy and terms on the app's website (site/ in this repository). */
const val PRIVACY_POLICY_URL = "https://mrghxst.github.io/mensa/privacy/"
const val TERMS_URL = "https://mrghxst.github.io/mensa/terms/"

fun openUrl(context: Context, url: String) {
    runCatching { CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, url.toUri()) }
        .onFailure { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } }
}

/** Android 12+ needs the user to allow the iOS app's shared links once (unverified App Links, §8). */
private fun openLinkSettings(context: Context) {
    val pkg = "package:${context.packageName}".toUri()
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(AndroidSettings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, pkg)
    } else {
        Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
    }
    runCatching { context.startActivity(intent) }
}
