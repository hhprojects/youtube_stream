package com.youtubestream.app.di

import android.content.Context
import androidx.room.Room
import com.youtubestream.app.data.local.AppDatabase
import com.youtubestream.app.data.local.MIGRATION_3_4
import com.youtubestream.app.data.local.MIGRATION_4_5
import com.youtubestream.app.data.local.MIGRATION_5_6
import com.youtubestream.app.data.local.MIGRATION_6_7
import com.youtubestream.app.data.local.MIGRATION_7_8
import com.youtubestream.app.data.local.MIGRATION_8_9
import com.youtubestream.app.data.network.ConnectivityObserver
import com.youtubestream.app.data.network.ServerReachability
import com.youtubestream.app.data.network.ServerReachabilityInterceptor
import com.youtubestream.app.data.playback.QueueDataStore
import com.youtubestream.app.data.remote.BaseUrlInterceptor
import com.youtubestream.app.data.remote.DiscoveryApi
import com.youtubestream.app.data.remote.LyricsApi
import com.youtubestream.app.data.remote.PodcastApi
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.repository.DownloadRepository
import com.youtubestream.app.data.repository.ImportDownloadManager
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.LyricsRepository
import com.youtubestream.app.data.repository.PiLibraryRepository
import com.youtubestream.app.data.repository.PlayHistoryRepository
import com.youtubestream.app.data.repository.PlaylistRepository
import com.youtubestream.app.data.repository.PodcastRepository
import com.youtubestream.app.data.repository.DiscoveryRepository
import com.youtubestream.app.data.repository.RecentSearchRepository
import com.youtubestream.app.data.repository.SearchRepository
import com.youtubestream.app.data.settings.DEFAULT_SERVER_URL
import com.youtubestream.app.data.settings.SettingsDataStore
import com.youtubestream.app.playback.PlaybackConnection
import com.youtubestream.app.playback.PodcastProgressWriter
import com.youtubestream.app.ui.download.DownloadQueue
import com.youtubestream.app.ui.download.PodcastDownloads
import com.youtubestream.app.ui.download.SongDownloads
import com.youtubestream.app.widget.WidgetUpdater
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

    // Short CONNECT timeout (~5s): an unreachable/powered-off host stalls only the TCP handshake, so fail it
    // fast — that's what makes the reachability probe (and search/delete) react quickly instead of hanging
    // ~30s. READ stays 30s, so a live-but-slow Pi (slow to build a response) still succeeds and is never
    // mis-reported as offline. (Tailscale cold-starts are usually well under 5s; Retry covers the rest.)
    private val apiClient = OkHttpClient.Builder()
        .addInterceptor(BaseUrlInterceptor { currentUrl })
        .addInterceptor(ServerReachabilityInterceptor { ok -> serverReachability.report(ok) })
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // The /api/download POST blocks while yt-dlp fetches from YouTube — that can take minutes, so it
    // needs a long read timeout. A separate client keeps search/library fast-failing (they should fail fast).
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

    // A 1-2h episode extraction on the Pi can far exceed the 5-min song download, so the podcast
    // download POST needs a long read timeout (>= the backend's 30-min ceiling).
    private val podcastDownloadClient = OkHttpClient.Builder()
        .addInterceptor(BaseUrlInterceptor { currentUrl })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(1800, TimeUnit.SECONDS)
        .build()

    private inline fun <reified T> buildApi(client: OkHttpClient): T = Retrofit.Builder()
        .baseUrl("http://placeholder/")      // swapped per-request by the interceptor
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(T::class.java)

    private val api = buildApi<YoutubeStreamApi>(apiClient)               // search, library, delete — 30s timeout
    private val downloadApi = buildApi<YoutubeStreamApi>(downloadClient)  // the slow yt-dlp POST — 300s timeout
    private val discoveryApi = buildApi<DiscoveryApi>(apiClient)          // cached on the Pi → 30s is plenty
    private val lyricsApi = buildApi<LyricsApi>(apiClient)                // lrclib proxy on the Pi → 30s is plenty
    private val podcastApi = buildApi<PodcastApi>(apiClient)                       // home/show — cached on Pi
    private val podcastDownloadApi = buildApi<PodcastApi>(podcastDownloadClient)   // the slow episode download POST

    private val db = Room.databaseBuilder(context, AppDatabase::class.java, "library.db")
        .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
        .fallbackToDestructiveMigration(dropAllTables = true)  // net for unhandled jumps only
        .build()
    private val songsDir = File(context.filesDir, "songs")
    private val podcastsDir = File(context.filesDir, "podcasts")

    val searchRepository = SearchRepository(api)
    val discoveryRepository = DiscoveryRepository(discoveryApi)
    val lyricsRepository = LyricsRepository(lyricsApi, db.lyricsDao())
    val libraryRepository = LibraryRepository(db.libraryDao())
    val playHistoryRepository = PlayHistoryRepository(db.playEventDao())
    val playlistRepository = PlaylistRepository(db.playlistDao())
    val recentSearchRepository = RecentSearchRepository(db.recentSearchDao())
    val piLibraryRepository = PiLibraryRepository(api)
    val downloadRepository = DownloadRepository(downloadApi, fileClient, db.libraryDao(), songsDir) { currentUrl }
    val podcastRepository = PodcastRepository(
        api = podcastApi,
        downloadApi = podcastDownloadApi,
        dao = db.podcastDao(),
        fileClient = fileClient,
        podcastsDir = podcastsDir,
        baseUrl = { currentUrl },
    )

    // Downloads run on an app-lifetime, Main-confined scope so they survive leaving the screen and the
    // queue bookkeeping stays single-threaded (the repo flows do their blocking IO via flowOn(IO)).
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val songDownloads = SongDownloads(DownloadQueue(downloadScope), downloadRepository)
    val podcastDownloads = PodcastDownloads(DownloadQueue(downloadScope), podcastRepository)

    // Bulk Pi imports run here (app-scoped), so they keep going if the user leaves the Import screen.
    val importDownloadManager = ImportDownloadManager(downloadRepository, appScope)

    // MediaController is main-thread-confined, so the connection (and its position loop) runs on Main.
    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Constructed but NOT connected/started — startPlayback() wires them up. A background WorkManager wake
    // builds AppContainer but only touches podcastRepository, so the media stack never spins up off-UI.
    val playbackConnection = PlaybackConnection(context, playbackScope, QueueDataStore(context))
    val widgetUpdater = WidgetUpdater(context, playbackConnection, appScope)
    val podcastProgressWriter = PodcastProgressWriter(playbackConnection, podcastRepository, playbackScope)

    @Volatile private var playbackStarted = false

    /**
     * Connect the player + start the widget mirror, resume-writer, self-heal/play-history collectors, and
     * the reachability probe. Idempotent. Called only from UI / widget-command entry points — never from the
     * background episode check, so an idle-device wake never binds MediaSessionService.
     */
    fun startPlayback() {
        if (playbackStarted) return
        playbackStarted = true
        playbackConnection.connect()
        widgetUpdater.start()
        podcastProgressWriter.start()

        // Self-heal: a track that failed to play (missing local file) is pruned from the library.
        // The Pi copy stays, so it reappears in Import for re-download.
        playbackConnection.errors
            .onEach { id -> libraryRepository.deleteById(id) }
            .launchIn(playbackScope)

        // Record every track-start to on-device play-history (powers the For You home, fully offline).
        playbackConnection.playStarts
            .onEach { id -> playHistoryRepository.record(id, System.currentTimeMillis()) }
            .launchIn(playbackScope)

        // One probe at startup so the banner/gating reflect the Pi before the user touches anything.
        appScope.launch { serverReachability.probe() }
    }
}
