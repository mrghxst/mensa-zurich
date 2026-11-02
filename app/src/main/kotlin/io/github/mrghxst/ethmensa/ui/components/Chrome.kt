// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.ui.theme.Eth
import io.github.mrghxst.ethmensa.ui.theme.EthShapes
import io.github.mrghxst.ethmensa.ui.theme.rememberReducedMotion
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import io.github.mrghxst.ethmensa.ui.theme.pressScale

/** Circular liquid-glass icon button for floating chrome. 48 dp touch target. */
@Composable
fun GlassIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(44.dp)
            .pressScale(interaction, pressedScale = 0.9f)
            .glass(GlassStyle.LIQUID, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) { LucideIcon(icon, null, size = 20.dp) }
}

/** Floating search dock: search field and the filter button with an active badge. */
@Composable
fun FloatingDock(
    query: String,
    onQueryChange: (String) -> Unit,
    filtersActive: Boolean,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val colors = Eth.colors
    Row(
        modifier
            .height(56.dp)
            .glass(GlassStyle.LIQUID, EthShapes.dock)
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(Icons.search, null, tint = colors.textSecondary, size = 18.dp)
        Spacer(Modifier.width(10.dp))
        val searchLabel = stringResource(R.string.search_mensas)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(searchLabel, style = Eth.type.body, color = colors.textTertiary)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = Eth.type.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = searchLabel },
            )
        }
        if (query.isNotEmpty()) {
            val clear = stringResource(R.string.clear_search)
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = clear) { onQueryChange("") }
                    .semantics { contentDescription = clear },
                contentAlignment = Alignment.Center,
            ) { LucideIcon(Icons.close, null, tint = colors.textSecondary, size = 16.dp) }
        }
        Box(Modifier.padding(horizontal = 6.dp).width(1.dp).fillMaxHeight(0.5f).background(colors.border))
        val filterLabel = stringResource(R.string.filter)
        val activeLabel = stringResource(R.string.filters_active)
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = filterLabel, onClick = onFilterClick)
                .semantics { contentDescription = if (filtersActive) "$filterLabel, $activeLabel" else filterLabel },
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(Icons.filter, null, size = 20.dp)
            if (filtersActive) {
                Box(
                    Modifier.offset(9.dp, (-9).dp).size(8.dp)
                        .background(colors.accent, CircleShape)
                        .border(1.5.dp, colors.surface, CircleShape),
                )
            }
        }
    }
}

/** A moving highlight for skeletons. Static when animations are off. */
@Composable
fun rememberShimmerBrush(): Brush {
    val base = Eth.colors.surfaceMuted
    val highlight = Eth.colors.border
    if (rememberReducedMotion()) return remember(base) { SolidColor(base) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x = transition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x.value, 0f),
        end = Offset(x.value + 400f, 200f),
    )
}
