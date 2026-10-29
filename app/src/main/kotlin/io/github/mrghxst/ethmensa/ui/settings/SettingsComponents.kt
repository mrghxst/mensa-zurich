// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.ui.components.BorderedCard
import io.github.mrghxst.ethmensa.ui.components.GlassIconButton
import io.github.mrghxst.ethmensa.ui.components.GlassStyle
import io.github.mrghxst.ethmensa.ui.components.Icons
import io.github.mrghxst.ethmensa.ui.components.LucideIcon
import io.github.mrghxst.ethmensa.ui.components.ProvideGlass
import io.github.mrghxst.ethmensa.ui.components.SectionLabel
import io.github.mrghxst.ethmensa.ui.components.frostedGlassSource
import io.github.mrghxst.ethmensa.ui.components.glass
import io.github.mrghxst.ethmensa.ui.components.liquidGlassSource
import io.github.mrghxst.ethmensa.ui.components.rememberGlassSources
import io.github.mrghxst.ethmensa.ui.theme.Eth

/** A settings-style page: large title that collapses into a frosted bar with a back button. */
@Composable
fun SettingsPage(title: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    val glass = rememberGlassSources()
    val listState = rememberLazyListState()
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40 } }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
    ProvideGlass(glass) {
        Box(Modifier.fillMaxSize().background(Eth.colors.background)) {
            Box(Modifier.fillMaxSize().liquidGlassSource(glass)) {
                Box(Modifier.fillMaxSize().background(Eth.colors.background).frostedGlassSource(glass)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = top, bottom = bottom),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item(key = "title") {
                            Text(
                                title,
                                style = Eth.type.largeTitle,
                                color = Eth.colors.textPrimary,
                                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).semantics { heading() },
                            )
                        }
                        content()
                    }
                }
            }
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.matchParentSize().graphicsLayer { alpha = if (collapsed) 1f else 0f }
                        .glass(GlassStyle.FROSTED, RectangleShape, shadow = false),
                )
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIconButton(Icons.arrowLeft, stringResource(R.string.back), onBack)
                    Text(
                        title,
                        style = Eth.type.screenTitle,
                        color = Eth.colors.textPrimary,
                        modifier = Modifier.padding(start = 12.dp).graphicsLayer { alpha = if (collapsed) 1f else 0f },
                    )
                }
            }
        }
    }
}

/** A titled group of rows in one bordered card, with an optional footer. */
fun LazyListScope.settingsGroup(
    key: String,
    titleText: @Composable () -> String?,
    footerText: @Composable () -> String? = { null },
    destructiveFooter: Boolean = false,
    rows: @Composable ColumnScope.() -> Unit,
) {
    item(key = key) {
        val title = titleText()
        val footer = footerText()
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp).padding(top = 20.dp)) {
            if (title != null) SectionLabel(title, Modifier.padding(start = 4.dp, bottom = 8.dp))
            BorderedCard(Modifier.fillMaxWidth()) { Column(content = rows) }
            if (footer != null) {
                Text(
                    footer,
                    style = Eth.type.caption.copy(fontStyle = null),
                    color = if (destructiveFooter) Eth.colors.closed else Eth.colors.textSecondary,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
                )
            }
        }
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 1.dp, color = Eth.colors.border)
}

/** A tappable row with an optional icon, value and chevron. */
@Composable
fun SettingsRow(
    title: String,
    onClick: (() -> Unit)?,
    @DrawableRes icon: Int? = null,
    value: String? = null,
    subtitle: String? = null,
    titleColor: Color = Eth.colors.textPrimary,
    chevron: Boolean = onClick != null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            LucideIcon(icon, null, tint = Eth.colors.textSecondary, size = 20.dp)
            Spacer(Modifier.width(14.dp))
        }
        // The value moves under the title when both don't fit (large font sizes).
        FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.align(Alignment.CenterVertically).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = Eth.type.body, color = titleColor)
                if (subtitle != null) Text(subtitle, style = Eth.type.caption.copy(fontStyle = null), color = Eth.colors.textSecondary)
            }
            if (value != null) {
                Text(value, style = Eth.type.body, color = Eth.colors.textSecondary, modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
        if (chevron) {
            Spacer(Modifier.width(6.dp))
            LucideIcon(Icons.chevronRight, null, tint = Eth.colors.textTertiary, size = 18.dp)
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(checked, enabled = enabled, role = Role.Switch) {
                haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onCheckedChange(it)
            }
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            LucideIcon(icon, null, tint = Eth.colors.textSecondary, size = 20.dp)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Eth.type.body, color = if (enabled) Eth.colors.textPrimary else Eth.colors.textTertiary)
            if (subtitle != null) Text(subtitle, style = Eth.type.caption.copy(fontStyle = null), color = Eth.colors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Eth.colors.accent,
                checkedThumbColor = Eth.colors.onAccent,
                uncheckedTrackColor = Eth.colors.surfaceMuted,
                uncheckedBorderColor = Eth.colors.border,
                uncheckedThumbColor = Eth.colors.textTertiary,
            ),
        )
    }
}

/** Confirmation with a destructive button. */
@Composable
fun ConfirmDialog(title: String, message: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = Eth.type.screenTitle) },
        text = { Text(message, style = Eth.type.body, color = Eth.colors.textSecondary) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = Eth.colors.closed, style = Eth.type.label) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Eth.colors.textPrimary, style = Eth.type.label) }
        },
        containerColor = Eth.colors.surface,
    )
}
