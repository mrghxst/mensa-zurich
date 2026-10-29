// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mrghxst.ethmensa.domain.OpeningState
import io.github.mrghxst.ethmensa.ui.theme.Eth
import io.github.mrghxst.ethmensa.ui.theme.EthShapes
import io.github.mrghxst.ethmensa.ui.theme.rememberReducedMotion
import com.composables.icons.lucide.R as Lucide

/** A Lucide icon, tinted. */
@Composable
fun LucideIcon(
    @DrawableRes icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Eth.colors.textPrimary,
    size: Dp = 20.dp,
) {
    Icon(painterResource(icon), contentDescription, modifier.size(size), tint = tint)
}

/** Green when open (with a soft pulse), red when closed, grey while loading, nothing when unknown. */
@Composable
fun StatusDot(state: OpeningState?, modifier: Modifier = Modifier) {
    val colors = Eth.colors
    val color = when (state) {
        OpeningState.OPEN -> colors.open
        OpeningState.CLOSED -> colors.closed
        OpeningState.UNKNOWN -> return
        null -> colors.textTertiary.copy(alpha = 0.5f)
    }
    val pulse = state == OpeningState.OPEN && !rememberReducedMotion()
    Box(modifier.size(10.dp), contentAlignment = Alignment.Center) {
        if (pulse) {
            val transition = rememberInfiniteTransition(label = "pulse")
            val progress = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
                label = "pulse",
            )
            Box(
                Modifier
                    .size(10.dp)
                    .drawBehind {
                        val p = progress.value
                        drawCircle(color.copy(alpha = 0.35f * (1f - p)), radius = size.minDimension / 2 * (0.8f + 0.6f * p))
                    },
            )
        }
        Box(Modifier.size(8.dp).background(color, CircleShape))
    }
}

/** A bordered content card (no shadow). */
@Composable
fun BorderedCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .background(Eth.colors.surface, EthShapes.card)
            .border(1.dp, Eth.colors.border, EthShapes.card),
    ) { content() }
}

/** "LUNCH" style section label. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, style = Eth.type.sectionLabel, color = Eth.colors.textSecondary)
}

/** Primary pill button in the accent color. */
@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = EthShapes.pill,
        colors = ButtonDefaults.buttonColors(containerColor = Eth.colors.accent, contentColor = Eth.colors.onAccent),
    ) { Text(text, style = Eth.type.label) }
}

/** Centered icon, title, message and an optional action. */
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(56.dp).background(Eth.colors.surfaceMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) { LucideIcon(icon, null, tint = Eth.colors.textSecondary, size = 26.dp) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = Eth.type.screenTitle, color = Eth.colors.textPrimary, textAlign = TextAlign.Center)
        if (message != null) {
            Spacer(Modifier.height(6.dp))
            Text(message, style = Eth.type.body, color = Eth.colors.textSecondary, textAlign = TextAlign.Center)
        }
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            PillButton(action, onAction)
        }
    }
}

/** Re-export so screens can write `Icons.search` instead of the long R path. */
object Icons {
    val search = Lucide.drawable.lucide_ic_search
    val filter = Lucide.drawable.lucide_ic_sliders_horizontal
    val idCard = Lucide.drawable.lucide_ic_id_card
    val settings = Lucide.drawable.lucide_ic_settings
    val share = Lucide.drawable.lucide_ic_share_2
    val mapPin = Lucide.drawable.lucide_ic_map_pin
    val clock = Lucide.drawable.lucide_ic_clock
    val calendar = Lucide.drawable.lucide_ic_calendar
    val shieldCheck = Lucide.drawable.lucide_ic_shield_check
    val sort = Lucide.drawable.lucide_ic_arrow_up_down
    val layout = Lucide.drawable.lucide_ic_layout_list
    val close = Lucide.drawable.lucide_ic_x
    val chevronRight = Lucide.drawable.lucide_ic_chevron_right
    val chevronLeft = Lucide.drawable.lucide_ic_chevron_left
    val arrowLeft = Lucide.drawable.lucide_ic_arrow_left
    val leaf = Lucide.drawable.lucide_ic_leaf
    val sprout = Lucide.drawable.lucide_ic_sprout
    val fish = Lucide.drawable.lucide_ic_fish
    val beef = Lucide.drawable.lucide_ic_beef
    val bell = Lucide.drawable.lucide_ic_bell
    val globe = Lucide.drawable.lucide_ic_globe
    val alert = Lucide.drawable.lucide_ic_circle_alert
    val copy = Lucide.drawable.lucide_ic_copy
    val utensils = Lucide.drawable.lucide_ic_utensils
    val cookingPot = Lucide.drawable.lucide_ic_cooking_pot
    val wifiOff = Lucide.drawable.lucide_ic_wifi_off
    val info = Lucide.drawable.lucide_ic_info
    val check = Lucide.drawable.lucide_ic_check
    val reset = Lucide.drawable.lucide_ic_rotate_ccw
    val languages = Lucide.drawable.lucide_ic_languages
    val star = Lucide.drawable.lucide_ic_star
    val eyeOff = Lucide.drawable.lucide_ic_eye_off
    val externalLink = Lucide.drawable.lucide_ic_external_link
}
