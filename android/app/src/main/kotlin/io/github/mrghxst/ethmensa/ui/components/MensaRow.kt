// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.domain.OpeningState
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.model.MensaCellType
import io.github.mrghxst.ethmensa.ui.theme.Eth
import io.github.mrghxst.ethmensa.ui.theme.EthShapes

/**
 * One mensa in the list, in one of the three iOS cell styles. Long-press opens Open / Share.
 * [imageModifier] lets the caller attach a shared-element transition to the thumbnail.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MensaRow(
    mensa: Mensa,
    state: OpeningState?,
    hoursToday: String?,
    cellType: MensaCellType,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }
    val label = when (state) {
        OpeningState.OPEN -> stringResource(R.string.a11y_open_now, mensa.name)
        OpeningState.CLOSED -> stringResource(R.string.a11y_closed, mensa.name)
        else -> mensa.name
    }
    val shareLabel = stringResource(R.string.share)
    val type = if (cellType == MensaCellType.LARGE && mensa.imageUrl == null) MensaCellType.MINIMAL else cellType
    Box(modifier) {
        BorderedCard(
            Modifier
                .fillMaxWidth()
                .clip(EthShapes.card)
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                    onLongClick(shareLabel) { menuOpen = true; true }
                },
        ) {
            when (type) {
                MensaCellType.MINIMAL -> Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { RowText(mensa.name, hoursToday = null, state = state, modifier = Modifier.weight(1f)) }

                MensaCellType.STANDARD -> Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Thumbnail(mensa.imageUrl, imageModifier.size(60.dp))
                    Spacer(Modifier.width(14.dp))
                    RowText(mensa.name, hoursToday, state, Modifier.weight(1f).padding(end = 6.dp))
                }

                MensaCellType.LARGE -> Column(Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = mensa.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = imageModifier.fillMaxWidth().height(140.dp).background(Eth.colors.surfaceMuted),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { RowText(mensa.name, hoursToday, state, Modifier.weight(1f)) }
                }
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.open)) },
                leadingIcon = { LucideIcon(Icons.chevronRight, null, size = 18.dp) },
                onClick = { menuOpen = false; onOpen() },
            )
            DropdownMenuItem(
                text = { Text(shareLabel) },
                leadingIcon = { LucideIcon(Icons.share, null, size = 18.dp) },
                onClick = { menuOpen = false; onShare() },
            )
        }
    }
}

@Composable
private fun RowText(name: String, hoursToday: String?, state: OpeningState?, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center) {
        Text(name, style = Eth.type.rowTitle, color = Eth.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (!hoursToday.isNullOrEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(hoursToday, style = Eth.type.mono, color = Eth.colors.textTertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
    StatusDot(state)
    Spacer(Modifier.width(10.dp))
    LucideIcon(Icons.chevronRight, null, tint = Eth.colors.textTertiary, size = 18.dp)
}

/** Rounded thumbnail with the app icon as fallback. */
@Composable
fun Thumbnail(url: String?, modifier: Modifier) {
    val fallback = painterResource(R.drawable.app_icon)
    AsyncImage(
        model = url,
        contentDescription = null,
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(EthShapes.thumbnail).background(Eth.colors.surfaceMuted),
    )
}

/** Placeholder card while nothing has loaded. */
@Composable
fun SkeletonRow(cellType: MensaCellType, shimmer: Brush, modifier: Modifier = Modifier) {
    BorderedCard(modifier.fillMaxWidth()) {
        when (cellType) {
            MensaCellType.MINIMAL -> Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(160.dp).height(14.dp).clip(EthShapes.pill).background(shimmer))
            }
            MensaCellType.STANDARD -> Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).clip(EthShapes.thumbnail).background(shimmer))
                Spacer(Modifier.width(14.dp))
                Column {
                    Box(Modifier.width(150.dp).height(14.dp).clip(EthShapes.pill).background(shimmer))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(90.dp).height(10.dp).clip(EthShapes.pill).background(shimmer))
                }
            }
            MensaCellType.LARGE -> Column {
                Box(Modifier.fillMaxWidth().height(140.dp).background(shimmer))
                Box(Modifier.padding(16.dp).width(160.dp).height(14.dp).clip(EthShapes.pill).background(shimmer))
            }
        }
    }
}

