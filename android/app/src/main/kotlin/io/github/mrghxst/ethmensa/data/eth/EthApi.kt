// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.eth

import io.github.mrghxst.ethmensa.data.net.fetchText
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate

/** The two ETH endpoints (§5.1, §5.2). Returns raw JSON so it can be cached as-is. */
class EthApi(
    private val client: OkHttpClient,
    private val directoryUrl: HttpUrl = "https://glyph.ethz.ch/eth-ws/mensas/detail".toHttpUrl(),
    private val rotasUrl: HttpUrl = "https://idapps.ethz.ch/cookpit-pub-services/v1/weeklyrotas".toHttpUrl(),
) {
    suspend fun directory(lang: String): String = client.fetchText(
        request(directoryUrl.newBuilder().addQueryParameter("lang", lang).build(), lang),
    )

    /** Rotas from [monday] (ISO Monday of the week) to [monday] + 14 days. */
    suspend fun weeklyRotas(lang: String, monday: LocalDate): String {
        val url = rotasUrl.newBuilder()
            .addQueryParameter("client-id", "ethz-wcms")
            .addQueryParameter("lang", lang)
            .addQueryParameter("rs-first", "0")
            .addQueryParameter("rs-size", "50")
            .addQueryParameter("valid-after", monday.toString())
            .addQueryParameter("valid-before", monday.plusDays(14).toString())
            .build()
        return client.fetchText(request(url, lang))
    }

    private fun request(url: HttpUrl, lang: String) = Request.Builder()
        .url(url)
        .header("Accept", "application/json")
        .header("Accept-Language", if (lang == "de") "de-DE;de;q=0.9" else "en-EN,en;q=0.9")
        .build()
}
