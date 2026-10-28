// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Geocoder
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.github.mrghxst.ethmensa.data.AppLanguage
import io.github.mrghxst.ethmensa.data.MensaLabels
import io.github.mrghxst.ethmensa.data.MensaRepository
import io.github.mrghxst.ethmensa.data.cache.WeeklyJsonCache
import io.github.mrghxst.ethmensa.data.eth.EthApi
import io.github.mrghxst.ethmensa.data.geo.MensaLocator
import io.github.mrghxst.ethmensa.data.geo.PlatformGeocoder
import io.github.mrghxst.ethmensa.data.net.Connectivity
import io.github.mrghxst.ethmensa.data.net.createHttpClient
import io.github.mrghxst.ethmensa.data.prefs.ClickCountStore
import io.github.mrghxst.ethmensa.data.prefs.SettingsRepository
import io.github.mrghxst.ethmensa.data.zfv.OutletInfoRepository
import io.github.mrghxst.ethmensa.data.zfv.ZfvApi
import io.github.mrghxst.ethmensa.domain.zurichClock
import okhttp3.OkHttpClient
import java.time.Clock
import java.util.Locale

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("settings")
private val Context.clickStore: DataStore<Preferences> by preferencesDataStore("clicks")
private val Context.geoCacheStore: DataStore<Preferences> by preferencesDataStore("geocache")
private val Context.outletInfoStore: DataStore<Preferences> by preferencesDataStore("outlet_info")

/** Manual dependency container, created once in [EthMensaApplication]. */
class AppGraph(context: Context) {
    val appContext: Context = context.applicationContext
    val clock: Clock = zurichClock()

    val httpClient: OkHttpClient by lazy { createHttpClient(appContext.cacheDir, BuildConfig.VERSION_NAME) }

    val settings = SettingsRepository(appContext.settingsStore)
    val clickCounts = ClickCountStore(appContext.clickStore)
    val connectivity = Connectivity(appContext)

    val locator: MensaLocator by lazy {
        MensaLocator(
            bundledJson = { asset("mensa_coordinates.json") },
            cache = appContext.geoCacheStore,
            geocoder = PlatformGeocoder(Geocoder(appContext, Locale.forLanguageTag("de-CH"))),
        )
    }

    val outletInfo: OutletInfoRepository by lazy {
        OutletInfoRepository(httpClient, appContext.outletInfoStore, { asset("zfv_uzh_mapping.json") }, clock)
    }

    val mensaRepository: MensaRepository by lazy {
        val cacheDir = appContext.cacheDir.resolve("api")
        MensaRepository(
            ethApi = EthApi(httpClient),
            zfvApi = ZfvApi(httpClient, BuildConfig.ZFV_API_KEY, { asset("zfv_query.graphql").orEmpty() }),
            outletInfo = outletInfo,
            ethDirectoryCache = WeeklyJsonCache(cacheDir, "eth_directory", clock),
            ethRotasCache = WeeklyJsonCache(cacheDir, "eth_weekly_rotas", clock),
            zfvCache = WeeklyJsonCache(cacheDir, "zfv_outlets", clock),
            clock = clock,
            appLanguage = AppLanguage::current,
            labels = {
                val res = localizedResources()
                MensaLabels(menu = res.getString(R.string.menu), lunch = res.getString(R.string.lunch), dinner = res.getString(R.string.dinner))
            },
        )
    }

    /** Resources in the current app language, also on API < 33 where the app context may lag behind. */
    fun localizedResources(): Resources {
        val config = Configuration(appContext.resources.configuration)
        config.setLocale(Locale.forLanguageTag(AppLanguage.current()))
        return appContext.createConfigurationContext(config).resources
    }

    private fun asset(name: String): String? =
        runCatching { appContext.assets.open(name).bufferedReader().use { it.readText() } }.getOrNull()
}
