// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain.model

import androidx.compose.runtime.Immutable
import java.time.LocalTime

enum class MensaCellType { MINIMAL, STANDARD, LARGE }

enum class PriceType { ALL, STUDENT, STAFF, EXTERNAL }

enum class SortBy { SMART, NAME }

enum class MensaShowType { ALL, OPEN }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** iOS ids: `default-apple` = SYSTEM, `default` = DEFAULT, `click` = CLICK. */
enum class NotificationSound(val id: String) {
    SYSTEM("system"),
    DEFAULT("default"),
    CLICK("click"),
    ;

    companion object {
        fun fromId(id: String?): NotificationSound = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/** All persisted settings with the defaults from SettingsManager.swift (§6.10). */
@Immutable
data class Settings(
    val completedOnboarding: Boolean = false,
    val hideMensaWithNoMenus: Boolean = false,
    val mensaCellType: MensaCellType = MensaCellType.STANDARD,
    val priceType: PriceType = PriceType.ALL,
    val notifications: Boolean = false,
    val notificationSound: NotificationSound = NotificationSound.DEFAULT,
    val notificationTime: LocalTime = LocalTime.of(11, 0),
    val locationNotifications: Boolean = false,
    val sortBy: SortBy = SortBy.SMART,
    val mensaShowType: MensaShowType = MensaShowType.ALL,
    /** Null means all campuses. */
    val campus: Campus? = null,
    val showAllergens: Boolean = false,
    val allergens: Set<KnownAllergen> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
)
