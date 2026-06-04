package com.youtubestream.app.data.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Reports Pi reachability as a side effect of real traffic: any HTTP response (even 4xx/5xx) means the
 * Pi answered → reachable; an [IOException] (connection refused, timeout, unknown host) means unreachable.
 *
 * Install on the *fast* api client ONLY. The download/file clients use a 300s read timeout for slow
 * yt-dlp, where a read timeout is not "Pi down" — letting them report would produce false negatives.
 */
class ServerReachabilityInterceptor(private val onResult: (Boolean) -> Unit) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = try {
        chain.proceed(chain.request()).also { onResult(true) }
    } catch (e: IOException) {
        onResult(false)
        throw e
    }
}
