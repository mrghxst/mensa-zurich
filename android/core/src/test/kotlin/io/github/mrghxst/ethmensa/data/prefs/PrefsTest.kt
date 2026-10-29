// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.mrghxst.ethmensa.domain.model.Campus
import io.github.mrghxst.ethmensa.domain.model.KnownAllergen
import io.github.mrghxst.ethmensa.domain.model.MensaCellType
import io.github.mrghxst.ethmensa.domain.model.NotificationSound
import io.github.mrghxst.ethmensa.domain.model.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalTime

// DataStore moves files with android.system.Os, so this needs Robolectric.
@RunWith(RobolectricTestRunner::class)
class PrefsTest {
    @get:Rule val tmp = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() = scope.cancel()

    private fun store(name: String) = PreferenceDataStoreFactory.create(scope = scope) { tmp.root.resolve("$name.preferences_pb") }

    @Test
    fun `defaults and round trip`() = runBlocking {
        val repo = SettingsRepository(store("settings"))
        assertEquals(Settings(), repo.current())
        assertEquals(LocalTime.of(11, 0), repo.current().notificationTime)
        repo.update {
            it.copy(
                mensaCellType = MensaCellType.LARGE,
                campus = Campus.IRCHEL,
                allergens = setOf(KnownAllergen.SULPHUR_DIOXIDE, KnownAllergen.WHEAT),
                notificationTime = LocalTime.of(9, 45),
                notificationSound = NotificationSound.CLICK,
            )
        }
        val saved = repo.current()
        assertEquals(MensaCellType.LARGE, saved.mensaCellType)
        assertEquals(Campus.IRCHEL, saved.campus)
        assertEquals(setOf(KnownAllergen.SULPHUR_DIOXIDE, KnownAllergen.WHEAT), saved.allergens)
        assertEquals(LocalTime.of(9, 45), saved.notificationTime)
        assertEquals(NotificationSound.CLICK, saved.notificationSound)
        repo.update { it.copy(campus = null) }
        assertEquals(null, repo.current().campus)
        repo.reset()
        assertEquals(Settings(), repo.current())
    }

    @Test
    fun `click counts`() = runBlocking {
        val clicks = ClickCountStore(store("clicks"))
        clicks.increment("eth/9")
        clicks.increment("eth/9")
        clicks.increment("uzh/1")
        assertEquals(mapOf("eth/9" to 2, "uzh/1" to 1), clicks.counts.first())
        clicks.reset()
        assertEquals(emptyMap<String, Int>(), clicks.counts.first())
    }
}
