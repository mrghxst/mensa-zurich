// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.zfv

import io.github.mrghxst.ethmensa.data.net.fetchText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** ZFV GraphQL endpoint (§5.3). Returns raw JSON so it can be cached as-is. */
class ZfvApi(
    private val client: OkHttpClient,
    private val apiKey: String,
    private val query: () -> String,
    private val url: HttpUrl = "https://api.zfv.ch/graphql".toHttpUrl(),
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    suspend fun outlets(locale: String): String {
        val body = buildJsonObject { put("query", query()) }
        val request = Request.Builder()
            .url(url)
            .header("api-key", apiKey)
            .header("locale", locale)
            .post(Json.encodeToString(body).toRequestBody("application/json".toMediaType()))
            .build()
        return client.fetchText(request)
    }
}
