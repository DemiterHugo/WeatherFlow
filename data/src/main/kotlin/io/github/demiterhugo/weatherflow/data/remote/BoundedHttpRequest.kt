package io.github.demiterhugo.weatherflow.data.remote

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal data class HttpBytes(val code: Int, val bytes: ByteArray, val headers: Headers) {
    val successful: Boolean get() = code in 200..299
}

/** Cancellation remains attached until the bounded body has been read and closed. */
internal suspend fun OkHttpClient.readBytes(request: Request, maxBytes: Int): HttpBytes =
    suspendCancellableCoroutine { continuation ->
        val call = newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val result = try {
                    response.use {
                        val bytes = if (it.isSuccessful) {
                            val body = it.body ?: throw IOException("Empty HTTP response")
                            if (body.contentLength() > maxBytes) throw IOException("HTTP response exceeds size limit")
                            val source = body.source()
                            source.request(maxBytes.toLong() + 1)
                            source.buffer.readByteArray().also { data ->
                                if (data.size > maxBytes) throw IOException("HTTP response exceeds size limit")
                            }
                        } else byteArrayOf()
                        HttpBytes(it.code, bytes, it.headers)
                    }
                } catch (e: IOException) {
                    continuation.resumeWithException(e)
                    return
                }
                continuation.resume(result)
            }
        })
    }
