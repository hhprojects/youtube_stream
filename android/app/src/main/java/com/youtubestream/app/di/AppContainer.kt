package com.youtubestream.app.di

import android.content.Context
import androidx.room.Room
import com.youtubestream.app.data.local.AppDatabase
import com.youtubestream.app.data.remote.BaseUrlInterceptor
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.repository.DownloadRepository
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.PiLibraryRepository
import com.youtubestream.app.data.repository.SearchRepository
import com.youtubestream.app.data.settings.DEFAULT_SERVER_URL
import com.youtubestream.app.data.settings.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    private val apiClient = OkHttpClient.Builder()
        .addInterceptor(BaseUrlInterceptor { currentUrl })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val fileClient = OkHttpClient.Builder()
        .readTimeout(300, TimeUnit.SECONDS)  // big downloads
        .build()

    private val api: YoutubeStreamApi = Retrofit.Builder()
        .baseUrl("http://placeholder/")      // swapped per-request by the interceptor
        .client(apiClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(YoutubeStreamApi::class.java)

    private val db = Room.databaseBuilder(context, AppDatabase::class.java, "library.db").build()
    private val songsDir = File(context.filesDir, "songs")

    val searchRepository = SearchRepository(api)
    val libraryRepository = LibraryRepository(db.libraryDao())
    val piLibraryRepository = PiLibraryRepository(api)
    val downloadRepository = DownloadRepository(api, fileClient, db.libraryDao(), songsDir) { currentUrl }
}
