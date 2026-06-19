package com.example.lysnboxandroid

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.text.font.FontFamily
import com.example.lysnboxandroid.ui.theme.ThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Tab { LIBRARY, IMPORT, VOICES }
enum class LibraryFilter { SHELF, FAVORITES, HISTORY }

enum class ReadingFont(val id: String, val displayName: String, val fontFamily: FontFamily) {
    SERIF("serif", "Serif", FontFamily.Serif),
    SANS("sans", "Sans-Serif", FontFamily.SansSerif),
    MONOSPACE("monospace", "Monospace", FontFamily.Monospace);

    companion object {
        fun byId(id: String?): ReadingFont = values().firstOrNull { it.id == id } ?: SERIF
    }
}

class ReaderViewModel(application: Application) : AndroidViewModel(application), PlaybackService.PlaybackListener {
    private val libraryService = LibraryService(application)
    private val prefs = AppPreferences(application)

    // Navigation
    var selectedTab by mutableStateOf(Tab.LIBRARY)
    var isReaderOpen by mutableStateOf(false)
    var isImporting by mutableStateOf(false)
    var importError by mutableStateOf<String?>(null)

    private val _documents = MutableStateFlow<List<SavedDocument>>(emptyList())
    val documents: StateFlow<List<SavedDocument>> = _documents.asStateFlow()

    // Playback state mirrored from the service
    var isPlaying by mutableStateOf(false)
    var activeChapterIndex by mutableStateOf(0)
    var activeParagraphIndex by mutableStateOf(0)
    var activeWordRange by mutableStateOf<IntRange?>(null)
    var activeDocument by mutableStateOf<SavedDocument?>(null)
    var speed by mutableStateOf(prefs.speed)
    var selectedVoice by mutableStateOf<Voice?>(null)
    var availableVoices by mutableStateOf<List<Voice>>(emptyList())
    var isTtsReady by mutableStateOf(false)
    var sleepTimerMinutes by mutableStateOf<Int?>(null)

    // Reading preferences (live)
    var readingPalette by mutableStateOf(ThemePalette.byId(prefs.readingThemeId))
        private set
    var fontSize by mutableStateOf(prefs.fontSize)
        private set
    var hideText by mutableStateOf(prefs.hideText)
        private set
    var readingFont by mutableStateOf(ReadingFont.byId(prefs.fontFamily))
        private set

    private var playbackService: PlaybackService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlaybackService.LocalBinder
            val s = binder.getService()
            playbackService = s
            isBound = true
            s.addListener(this@ReaderViewModel)

