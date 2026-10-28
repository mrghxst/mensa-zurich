// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data

import android.util.Log
import io.github.mrghxst.ethmensa.data.cache.WeeklyJsonCache
import io.github.mrghxst.ethmensa.data.eth.EthApi
import io.github.mrghxst.ethmensa.data.eth.EthDirectoryMensa
import io.github.mrghxst.ethmensa.data.eth.EthMapper
import io.github.mrghxst.ethmensa.data.eth.EthRotaAnswer
import io.github.mrghxst.ethmensa.data.net.ApiJson
import io.github.mrghxst.ethmensa.data.net.HttpException
import io.github.mrghxst.ethmensa.data.net.InvalidResponseException
import io.github.mrghxst.ethmensa.data.zfv.OutletInfoRepository
import io.github.mrghxst.ethmensa.data.zfv.ZfvAnswer
import io.github.mrghxst.ethmensa.data.zfv.ZfvApi
import io.github.mrghxst.ethmensa.data.zfv.ZfvMapper
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.model.Provider
import io.github.mrghxst.ethmensa.domain.mondayOf
import io.github.mrghxst.ethmensa.domain.today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import kotlin.coroutines.cancellation.CancellationException

enum class ProviderError { NETWORK, API }

/**
 * One emission of one provider. Each provider first emits its cached mensas ([isCached],
 * not [isFinal]) if it has any, then one final result: fresh data, or the cache with an
 * [error] when the download failed.
 */
data class ProviderResult(
    val provider: Provider,
    val mensas: List<Mensa>,
    val isCached: Boolean,
    val isFinal: Boolean,
    val error: ProviderError? = null,
)

/** Localized labels the mappers need. */
data class MensaLabels(val menu: String, val lunch: String, val dinner: String)

