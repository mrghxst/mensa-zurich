// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain.model

/** An allergen of a meal: one of the 15 known ones, or an unknown one shown by name. */
sealed interface Allergen {
    data class Other(val name: String) : Allergen

    companion object {
        /** ETH `allergen-array` entry. The numeric code is language-independent, the desc is the fallback. */
        fun fromEth(code: Int?, desc: String?): Allergen? {
            ETH_CODES[code]?.let { return it }
            val key = desc?.trim()?.lowercase() ?: return null
            if (key.isEmpty()) return null
            return ETH_DESCS[key] ?: Other(desc.trim())
        }

        /** ZFV allergen ids (`fromZFV` in Allergen.swift). Some ids map to two allergens. */
        fun fromZfv(id: String, name: String?): List<Allergen> = when (id) {
            "cerealsContainingGluten", "barley", "oat", "rye" -> listOf(KnownAllergen.GLUTEN)
            "wheat", "kamut", "spelt" -> listOf(KnownAllergen.GLUTEN, KnownAllergen.WHEAT)
            "crustaceans" -> listOf(KnownAllergen.CRUSTACEANS)
            "eggs" -> listOf(KnownAllergen.EGGS)
            "fish" -> listOf(KnownAllergen.FISH)
            "peanuts" -> listOf(KnownAllergen.PEANUTS)
            "soybeans" -> listOf(KnownAllergen.SOYA)
            "milk" -> listOf(KnownAllergen.LACTOSE)
            "nuts", "almond", "brazilNut", "cashew", "hazel", "macadamia", "pecan", "pistachio",
            "queenslandNut", "walnut",
            -> listOf(KnownAllergen.NUTS)
            "celery" -> listOf(KnownAllergen.CELERY)
            "mustard" -> listOf(KnownAllergen.MUSTARD)
            "sesame" -> listOf(KnownAllergen.SESAME)
            "sulphites" -> listOf(KnownAllergen.SULPHUR_DIOXIDE)
            "lupin" -> listOf(KnownAllergen.LUPIN)
            "molluscs" -> listOf(KnownAllergen.MOLLUSCS)
            else -> listOf(Other(name?.takeIf { it.isNotBlank() } ?: id))
        }

        private val ETH_CODES = mapOf(
            10 to KnownAllergen.GLUTEN,
            11 to KnownAllergen.CRUSTACEANS,
            12 to KnownAllergen.EGGS,
            13 to KnownAllergen.FISH,
            14 to KnownAllergen.PEANUTS,
            15 to KnownAllergen.SOYA,
            16 to KnownAllergen.LACTOSE,
            17 to KnownAllergen.NUTS,
            18 to KnownAllergen.CELERY,
            19 to KnownAllergen.MUSTARD,
            20 to KnownAllergen.SESAME,
            21 to KnownAllergen.SULPHUR_DIOXIDE,
            22 to KnownAllergen.LUPIN,
            23 to KnownAllergen.MOLLUSCS,
        )

        // English descs from Allergen.swift plus "milk, lactose" and the German descs seen in live data.
        private val ETH_DESCS = mapOf(
            "gluten wheat" to KnownAllergen.GLUTEN,
            "gluten" to KnownAllergen.GLUTEN,
            "crustaceans" to KnownAllergen.CRUSTACEANS,
            "krebstiere" to KnownAllergen.CRUSTACEANS,
            "eggs" to KnownAllergen.EGGS,
            "ei" to KnownAllergen.EGGS,
            "fish" to KnownAllergen.FISH,
            "fisch" to KnownAllergen.FISH,
            "peanuts" to KnownAllergen.PEANUTS,
            "erdnüsse" to KnownAllergen.PEANUTS,
            "soya" to KnownAllergen.SOYA,
            "soja" to KnownAllergen.SOYA,
            "lactose" to KnownAllergen.LACTOSE,
            "milk" to KnownAllergen.LACTOSE,
            "milk, lactose" to KnownAllergen.LACTOSE,
            "milch, laktose" to KnownAllergen.LACTOSE,
            "nuts" to KnownAllergen.NUTS,
            "schalenfrüchte" to KnownAllergen.NUTS,
            "celery" to KnownAllergen.CELERY,
            "sellerie" to KnownAllergen.CELERY,
            "mustard" to KnownAllergen.MUSTARD,
            "senf" to KnownAllergen.MUSTARD,
            "sesame" to KnownAllergen.SESAME,
            "sesam" to KnownAllergen.SESAME,
            "sulfites" to KnownAllergen.SULPHUR_DIOXIDE,
            "sulfite" to KnownAllergen.SULPHUR_DIOXIDE,
            "lupin" to KnownAllergen.LUPIN,
            "lupinen" to KnownAllergen.LUPIN,
            "molluscs" to KnownAllergen.MOLLUSCS,
            "weichtiere" to KnownAllergen.MOLLUSCS,
        )
    }
}

/** The 15 known allergens, in the settings list order. [id] is the persisted raw id. */
enum class KnownAllergen(val id: String) : Allergen {
    GLUTEN("gluten"),
    CRUSTACEANS("crustaceans"),
    EGGS("eggs"),
    FISH("fish"),
    PEANUTS("peanuts"),
    SOYA("soya"),
    LACTOSE("lactose"),
    NUTS("nuts"),
    CELERY("celery"),
    MUSTARD("mustard"),
    SESAME("sesame"),
    SULPHUR_DIOXIDE("sulphurdioxide"),
    LUPIN("lupin"),
    MOLLUSCS("molluscs"),
    WHEAT("wheat"),
    ;

    companion object {
        fun fromId(id: String): KnownAllergen? = entries.firstOrNull { it.id == id }
    }
}