            isPlaying = s.isPlaying
            activeChapterIndex = s.currentChapterIndex
            activeParagraphIndex = s.currentParagraphIndex
            activeDocument = s.activeDocument
            s.speed = speed
            availableVoices = s.getAvailableVoices()
            restoreVoiceFromPrefs()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService?.removeListener(this@ReaderViewModel)
            playbackService = null
            isBound = false
        }
    }

    init {
        loadLibrary()
        bindPlaybackService()
    }

    private fun bindPlaybackService() {
        val intent = Intent(getApplication(), PlaybackService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        getApplication<Application>().startService(intent)
    }

    fun loadLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            _documents.value = libraryService.getAllDocuments()
        }
    }

    fun documentsFor(filter: LibraryFilter): List<SavedDocument> {
        val all = _documents.value
        return when (filter) {
            LibraryFilter.SHELF -> all
            LibraryFilter.FAVORITES -> all.filter { it.favorite }
            LibraryFilter.HISTORY -> all.sortedByDescending { it.lastOpenedAt }
        }
    }

    // MARK: - Import

    fun importFile(uri: Uri) = runImport {
        val mime = getApplication<Application>().contentResolver.getType(uri) ?: ""
        val name = uri.toString().lowercase()
        when {
            mime.contains("pdf") || name.endsWith(".pdf") -> PdfImporter.import(getApplication(), uri)
            else -> EpubParser.parse(getApplication(), uri)
        }
    }

    fun importUrl(url: String) = runImport { WebArticleImporter.import(url) }

    fun importText(text: String, title: String?) = runImport { PastedTextImporter.import(text, title) }

    private fun runImport(block: suspend () -> SavedDocument?) {
        isImporting = true
        importError = null
        viewModelScope.launch(Dispatchers.IO) {
            val doc = try {
                block()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            withContext(Dispatchers.Main) {
                isImporting = false
                if (doc != null) {
                    libraryService.saveDocument(doc)
                    loadLibrary()
                    openDocument(doc)
                } else {
                    importError = "Couldn't import that file. It may be empty or unsupported."
                }
            }
        }
    }

    // MARK: - Library actions

    fun openDocument(doc: SavedDocument) {
        val loaded = libraryService.loadDocument(doc.id) ?: doc
        activeDocument = loaded
        playbackService?.loadBook(loaded)
        isReaderOpen = true
    }

    fun closeReader() {
        isReaderOpen = false
    }

    fun deleteDocument(doc: SavedDocument) {
        viewModelScope.launch(Dispatchers.IO) {
            if (activeDocument?.id == doc.id) {
                withContext(Dispatchers.Main) {
                    playbackService?.stopPlayback()
                    activeDocument = null
                    isReaderOpen = false
                }
            }
            libraryService.deleteDocument(doc.id)
            loadLibrary()
        }
    }

    fun toggleFavorite(doc: SavedDocument) {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = libraryService.loadDocument(doc.id) ?: doc
            loaded.favorite = !loaded.favorite
            libraryService.saveDocument(loaded)
            loadLibrary()
        }
    }

    // MARK: - Playback controls

    fun playPause() = playbackService?.playPause() ?: Unit
    fun skipForward() = playbackService?.skipForward() ?: Unit
    fun skipBackward() = playbackService?.skipBackward() ?: Unit

    fun setPlaybackSpeed(newSpeed: Float) {
        speed = newSpeed
        prefs.speed = newSpeed
        playbackService?.speed = newSpeed
    }

    fun setVoice(voice: Voice) {
        selectedVoice = voice
        prefs.voiceName = voice.name
        playbackService?.selectedVoice = voice
    }

    fun previewVoice(voice: Voice) {
        playbackService?.speakSample(
            "This is a preview of the LysnBox narration voice.",
            voice
        )
    }

    fun seekTo(chapterIdx: Int, paragraphIdx: Int) = playbackService?.seekTo(chapterIdx, paragraphIdx) ?: Unit

    fun setSleepTimer(minutes: Int?) = playbackService?.setSleepTimer(minutes) ?: Unit

    private fun restoreVoiceFromPrefs() {
        val savedName = prefs.voiceName
        val voice = availableVoices.firstOrNull { it.name == savedName }
            ?: playbackService?.selectedVoice
            ?: availableVoices.firstOrNull()
        voice?.let {
            selectedVoice = it
            playbackService?.selectedVoice = it
        }
    }

    // MARK: - Reading preferences

    fun setReadingTheme(palette: ThemePalette) {
        readingPalette = palette
        prefs.readingThemeId = palette.id
    }

    fun updateFontSize(size: Float) {
        fontSize = size
        prefs.fontSize = size
    }

    fun updateHideText(hide: Boolean) {
        hideText = hide
        prefs.hideText = hide
    }

    fun updateReadingFont(font: ReadingFont) {
        readingFont = font
        prefs.fontFamily = font.id
    }

    // MARK: - PlaybackListener

    override fun onParagraphChanged(chapterIndex: Int, paragraphIndex: Int) {
        activeChapterIndex = chapterIndex
        activeParagraphIndex = paragraphIndex
        activeWordRange = null
    }

    override fun onPlaybackStateChanged(playing: Boolean) {
        isPlaying = playing
        playbackService?.activeDocument?.let { activeDocument = it }
    }

    override fun onTtsInitialized() {
        isTtsReady = true
        availableVoices = playbackService?.getAvailableVoices() ?: emptyList()
        restoreVoiceFromPrefs()
    }

    override fun onWordRange(start: Int, end: Int) {
        activeWordRange = if (start in 0..end) start until end else null
    }

    override fun onSleepTimerChanged(minutesRemaining: Int?) {
        sleepTimerMinutes = minutesRemaining
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            playbackService?.removeListener(this)
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}