/** API.swift: both providers run concurrently, cached results first, then fresh ones. */
class MensaRepository(
    private val ethApi: EthApi,
    private val zfvApi: ZfvApi,
    private val outletInfo: OutletInfoRepository,
    private val ethDirectoryCache: WeeklyJsonCache,
    private val ethRotasCache: WeeklyJsonCache,
    private val zfvCache: WeeklyJsonCache,
    private val clock: Clock,
    /** The app's UI language code (en, de, fr, it). */
    private val appLanguage: () -> String,
    private val labels: () -> MensaLabels,
) {
    fun observeMensas(): Flow<ProviderResult> = channelFlow {
        launch { eth().collect { send(it) } }
        launch { zfv().collect { send(it) } }
    }

    /** ETH menus are German or English (§5): fr and it get English. */
    fun ethLanguage(): String = if (appLanguage() == "de") "de" else "en"

    /** ZFV translates to de, fr, it and en, so UZH menus follow the app language (§5.7 item 4). */
    fun zfvLocale(): String = appLanguage().takeIf { it in ZFV_LOCALES } ?: "en"

    private fun eth(): Flow<ProviderResult> = flow {
        val lang = ethLanguage()
        val labels = labels()
        val today = today(clock)
        val cachedDirectory = ethDirectoryCache.read(lang)
        val cachedRotas = ethRotasCache.read(lang)
        val cached = if (cachedDirectory != null && cachedRotas != null) {
            convertEth(cachedDirectory, cachedRotas, today, labels)
        } else {
            null
        }
        if (cached != null) emit(ProviderResult(Provider.ETH, cached, isCached = true, isFinal = false))

        val directory = runCatching {
            ethApi.directory(lang).also {
                parseDirectory(it) ?: throw InvalidResponseException("Invalid ETH directory")
                ethDirectoryCache.write(lang, it)
            }
        }
        val rotas = runCatching {
            ethApi.weeklyRotas(lang, mondayOf(today)).also {
                // An answer without weekly rotas is an error response and must not replace the cache.
                parseRotas(it)?.weeklyRotas ?: throw InvalidResponseException("ETH answer without weekly rotas")
                ethRotasCache.write(lang, it)
            }
        }
        val failure = directory.exceptionOrNull() ?: rotas.exceptionOrNull()
        failure?.let { rethrowCancellation(it); Log.w(TAG, "ETH download failed", it) }
        val error = failure?.let(::classify)
        val directoryJson = directory.getOrNull() ?: cachedDirectory
        val rotasJson = rotas.getOrNull() ?: cachedRotas
        val mensas = if (directoryJson != null && rotasJson != null) {
            convertEth(directoryJson, rotasJson, today, labels)
        } else {
            null
        }
        if (mensas != null) {
            emit(ProviderResult(Provider.ETH, mensas, isCached = error != null, isFinal = true, error = error))
        } else {
            emit(ProviderResult(Provider.ETH, cached.orEmpty(), isCached = cached != null, isFinal = true, error = error))
        }
    }.flowOn(Dispatchers.Default)

    private fun zfv(): Flow<ProviderResult> = flow {
        if (!zfvApi.isConfigured) {
            // No key means the provider is disabled, not an error.
            emit(ProviderResult(Provider.UZH, emptyList(), isCached = false, isFinal = true))
            return@flow
        }
        val locale = zfvLocale()
        val labels = labels()
        val cachedJson = zfvCache.read(locale)
        val cached = cachedJson?.let { json ->
            parseZfv(json)?.data?.outlets?.let { ZfvMapper.toMensas(it, outletInfo.cached(), labels.zfv()) }
        }
        if (cached != null) emit(ProviderResult(Provider.UZH, cached, isCached = true, isFinal = false))

        val (download, infos) = coroutineScope {
            val infos = async { outletInfo.load() }
            runCatching { zfvApi.outlets(locale) } to infos.await()
        }
        val failure = download.exceptionOrNull()
        if (failure != null) {
            rethrowCancellation(failure)
            Log.w(TAG, "ZFV download failed", failure)
            emit(ProviderResult(Provider.UZH, cached.orEmpty(), isCached = cached != null, isFinal = true, error = classify(failure)))
            return@flow
        }
        val json = download.getOrThrow()
        val answer = parseZfv(json)
        val outlets = answer?.data?.outlets
        when {
            answer == null -> {
                Log.w(TAG, "ZFV answer could not be parsed")
                emit(ProviderResult(Provider.UZH, cached.orEmpty(), isCached = cached != null, isFinal = true, error = ProviderError.API))
            }
            !answer.errors.isNullOrEmpty() -> {
                // A response with errors may be incomplete, so it doesn't replace the cache.
                Log.w(TAG, "ZFV GraphQL errors: ${answer.errors.mapNotNull { it.message }}")
                emit(ProviderResult(Provider.UZH, cached.orEmpty(), isCached = cached != null, isFinal = true, error = ProviderError.API))
            }
            outlets.isNullOrEmpty() -> {
                // An invalid or rotated key returns 200 with no outlets. Keep the cache, no user-facing error.
                Log.w(TAG, "ZFV returned no outlets; the shared API key may have been rotated")
                emit(ProviderResult(Provider.UZH, cached.orEmpty(), isCached = cached != null, isFinal = true))
            }
            else -> {
                zfvCache.write(locale, json)
                emit(ProviderResult(Provider.UZH, ZfvMapper.toMensas(outlets, infos, labels.zfv()), isCached = false, isFinal = true))
            }
        }
    }.flowOn(Dispatchers.Default)

    suspend fun clearCaches() {
        ethDirectoryCache.clear()
        ethRotasCache.clear()
        zfvCache.clear()
    }

    private fun MensaLabels.zfv() = ZfvMapper.Labels(lunch = lunch, dinner = dinner)

    private fun convertEth(directory: String, rotas: String, today: java.time.LocalDate, labels: MensaLabels): List<Mensa>? {
        val dir = parseDirectory(directory) ?: return null
        val answer = parseRotas(rotas) ?: return null
        return EthMapper.toMensas(dir, answer, today, labels.menu)
    }

    private fun parseDirectory(json: String): List<EthDirectoryMensa>? =
        runCatching { ApiJson.decodeFromString<List<EthDirectoryMensa>>(json) }.getOrNull()

    private fun parseRotas(json: String): EthRotaAnswer? =
        runCatching { ApiJson.decodeFromString<EthRotaAnswer>(json) }.getOrNull()

    private fun parseZfv(json: String): ZfvAnswer? =
        runCatching { ApiJson.decodeFromString<ZfvAnswer>(json) }.getOrNull()

    private fun classify(error: Throwable): ProviderError = when (error) {
        is HttpException, is InvalidResponseException -> ProviderError.API
        is IOException -> ProviderError.NETWORK
        else -> ProviderError.API
    }

    private fun rethrowCancellation(error: Throwable) {
        if (error is CancellationException) throw error
    }

    private companion object {
        const val TAG = "MensaRepository"
        val ZFV_LOCALES = setOf("de", "fr", "it", "en")
    }
}
