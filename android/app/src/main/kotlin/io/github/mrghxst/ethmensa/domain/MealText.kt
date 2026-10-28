// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain

import java.text.Normalizer

/** Text rules for meals: placeholder detection and whitespace normalization (§5.7 items 1 and 2). */
object MealText {
    private val PLACEHOLDERS = listOf(
        "novalue",
        "closed",
        "geschlossen",
        "no dinner",
        "kein abendessen",
        "we look forward to serving you this menu again soon!",
        "dieses menu servieren wir ihnen gerne bald wieder!",
    )
    private val WHITESPACE = Regex("\\s+")

    /** True if the name or description says the meal isn't really served. */
    fun isPlaceholder(name: String?, description: String?): Boolean =
        listOfNotNull(name, description).any { text ->
            val lower = text.lowercase()
            PLACEHOLDERS.any { it in lower }
        }

    /** Trims and turns empty strings into null. */
    fun cleanName(value: String?): String? = value?.let { WHITESPACE.replace(it, " ").trim() }?.takeIf { it.isNotEmpty() }

    /** ETHAPI.mapMeal pipe rules, then collapse repeated whitespace. */
    fun cleanEthDescription(value: String?): String? = value
        ?.replace("|\n", "| ")
        ?.replace("\n", " | ")
        ?.let { WHITESPACE.replace(it, " ").trim() }
        ?.takeIf { it.isNotEmpty() }

    fun cleanDescription(value: String?): String? = cleanName(value)

    /** "WOK STREET" → "Wok Street" (iOS `.capitalized` for all-uppercase titles). */
    fun displayTitle(title: String): String {
        val letters = title.filter { it.isLetter() }
        if (letters.isEmpty() || letters != letters.uppercase()) return title
        return title.lowercase().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.titlecase() }
        }
    }
}

/** Lowercase without diacritics, for "Hönggerberg" matching "honggerberg". */
fun String.foldForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()
