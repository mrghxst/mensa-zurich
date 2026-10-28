// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.mrghxst.ethmensa.data.cache.WeeklyJsonCache
import io.github.mrghxst.ethmensa.data.eth.EthApi
import io.github.mrghxst.ethmensa.data.zfv.OutletInfoRepository
import io.github.mrghxst.ethmensa.data.zfv.ZfvApi
import io.github.mrghxst.ethmensa.domain.model.Provider
import io.github.mrghxst.ethmensa.fixedClock
import io.github.mrghxst.ethmensa.fixture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class MensaRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private val server = MockWebServer()
    private val clock = fixedClock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Status code per path; 200 serves the fixture. */
    private val codes = mutableMapOf("/eth/dir" to 200, "/eth/rotas" to 200, "/zfv" to 200, "/mapping" to 200)
    private var zfvBody = fixture("zfv_en.json")
    private var delaySeconds = 0L

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                val code = codes[path] ?: 404
                val body = when (path) {
                    "/eth/dir" -> fixture("eth_directory_en.json")
                    "/eth/rotas" -> fixture("eth_rotas_en.json")
                    "/zfv" -> zfvBody
                    "/mapping" -> fixture("zfv_uzh_mapping.json")
                    else -> ""
                }
                return MockResponse.Builder()
                    .code(code)
                    .body(if (code == 200) body else "error")
                    .headersDelay(delaySeconds, TimeUnit.SECONDS)
                    .build()
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
        scope.cancel()
    }

    private val client = OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build()
    private val cacheDir by lazy { tmp.newFolder("api") }

    private fun repository(apiKey: String = "key", language: String = "en"): MensaRepository {
        val store = PreferenceDataStoreFactory.create(scope = scope) { tmp.root.resolve("outlet_${System.nanoTime()}.preferences_pb") }
        return MensaRepository(
            ethApi = EthApi(client, server.url("/eth/dir"), server.url("/eth/rotas")),
            zfvApi = ZfvApi(client, apiKey, { "query" }, server.url("/zfv")),
            outletInfo = OutletInfoRepository(client, store, { fixture("zfv_uzh_mapping.json") }, clock, server.url("/mapping").toString()),
            ethDirectoryCache = WeeklyJsonCache(cacheDir, "eth_directory", clock),
            ethRotasCache = WeeklyJsonCache(cacheDir, "eth_weekly_rotas", clock),
            zfvCache = WeeklyJsonCache(cacheDir, "zfv_outlets", clock),
            clock = clock,
            appLanguage = { language },
            labels = { MensaLabels("Menu", "Lunch", "Dinner") },
        )
    }

    private fun collect(repo: MensaRepository) = runBlocking { repo.observeMensas().toList() }

    @Test
    fun `first run emits only fresh results`() {
        val results = collect(repository())
        val eth = results.filter { it.provider == Provider.ETH }
        val uzh = results.filter { it.provider == Provider.UZH }
        assertEquals(1, eth.size)
        assertFalse(eth.single().isCached)
        assertTrue(eth.single().isFinal)
        assertEquals(22, eth.single().mensas.size)
        assertTrue(uzh.single().mensas.isNotEmpty())
        assertNull(uzh.single().error)
    }

    @Test
    fun `second run emits cached first then fresh`() {
        collect(repository())
        val results = collect(repository())
        listOf(Provider.ETH, Provider.UZH).forEach { provider ->
            val emissions = results.filter { it.provider == provider }
            assertEquals(listOf(true, false), emissions.map { it.isCached })
            assertEquals(listOf(false, true), emissions.map { it.isFinal })
            assertEquals(emissions[0].mensas, emissions[1].mensas)
        }
    }

    @Test
    fun `server error falls back to the cache with an error`() {
        collect(repository())
        codes["/eth/rotas"] = 500
        codes["/zfv"] = 500
        val results = collect(repository())
        val ethFinal = results.last { it.provider == Provider.ETH }
        assertTrue(ethFinal.isCached)
        assertEquals(ProviderError.API, ethFinal.error)
        assertEquals(22, ethFinal.mensas.size)
        assertTrue(ethFinal.mensas.any { it.mealTimes.isNotEmpty() })
        val uzhFinal = results.last { it.provider == Provider.UZH }
        assertTrue(uzhFinal.mensas.isNotEmpty())
        assertEquals(ProviderError.API, uzhFinal.error)
    }

    @Test
    fun `timeout without cache yields an empty list and a network error`() {
        delaySeconds = 3
        val results = collect(repository())
        val eth = results.single { it.provider == Provider.ETH }
        assertTrue(eth.mensas.isEmpty())
        assertEquals(ProviderError.NETWORK, eth.error)
    }

    @Test
    fun `failed directory without cache yields no eth mensas`() {
        codes["/eth/dir"] = 500
        val eth = collect(repository()).single { it.provider == Provider.ETH }
        assertTrue(eth.mensas.isEmpty())
        assertEquals(ProviderError.API, eth.error)
    }

    @Test
    fun `zfv without outlets keeps the cache and shows no error`() {
        collect(repository())
        zfvBody = """{"data":{"outlets":[]}}"""
        val uzh = collect(repository()).last { it.provider == Provider.UZH }
        assertTrue(uzh.mensas.isNotEmpty())
        assertNull(uzh.error)
    }

    @Test
    fun `zfv graphql errors keep the cache`() {
        collect(repository())
        zfvBody = """{"errors":[{"message":"boom"}],"data":{"outlets":[]}}"""
        val uzh = collect(repository()).last { it.provider == Provider.UZH }
        assertTrue(uzh.mensas.isNotEmpty())
        assertEquals(ProviderError.API, uzh.error)
    }

    @Test
    fun `no key disables zfv without a request`() {
        val uzh = collect(repository(apiKey = "")).single { it.provider == Provider.UZH }
        assertTrue(uzh.mensas.isEmpty())
        assertNull(uzh.error)
        assertTrue(generateSequence { server.takeRequest(10, TimeUnit.MILLISECONDS) }.none { it.url.encodedPath == "/zfv" })
    }

    @Test
    fun `languages`() {
        assertEquals("en", repository(language = "fr").ethLanguage())
        assertEquals("fr", repository(language = "fr").zfvLocale())
        assertEquals("de", repository(language = "de").ethLanguage())
    }

    @Test
    fun `rotas request uses iso monday and two weeks`() {
        collect(repository())
        val rotas = generateSequence { server.takeRequest(10, TimeUnit.MILLISECONDS) }.first { it.url.encodedPath == "/eth/rotas" }
        assertEquals(LocalDate.of(2026, 10, 26).toString(), rotas.url.queryParameter("valid-after"))
        assertEquals(LocalDate.of(2026, 11, 9).toString(), rotas.url.queryParameter("valid-before"))
        assertEquals("ethz-wcms", rotas.url.queryParameter("client-id"))
    }
}

class WeeklyJsonCacheTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `valid only in the same week and language`() = runBlocking {
        val dir = tmp.newFolder()
        WeeklyJsonCache(dir, "x", fixedClock()).write("en", "payload")
        assertEquals("payload", WeeklyJsonCache(dir, "x", fixedClock()).read("en"))
        assertNull(WeeklyJsonCache(dir, "x", fixedClock()).read("de"))
        // Sunday 2026-11-01 is still ISO week 44, Monday 2026-11-02 is not.
        assertEquals("payload", WeeklyJsonCache(dir, "x", fixedClock(LocalDate.of(2026, 11, 1).atTime(23, 0))).read("en"))
        assertNull(WeeklyJsonCache(dir, "x", fixedClock(LocalDate.of(2026, 11, 2).atTime(0, 30))).read("en"))
    }

    @Test
    fun `corrupt file reads as missing`() = runBlocking {
        val dir = tmp.newFolder()
        dir.resolve("x.json").writeText("{not json")
        assertNull(WeeklyJsonCache(dir, "x", fixedClock()).read("en"))
    }
}
