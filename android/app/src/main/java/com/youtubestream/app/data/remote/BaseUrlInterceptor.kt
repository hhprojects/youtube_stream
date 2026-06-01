package com.youtubestream.app.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites every request's scheme/host/port to whatever [currentBaseUrl] returns *right now*,
 * keeping the path. Retrofit is built with a throwaway placeholder base URL; this is what makes
 * a Settings URL change take effect on the next request with no restart.
 */
class BaseUrlInterceptor(private val currentBaseUrl: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val base = currentBaseUrl().toHttpUrl()
        val request = chain.request()
        val newUrl = request.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()
        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
