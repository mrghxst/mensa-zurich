// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.components

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.domain.MealText
import io.github.mrghxst.ethmensa.domain.formatPrice
import io.github.mrghxst.ethmensa.domain.model.Allergen
import io.github.mrghxst.ethmensa.domain.model.KnownAllergen
import io.github.mrghxst.ethmensa.domain.model.Meal
import io.github.mrghxst.ethmensa.domain.model.MealType
import io.github.mrghxst.ethmensa.domain.model.MeatType
import io.github.mrghxst.ethmensa.domain.model.Nutrition
import io.github.mrghxst.ethmensa.domain.model.PriceType
import io.github.mrghxst.ethmensa.ui.shareText
import io.github.mrghxst.ethmensa.ui.theme.Eth
import io.github.mrghxst.ethmensa.ui.theme.EthShapes
import io.github.mrghxst.ethmensa.ui.theme.MeatColors
import kotlinx.coroutines.launch
import java.util.Locale

/** One meal: title + diet icons + price, name + photo, description, allergens and nutrition. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MealCard(
    meal: Meal,
    priceType: PriceType,
    showAllergens: Boolean,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var actions by rememberSaveable { mutableStateOf(false) }
    var nutritionOpen by rememberSaveable { mutableStateOf(false) }
    BorderedCard(
        modifier
            .fillMaxWidth()
            .clip(EthShapes.card)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    actions = true
                },
                onLongClickLabel = stringResource(R.string.share),
            ),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    meal.title?.let {
                        Text(MealText.displayTitle(it), style = Eth.type.mealTitle, color = Eth.colors.textPrimary, modifier = Modifier.weight(1f, fill = false))
                    }
                    DietIcons(meal.mealTypes, meal.meatTypes, Modifier.padding(start = 8.dp))
                }
                formatPrice(meal.price, priceType)?.let {
                    Spacer(Modifier.width(12.dp))
                    Text(it, style = Eth.type.mono, color = Eth.colors.textSecondary, textAlign = TextAlign.End)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    meal.name?.let { Text(it, style = Eth.type.mealName, color = Eth.colors.textPrimary) }
                    meal.description?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = Eth.type.body, color = Eth.colors.textSecondary)
                    }
                }
                meal.imageUrl?.let { url ->
                    Spacer(Modifier.width(12.dp))
                    val label = stringResource(R.string.meal_photo)
                    Thumbnail(
                        url,
                        Modifier.size(64.dp).clickable(onClickLabel = label) { onImageClick(url) }.semantics { contentDescription = label },
                    )
                }
            }
            if (showAllergens && meal.allergens.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.contains, meal.allergens.map { allergenName(it) }.joinToString(", ")),
                    style = Eth.type.caption,
                    color = Eth.colors.textTertiary,
                )
            }
            val nutrition = meal.nutrition
            if (nutrition != null && !nutrition.isEmpty) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.clip(EthShapes.pill).clickable { nutritionOpen = !nutritionOpen }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.nutrition), style = Eth.type.label, color = Eth.colors.accent)
                    Spacer(Modifier.width(4.dp))
                    LucideIcon(
                        Icons.chevronRight,
                        null,
                        tint = Eth.colors.accent,
                        size = 16.dp,
                        modifier = Modifier.rotate(if (nutritionOpen) 90f else 0f),
                    )
                }
                AnimatedVisibility(nutritionOpen) { NutritionTable(nutrition) }
            }
        }
    }
    if (actions) MealActionsSheet(meal, onDismiss = { actions = false })
}

@Composable
private fun NutritionTable(n: Nutrition) {
    fun fmt(v: Double, unit: String) = String.format(Locale.ROOT, if (v >= 100) "%.0f %s" else "%.1f %s", v, unit)
    val rows = buildList {
        val energy = listOfNotNull(n.energyKj?.let { fmt(it, "kJ") }, n.energyKcal?.let { fmt(it, "kcal") }).joinToString(" / ")
        if (energy.isNotEmpty()) add(R.string.nutrition_energy to energy)
        n.fat?.let { add(R.string.nutrition_fat to fmt(it, "g")) }
        n.saturatedFat?.let { add(R.string.nutrition_saturated to fmt(it, "g")) }
        n.carbohydrates?.let { add(R.string.nutrition_carbohydrates to fmt(it, "g")) }
        n.sugar?.let { add(R.string.nutrition_sugar to fmt(it, "g")) }
        n.fibers?.let { add(R.string.nutrition_fibers to fmt(it, "g")) }
        n.protein?.let { add(R.string.nutrition_protein to fmt(it, "g")) }
        n.salt?.let { add(R.string.nutrition_salt to fmt(it, "g")) }
    }
    Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(if (n.basis == Nutrition.Basis.PER_100G) R.string.per_100g else R.string.per_serving),
            style = Eth.type.caption,
            color = Eth.colors.textTertiary,
        )
        rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth()) {
                Text(stringResource(label), style = Eth.type.body, color = Eth.colors.textSecondary, modifier = Modifier.weight(1f))
                Text(value, style = Eth.type.mono, color = Eth.colors.textPrimary)
            }
        }
    }
}

@Composable
private fun MealActionsSheet(meal: Meal, onDismiss: () -> Unit) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Eth.colors.background) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(
                listOfNotNull(meal.title?.let(MealText::displayTitle), meal.name).joinToString(" · "),
                style = Eth.type.mealTitle,
                color = Eth.colors.textPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            val itemColors = ListItemDefaults.colors(containerColor = Eth.colors.background)
            ListItem(
                headlineContent = { Text(stringResource(R.string.copy), style = Eth.type.body) },
                leadingContent = { LucideIcon(Icons.copy, null) },
                colors = itemColors,
                modifier = Modifier.clickable {
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(meal.name, meal.shareText))) }
                    onDismiss()
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.share), style = Eth.type.body) },
                leadingContent = { LucideIcon(Icons.share, null) },
                colors = itemColors,
                modifier = Modifier.clickable {
                    shareText(context, meal.shareText, meal.name)
                    onDismiss()
                },
            )
        }
    }
}

/** Vegan/vegetarian/fish and meat icons with TalkBack names. */
@Composable
fun DietIcons(mealTypes: List<MealType>, meatTypes: List<MeatType>, modifier: Modifier = Modifier) {
    if (mealTypes.isEmpty() && meatTypes.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        mealTypes.forEach { type ->
            val (icon, tint, label) = when (type) {
                MealType.VEGAN -> Triple(Icons.sprout, Eth.colors.veg, R.string.diet_vegan)
                MealType.VEGETARIAN -> Triple(Icons.leaf, Eth.colors.veg, R.string.diet_vegetarian)
                MealType.FISH -> Triple(Icons.fish, Eth.colors.fish, R.string.diet_fish)
            }
            LucideIcon(icon, stringResource(label), tint = tint, size = 16.dp)
        }
        meatTypes.forEach { type ->
            val (icon, tint, label) = when (type) {
                MeatType.BEEF -> Triple(R.drawable.meat_beef, MeatColors.beef, R.string.meat_beef)
                MeatType.VEAL -> Triple(R.drawable.meat_beef, MeatColors.veal, R.string.meat_veal)
                MeatType.CHICKEN -> Triple(R.drawable.meat_chicken, MeatColors.chicken, R.string.meat_chicken)
                MeatType.DUCK -> Triple(R.drawable.meat_duck, MeatColors.duck, R.string.meat_duck)
                MeatType.TURKEY -> Triple(R.drawable.meat_turkey, MeatColors.turkey, R.string.meat_turkey)
                MeatType.PORK -> Triple(R.drawable.meat_pork, MeatColors.pork, R.string.meat_pork)
                MeatType.LAMB -> Triple(Icons.beef, MeatColors.other, R.string.meat_lamb)
                MeatType.GAME -> Triple(Icons.beef, MeatColors.other, R.string.meat_game)
            }
            Icon(painterResource(icon), stringResource(label), Modifier.size(16.dp), tint = tint)
        }
    }
}

