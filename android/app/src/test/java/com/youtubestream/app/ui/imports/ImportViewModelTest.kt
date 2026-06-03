package com.youtubestream.app.ui.imports

import com.youtubestream.app.data.local.LibraryDao
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.model.DownloadState
import com.youtubestream.app.data.model.PiSong
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.remote.dto.DeleteResponseDto
import com.youtubestream.app.data.remote.dto.DownloadRequestDto
import com.youtubestream.app.data.remote.dto.LibraryResponseDto
import com.youtubestream.app.data.remote.dto.LibrarySongDto
import com.youtubestream.app.data.remote.dto.SearchRequestDto
import com.youtubestream.app.data.repository.Importer
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.PiLibraryRepository
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ImportViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()

    private fun fakeApi(
        onLibrary: suspend () -> LibraryResponseDto,
        onDelete: suspend (String) -> DeleteResponseDto = { error("unused") },
    ) = object : YoutubeStreamApi {
        override suspend fun search(body: SearchRequestDto) = error("unused")
        override suspend fun download(body: DownloadRequestDto) = error("unused")
        override suspend fun library() = onLibrary()
        override suspend fun deleteFromPi(filename: String) = onDelete(filename)
    }

    private class FakeDao(initial: List<LibrarySong> = emptyList()) : LibraryDao {
        val songs = MutableStateFlow(initial)
        override fun observeAll(): Flow<List<LibrarySong>> = songs
        override suspend fun exists(id: String) = songs.value.any { it.id == id }
        override suspend fun insert(song: LibrarySong) = songs.update { it + song }
        override suspend fun deleteById(id: String) = songs.update { l -> l.filterNot { it.id == id } }
    }

    private fun piDto(id: String) =
        LibrarySongDto(id, "T$id", "A$id", "Unknown", "$id.m4a", "http://pi/downloads/$id.m4a", 1L, "d")

    private fun vm(
        pi: List<LibrarySongDto> = emptyList(),
        local: List<LibrarySong> = emptyList(),
        importer: Importer = Importer { emptyFlow() },
        onDelete: suspend (String) -> DeleteResponseDto = { error("unused") },
    ) = ImportViewModel(
        pi = PiLibraryRepository(fakeApi({ LibraryResponseDto(pi) }, onDelete)),
        library = LibraryRepository(FakeDao(local)),
        importer = importer,
    )

    @Test fun importableIsPiMinusLocal() = runTest {
        val vm = vm(
            pi = listOf(piDto("s1"), piDto("s2")),
            local = listOf(LibrarySong("s1", "T", "A", 0, "s1.m4a", "/p/s1.m4a", 1L, 1L)),
        )
        backgroundScope.launch { vm.state.collect {} }   // activate combine + WhileSubscribed
        runCurrent()
        val content = vm.state.value as UiState.Content
        assertEquals(listOf("s2"), content.data.map { it.id })   // s1 already local → excluded
    }

    @Test fun downloadedSongIsExcludedEvenWhenIdsDiffer() = runTest {
        // A song added via Search→Download has id=videoId, while the same file on the Pi has id=filename.
        // The diff must match on filename (the stable identity across both paths), not id — otherwise the
        // downloaded song shows as importable forever and re-importing inserts a duplicate Library row.
        val vm = vm(
            pi = listOf(LibrarySongDto("good.m4a", "Good Luck, Babe!", "Chappell Roan", "Unknown", "good.m4a", "http://pi/downloads/good.m4a", 1L, "d")),
            local = listOf(LibrarySong("vid123", "Good Luck, Babe!", "Chappell Roan", 0, "good.m4a", "/p/good.m4a", 1L, 1L)),
        )
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
        val content = vm.state.value as UiState.Content
        assertEquals(emptyList<String>(), content.data.map { it.id })  // already on device by filename → excluded
    }

    @Test fun toggleAddsAndRemovesSelection() = runTest {
        val vm = vm()
        runCurrent()
        vm.toggle("s2"); assertEquals(setOf("s2"), vm.selected.value)
        vm.toggle("s2"); assertEquals(emptySet<String>(), vm.selected.value)
    }

    @Test fun downloadSelectedDrivesProgressThenClears() = runTest {
        val channel = Channel<DownloadState>(Channel.UNLIMITED)
        val vm = vm(importer = Importer { channel.receiveAsFlow() })
        runCurrent()
        val song = PiSong("s2", "T", "A", "s2.m4a", "http://pi/downloads/s2.m4a", 1L)
        vm.toggle("s2"); runCurrent()
        vm.downloadSelected(listOf(song)); runCurrent()
        assertEquals(emptySet<String>(), vm.selected.value)   // selection consumed on start

        channel.send(DownloadState.InProgress(0.5f)); runCurrent()
        assertEquals(ImportItemState.Downloading(0.5f), vm.downloads.value["s2"])

        val row = LibrarySong("s2", "T", "A", 0, "s2.m4a", "/p/s2.m4a", 1L, 1L)
        channel.send(DownloadState.Completed(row)); runCurrent()
        assertNull(vm.downloads.value["s2"])                  // completed → cleared from progress map
        channel.close()
    }

    @Test fun completedImportLeavesTheImportableList() = runTest {
        // The headline reactive behavior: importing inserts a Room row, the local-library flow updates,
        // and the song drops off `importable`. The fake importer inserts like the real repo does.
        val dao = FakeDao()
        val importer = Importer { song ->
            flow {
                val row = LibrarySong(song.id, song.title, song.artist, 0, song.filename, "/p/${song.filename}", song.size, 1L)
                dao.insert(row)
                emit(DownloadState.Completed(row))
            }
        }
        val vm = ImportViewModel(
            pi = PiLibraryRepository(fakeApi(onLibrary = { LibraryResponseDto(listOf(piDto("s1"), piDto("s2"))) })),
            library = LibraryRepository(dao),
            importer = importer,
        )
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
        assertEquals(listOf("s1", "s2"), (vm.state.value as UiState.Content).data.map { it.id })

        vm.toggle("s1"); runCurrent()
        vm.downloadSelected(listOf(PiSong("s1", "Ts1", "As1", "s1.m4a", "http://pi/downloads/s1.m4a", 1L))); runCurrent()

        assertEquals(listOf("s2"), (vm.state.value as UiState.Content).data.map { it.id })  // s1 imported → gone
    }

    @Test fun deleteFromPiRemovesSongFromImportable() = runTest {
        val deleted = mutableListOf<String>()
        val vm = vm(
            pi = listOf(piDto("s1"), piDto("s2")),
            onDelete = { deleted += it; DeleteResponseDto(true) },
        )
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
        assertEquals(listOf("s1", "s2"), (vm.state.value as UiState.Content).data.map { it.id })

        vm.deleteFromPi(PiSong("s1", "Ts1", "As1", "s1.m4a", "http://pi/downloads/s1.m4a", 1L))
        runCurrent()

        assertEquals(listOf("s1.m4a"), deleted)                                            // Pi delete called w/ filename
        assertEquals(listOf("s2"), (vm.state.value as UiState.Content).data.map { it.id })  // dropped off the list
    }

    @Test fun deleteFromPiKeepsSongAndEmitsErrorOnFailure() = runTest {
        val vm = vm(
            pi = listOf(piDto("s1"), piDto("s2")),
            onDelete = { throw RuntimeException("Pi offline") },
        )
        val errors = mutableListOf<String>()
        backgroundScope.launch { vm.state.collect {} }
        backgroundScope.launch { vm.errors.collect { errors += it } }
        runCurrent()

        vm.deleteFromPi(PiSong("s1", "Ts1", "As1", "s1.m4a", "http://pi/downloads/s1.m4a", 1L))
        runCurrent()

        assertEquals(listOf("s1", "s2"), (vm.state.value as UiState.Content).data.map { it.id })  // list unchanged
        assertEquals(1, errors.size)                                                              // error surfaced
    }
}
