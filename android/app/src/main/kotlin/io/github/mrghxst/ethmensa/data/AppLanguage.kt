// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/** The app's UI language: the per-app language if set, else the device language, limited to en/de/fr/it. */
object AppLanguage {
    val SUPPORTED = listOf("en", "de", "fr", "it")

    fun current(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val language = if (!appLocales.isEmpty) appLocales[0]?.language else Locale.getDefault().language
        return language?.takeIf { it in SUPPORTED } ?: "en"
    }

    /** The explicitly chosen language, or null when following the system. */
    fun override(): String? {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        return if (appLocales.isEmpty) null else appLocales[0]?.language
    }

    /** Applies immediately; null follows the system language. */
    fun set(language: String?) {
        val locales = if (language == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
