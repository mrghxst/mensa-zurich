// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.domain.model.Campus
import io.github.mrghxst.ethmensa.domain.model.MensaCellType
import io.github.mrghxst.ethmensa.domain.model.MensaShowType
import io.github.mrghxst.ethmensa.domain.model.SortBy
import io.github.mrghxst.ethmensa.ui.components.BorderedCard
import io.github.mrghxst.ethmensa.ui.components.SectionLabel
import io.github.mrghxst.ethmensa.ui.list.MensaListUiState
import io.github.mrghxst.ethmensa.ui.list.campusLabel
import io.github.mrghxst.ethmensa.ui.list.weekdayName
import io.github.mrghxst.ethmensa.ui.theme.Eth
import java.time.format.TextStyle

@Composable
fun FilterSheet(
    state: MensaListUiState,
    showLarge: Boolean,
    onDismiss: () -> Unit,
    onOpenOnly: (Boolean) -> Unit,
    onCampus: (Campus) -> Unit,
    onWeekday: (Int) -> Unit,
    onAllergenFriendly: (Boolean) -> Unit,
    onSort: (SortBy) -> Unit,
    onCellType: (MensaCellType) -> Unit,
    onReset: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    val filters = state.filters
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Eth.colors.background,
        contentColor = Eth.colors.textPrimary,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.filter), style = Eth.type.screenTitle, modifier = Modifier.weight(1f))
                if (filters.isAnyActive) {
                    TextButton(onClick = onReset) {
                        Text(stringResource(R.string.reset_filters), style = Eth.type.label, color = Eth.colors.accent)
                    }
                }
            }

            Section(stringResource(R.string.status)) {
                val disabled = filters.weekdayOverride != null
                SwitchRow(
                    title = stringResource(R.string.open_only),
                    subtitle = if (disabled) stringResource(R.string.open_only_disabled_hint) else null,
                    checked = filters.showType == MensaShowType.OPEN && !disabled,
                    enabled = !disabled,
                    onCheckedChange = {
                        haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                        onOpenOnly(it)
                    },
                )
            }

            Section(stringResource(R.string.location)) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Campus.entries.forEach { campus ->
                        val selected = filters.campus == campus
                        FilterChip(
                            selected = selected,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onCampus(campus)
                            },
                            label = { Text(campusLabel(campus), style = Eth.type.label) },
                            shape = io.github.mrghxst.ethmensa.ui.theme.EthShapes.pill,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Eth.colors.accentSoft,
                                selectedLabelColor = Eth.colors.accent,
                                labelColor = Eth.colors.textPrimary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = Eth.colors.border,
                                selectedBorderColor = Eth.colors.accentSoft,
                            ),
                        )
                    }
                }
            }

            Section(stringResource(R.string.day)) {
                val selectedDay = filters.weekdayOverride ?: state.today
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    (1..7).forEach { day ->
                        val label = weekdayName(day, TextStyle.SHORT).take(2)
                        val full = weekdayName(day, TextStyle.FULL)
                        val todayLabel = stringResource(R.string.today)
                        ToggleButton(
                            checked = day == selectedDay,
                            onCheckedChange = {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onWeekday(day)
                            },
                            shapes = when (day) {
                                1 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                7 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            colors = ToggleButtonDefaults.colors(
                                containerColor = Eth.colors.surfaceMuted,
                                contentColor = Eth.colors.textPrimary,
                                checkedContainerColor = Eth.colors.accent,
                                checkedContentColor = Eth.colors.onAccent,
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = if (day == state.today) "$full, $todayLabel" else full },
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, style = Eth.type.label)
                                Box(
                                    Modifier.padding(top = 2.dp).size(4.dp).background(
                                        if (day == state.today) {
                                            if (day == selectedDay) Eth.colors.onAccent else Eth.colors.accent
                                        } else {
                                            androidx.compose.ui.graphics.Color.Transparent
                                        },
                                        CircleShape,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            if (state.userHasAllergens) {
                Section(stringResource(R.string.allergen_friendly)) {
                    SwitchRow(
                        title = stringResource(R.string.allergen_friendly),
                        subtitle = stringResource(R.string.at_least_one_meal_without_your_allergens),
                        checked = filters.allergenFriendly,
                        enabled = true,
                        onCheckedChange = {
                            haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                            onAllergenFriendly(it)
                        },
                    )
                }
            }

            Section(stringResource(R.string.sort_by)) {
                Segmented(
                    options = listOf(SortBy.SMART to stringResource(R.string.sort_smart), SortBy.NAME to stringResource(R.string.sort_name)),
                    selected = filters.sortBy,
                    onSelect = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onSort(it)
                    },
                )
            }

            Section(stringResource(R.string.layout)) {
                val options = buildList {
                    add(MensaCellType.MINIMAL to stringResource(R.string.layout_minimal))
                    add(MensaCellType.STANDARD to stringResource(R.string.layout_standard))
                    if (showLarge) add(MensaCellType.LARGE to stringResource(R.string.layout_large))
                }
                val selected = if (!showLarge && state.cellType == MensaCellType.LARGE) MensaCellType.STANDARD else state.cellType
                Segmented(options, selected) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onCellType(it)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        SectionLabel(title, Modifier.padding(start = 4.dp, bottom = 8.dp))
        BorderedCard(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Eth.type.body, color = if (enabled) Eth.colors.textPrimary else Eth.colors.textTertiary)
            if (subtitle != null) {
                Text(subtitle, style = Eth.type.caption.copy(fontStyle = null), color = Eth.colors.textSecondary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(12.dp)) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Eth.colors.accentSoft,
                    activeContentColor = Eth.colors.accent,
                    activeBorderColor = Eth.colors.border,
                    inactiveContainerColor = Eth.colors.surface,
                    inactiveContentColor = Eth.colors.textPrimary,
                    inactiveBorderColor = Eth.colors.border,
                ),
                label = { Text(label, style = Eth.type.label) },
            )
        }
    }
}