@Composable
fun allergenName(allergen: Allergen): String = when (allergen) {
    is Allergen.Other -> allergen.name
    is KnownAllergen -> stringResource(allergenNameRes(allergen))
}

fun allergenNameRes(allergen: KnownAllergen): Int = when (allergen) {
    KnownAllergen.GLUTEN -> R.string.allergen_gluten
    KnownAllergen.CRUSTACEANS -> R.string.allergen_crustaceans
    KnownAllergen.EGGS -> R.string.allergen_eggs
    KnownAllergen.FISH -> R.string.allergen_fish
    KnownAllergen.PEANUTS -> R.string.allergen_peanuts
    KnownAllergen.SOYA -> R.string.allergen_soya
    KnownAllergen.LACTOSE -> R.string.allergen_lactose
    KnownAllergen.NUTS -> R.string.allergen_nuts
    KnownAllergen.CELERY -> R.string.allergen_celery
    KnownAllergen.MUSTARD -> R.string.allergen_mustard
    KnownAllergen.SESAME -> R.string.allergen_sesame
    KnownAllergen.SULPHUR_DIOXIDE -> R.string.allergen_sulphurdioxide
    KnownAllergen.LUPIN -> R.string.allergen_lupin
    KnownAllergen.MOLLUSCS -> R.string.allergen_molluscs
    KnownAllergen.WHEAT -> R.string.allergen_wheat
}

