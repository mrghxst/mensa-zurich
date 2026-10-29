// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.data.net

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** JSON settings for all APIs: they omit fields freely (§5). */
val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

fun userAgent(versionName: String) = "MensaZurich-Android/$versionName (+https://github.com/mrghxst/ethmensa)"

/** One shared client: 15 s timeouts, our User-Agent and a 10 MB HTTP cache. */
fun createHttpClient(cacheDir: File?, versionName: String): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .apply { if (cacheDir != null) cache(Cache(File(cacheDir, "http"), 10L * 1024 * 1024)) }
    .addInterceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent(versionName)).build())
    }
    .build()

class HttpException(val code: Int, message: String) : IOException("HTTP $code: $message")

/** The server answered, but not with usable data. */
class InvalidResponseException(message: String) : IOException(message)

/** Runs the call without blocking a thread and cancels it with the coroutine. */
suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(
        object : Callback {
            override fun onResponse(call: Call, response: Response) = cont.resume(response)

            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isCancelled) cont.resumeWithException(e)
            }
        },
    )
}

/** Returns the response body as text, throwing [HttpException] for non-2xx codes. */
suspend fun OkHttpClient.fetchText(request: Request): String {
    val response = newCall(request).await()
    return response.use {
        if (!it.isSuccessful) throw HttpException(it.code, it.message)
        it.body.string()
    }
}
