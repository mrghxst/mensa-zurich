// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.ui.theme.Eth
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Mon–Sun connected button group with a dot under today. */
@Composable
fun WeekdayGroup(
    selected: Int,
    today: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val todayLabel = stringResource(R.string.today)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        (1..7).forEach { day ->
            val full = weekdayName(day, TextStyle.FULL)
            val checked = day == selected
            ToggleButton(
                checked = checked,
                enabled = enabled,
                onCheckedChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onSelect(day)
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
                    .semantics { contentDescription = if (day == today) "$full, $todayLabel" else full },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(weekdayName(day, TextStyle.SHORT).take(2), style = Eth.type.label)
                    val dot = when {
                        day != today -> Color.Transparent
                        checked -> Eth.colors.onAccent
                        else -> Eth.colors.accent
                    }
                    Box(Modifier.padding(top = 2.dp).size(4.dp).background(dot, CircleShape))
                }
            }
        }
    }
}

/** Localized weekday name for an ISO weekday, capitalized. */
@Composable
fun weekdayName(weekday: Int, style: TextStyle): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return DayOfWeek.of(weekday).getDisplayName(style, locale).replaceFirstChar { it.titlecase(locale) }
}
