package com.youtubestream.app.di

import android.content.Context
import androidx.room.Room
import com.youtubestream.app.data.local.AppDatabase
import com.youtubestream.app.data.network.ConnectivityObserver
import com.youtubestream.app.data.network.ServerReachability
import com.youtubestream.app.data.network.ServerReachabilityInterceptor
import com.youtubestream.app.data.playback.QueueDataStore
import com.youtubestream.app.data.remote.BaseUrlInterceptor
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.repository.DownloadRepository
import com.youtubestream.app.data.repository.ImportDownloadManager
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.PiLibraryRepository
import com.youtubestream.app.data.repository.SearchRepository
import com.youtubestream.app.data.settings.DEFAULT_SERVER_URL
import com.youtubestream.app.data.settings.SettingsDataStore
import com.youtubestream.app.playback.PlaybackConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/** Hand-rolled dependency graph (Hilt deferred). One instance, held by [com.youtubestream.app.App]. */
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = SettingsDataStore(context)

    // Cache the current URL so the (synchronous) interceptor can read it per request.
    @Volatile
    private var currentUrl: String = DEFAULT_SERVER_URL
    init { settings.serverUrl.onEach { currentUrl = it }.launchIn(appScope) }

    private val json = Json { ignoreUnknownKeys = true }

    // Reachability: device connectivity + a Pi probe (GET /api/library), merged into one app-wide status.
    // Declared before apiClient because the interceptor below calls serverReachability.report(...).
    val connectivity = ConnectivityObserver(context)
    // Explicit type breaks a type-inference cycle: apiClient's interceptor calls serverReachability.report,
    // while this initializer's probeAction references api (built from apiClient).
    val serverReachability: ServerReachability = ServerReachability(connectivity, appScope, probeAction = { api.library() })

    private val apiClient = OkHttpClient.Builder()
        .addInterceptor(BaseUrlInterceptor { currentUrl })
        .addInterceptor(ServerReachabilityInterceptor { ok -> serverReachability.report(ok) })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // The /api/download POST blocks while yt-dlp fetches from YouTube — that can take minutes, so it
    // needs a long read timeout. A separate client keeps search/library on 30s (they should fail fast).
    private val downloadClient = OkHttpClient.Builder()
        .addInterceptor(BaseUrlInterceptor { currentUrl })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    // The interceptor makes the file fetch follow the configured Pi URL too: the Pi bakes a static
    // host into downloadUrl, but /downloads is same-origin with /api, so rewriting host/port is correct.
    private val fileClient = OkHttpClient.Builder()
        .addInterceptor(BaseUrlInterceptor { currentUrl })
        .readTimeout(300, TimeUnit.SECONDS)  // big downloads
        .build()

    private fun buildApi(client: OkHttpClient): YoutubeStreamApi = Retrofit.Builder()
        .baseUrl("http://placeholder/")      // swapped per-request by the interceptor
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(YoutubeStreamApi::class.java)

    private val api = buildApi(apiClient)               // search, library, delete — 30s timeout
    private val downloadApi = buildApi(downloadClient)  // the slow yt-dlp POST — 300s timeout

    private val db = Room.databaseBuilder(context, AppDatabase::class.java, "library.db")
        .fallbackToDestructiveMigration(dropAllTables = true)  // rows re-derive from the Pi via Import
        .build()
    private val songsDir = File(context.filesDir, "songs")

    val searchRepository = SearchRepository(api)
    val libraryRepository = LibraryRepository(db.libraryDao())
    val piLibraryRepository = PiLibraryRepository(api)
    val downloadRepository = DownloadRepository(downloadApi, fileClient, db.libraryDao(), songsDir) { currentUrl }

    // Bulk Pi imports run here (app-scoped), so they keep going if the user leaves the Import screen.
    val importDownloadManager = ImportDownloadManager(downloadRepository, appScope)

    // MediaController is main-thread-confined, so the connection (and its position loop) runs on Main.
    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** App-scoped: connected once, never released — it lives for the process like the player itself. */
    val playbackConnection =
        PlaybackConnection(context, playbackScope, QueueDataStore(context)).also { it.connect() }

    init {
        // Self-heal: a track that failed to play (missing local file) is pruned from the library.
        // The Pi copy stays, so it reappears in Import for re-download.
        playbackConnection.errors
            .onEach { id -> libraryRepository.deleteById(id) }
            .launchIn(playbackScope)

        // One probe at startup so the banner/gating reflect the Pi before the user touches anything.
        // Placed in this (final) init block so every property — including `api` — is initialized first.
        appScope.launch { serverReachability.probe() }
    }
}
