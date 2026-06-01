package com.youtubestream.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class BaseUrlInterceptorTest {

    private fun get(client: OkHttpClient, path: String) =
        client.newCall(Request.Builder().url("http://placeholder$path").build()).execute().close()

    @Test
    fun routesRequestToCurrentServerUrlAndKeepsThePath() {
        val server = MockWebServer().apply { start(); enqueue(MockResponse().setBody("ok")) }
        val client = OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor { server.url("/").toString().removeSuffix("/") })
            .build()

        get(client, "/api/library")

        val recorded = server.takeRequest()
        assertEquals("/api/library", recorded.path)            // path preserved
        assertEquals(server.port, recorded.requestUrl!!.port)  // routed to the current server
        server.shutdown()
    }

    @Test
    fun picksUpUrlChangesBetweenRequests() {
        val serverA = MockWebServer().apply { start(); enqueue(MockResponse().setBody("a")) }
        val serverB = MockWebServer().apply { start(); enqueue(MockResponse().setBody("b")) }
        var current = serverA.url("/").toString().removeSuffix("/")
        val client = OkHttpClient.Builder().addInterceptor(BaseUrlInterceptor { current }).build()

        get(client, "/api/x")
        current = serverB.url("/").toString().removeSuffix("/") // "Settings changed the URL"
        get(client, "/api/x")

        assertEquals(serverA.port, serverA.takeRequest().requestUrl!!.port) // first hit A
        assertEquals(serverB.port, serverB.takeRequest().requestUrl!!.port) // second hit B
        serverA.shutdown(); serverB.shutdown()
    }
}
