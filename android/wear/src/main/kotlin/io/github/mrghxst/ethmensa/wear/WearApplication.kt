// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.wear

import android.app.Application
import android.content.Context
import android.location.Geocoder
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.github.mrghxst.ethmensa.data.AppLanguage
import io.github.mrghxst.ethmensa.data.MensaLabels
import io.github.mrghxst.ethmensa.data.MensaRepository
import io.github.mrghxst.ethmensa.data.MensaStore
import io.github.mrghxst.ethmensa.data.cache.WeeklyJsonCache
import io.github.mrghxst.ethmensa.data.eth.EthApi
import io.github.mrghxst.ethmensa.data.geo.MensaLocator
import io.github.mrghxst.ethmensa.data.geo.PlatformGeocoder
import io.github.mrghxst.ethmensa.data.net.createHttpClient
import io.github.mrghxst.ethmensa.data.prefs.ClickCountStore
import io.github.mrghxst.ethmensa.data.prefs.SettingsRepository
import io.github.mrghxst.ethmensa.data.zfv.OutletInfoRepository
import io.github.mrghxst.ethmensa.data.zfv.ZfvApi
import io.github.mrghxst.ethmensa.domain.zurichClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import java.util.Locale

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("settings")
private val Context.clickStore: DataStore<Preferences> by preferencesDataStore("clicks")
private val Context.geoCacheStore: DataStore<Preferences> by preferencesDataStore("geocache")
private val Context.outletInfoStore: DataStore<Preferences> by preferencesDataStore("outlet_info")

class WearApplication : Application() {
    lateinit var graph: WearGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = WearGraph(this)
    }
}

/**
 * The watch's dependency container: the same data layer as the phone (core module), with its own
 * settings on the watch, like the watchOS app.
 */
class WearGraph(context: Context) {
    private val appContext: Context = context.applicationContext
    val clock: Clock = zurichClock()
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val httpClient by lazy { createHttpClient(appContext.cacheDir, BuildConfig.VERSION_NAME) }

    val settings = SettingsRepository(appContext.settingsStore)
    val clickCounts = ClickCountStore(appContext.clickStore)

    private val locator by lazy {
        MensaLocator(
            bundledJson = { asset("mensa_coordinates.json") },
            cache = appContext.geoCacheStore,
            geocoder = PlatformGeocoder(Geocoder(appContext, Locale.forLanguageTag("de-CH"))),
        )
    }

    private val mensaRepository by lazy {
        val cacheDir = appContext.cacheDir.resolve("api")
        MensaRepository(
            ethApi = EthApi(httpClient),
            zfvApi = ZfvApi(httpClient, BuildConfig.ZFV_API_KEY, { asset("zfv_query.graphql").orEmpty() }),
            outletInfo = OutletInfoRepository(httpClient, appContext.outletInfoStore, { asset("zfv_uzh_mapping.json") }, clock),
            ethDirectoryCache = WeeklyJsonCache(cacheDir, "eth_directory", clock),
            ethRotasCache = WeeklyJsonCache(cacheDir, "eth_weekly_rotas", clock),
            zfvCache = WeeklyJsonCache(cacheDir, "zfv_outlets", clock),
            clock = clock,
            appLanguage = AppLanguage::current,
            labels = {
                MensaLabels(
                    menu = appContext.getString(R.string.menu),
                    lunch = appContext.getString(R.string.lunch),
                    dinner = appContext.getString(R.string.dinner),
                )
            },
        )
    }

    val mensaStore: MensaStore by lazy { MensaStore(mensaRepository, locator, appScope) }

    // Only called from the repositories, which run off the main thread.
    private fun asset(name: String): String? =
        runCatching { appContext.assets.open(name).bufferedReader().use { it.readText() } }.getOrNull()
}
