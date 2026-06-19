package com.example.lysnboxandroid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale

class PlaybackService : Service(), TextToSpeech.OnInitListener {
    private val TAG = "PlaybackService"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "lysnbox_playback_channel"

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    private val binder = LocalBinder()
    
    // TTS Engine
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    
    // Playback State
    var activeDocument: SavedDocument? = null
        private set
    var currentChapterIndex: Int = 0
        private set
    var currentParagraphIndex: Int = 0
        private set
    
    var isPlaying: Boolean = false
        private set
    
    var speed: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
            updatePlaybackState()
        }

    var selectedVoice: Voice? = null
        set(value) {
            field = value
            value?.let { tts?.voice = it }
        }

    // Callbacks to UI
    interface PlaybackListener {
        fun onParagraphChanged(chapterIndex: Int, paragraphIndex: Int)
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onTtsInitialized()
        /** Character range within the current paragraph being spoken (word-level highlight). */
        fun onWordRange(start: Int, end: Int) {}
        fun onSleepTimerChanged(minutesRemaining: Int?) {}
    }
    
    private val listeners = mutableListOf<PlaybackListener>()

    fun addListener(listener: PlaybackListener) {
        listeners.add(listener)
        // Trigger initial state
        listener.onPlaybackStateChanged(isPlaying)
        listener.onParagraphChanged(currentChapterIndex, currentParagraphIndex)
        if (isTtsInitialized) listener.onTtsInitialized()
    }

    fun removeListener(listener: PlaybackListener) {
        listeners.remove(listener)
    }

    // Audio Focus & Media Session
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var mediaSession: MediaSession? = null
    private var libraryService: LibraryService? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        libraryService = LibraryService(getContext())
        
        // Initialize TTS
        tts = TextToSpeech(getContext(), this)
        
        setupMediaSession()
        createNotificationChannel()
    }

    private fun getContext(): Context = this

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_PLAY_PAUSE" -> playPause()
            "ACTION_NEXT" -> skipForward()
            "ACTION_PREVIOUS" -> skipBackward()
            "ACTION_STOP" -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val parts = utteranceId?.split("_") ?: return
                    if (parts.size == 3 && parts[0] == "para") {
                        val cIdx = parts[1].toIntOrNull() ?: 0
                        val pIdx = parts[2].toIntOrNull() ?: 0
                        currentChapterIndex = cIdx
                        currentParagraphIndex = pIdx
                        
                        // Notify listeners
                        mainExecutor.execute {
                            listeners.forEach { it.onParagraphChanged(cIdx, pIdx) }
                            // Save cursor periodically
                            saveCursorPosition()
                        }
                    }
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    if (utteranceId?.startsWith("para_") != true) return
                    mainExecutor.execute {
                        listeners.forEach { it.onWordRange(start, end) }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId?.startsWith("sample_") == true) return
                    mainExecutor.execute {
                        moveToNextParagraph()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS error on utterance: $utteranceId")
                }
            })
            
            // Set default speech rate
            tts?.setSpeechRate(speed)
            
            mainExecutor.execute {
                listeners.forEach { it.onTtsInitialized() }
            }
        } else {
            Log.e(TAG, "Failed to initialize TTS engine")
        }
    }

    fun getAvailableVoices(): List<Voice> {
        if (!isTtsInitialized) return emptyList()
        return try {
            tts?.voices?.toList()?.filter { 
                it.locale.language == Locale.getDefault().language || 
                it.locale.language == "en"
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Speak a short sample with a specific voice without disturbing book playback state. */
    fun speakSample(text: String, voice: Voice?) {
        if (!isTtsInitialized) return
        val previous = tts?.voice
        voice?.let { tts?.voice = it }
        val id = "sample_${System.currentTimeMillis()}"
        val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id) }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, id)
        // Restore the active book voice afterwards is best-effort; selectedVoice setter re-applies on next paragraph.
        if (voice == null && previous != null) tts?.voice = previous
    }

    // Sleep timer
    private val sleepHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var sleepRunnable: Runnable? = null
    var sleepTimerMinutes: Int? = null
        private set

    fun setSleepTimer(minutes: Int?) {
        sleepRunnable?.let { sleepHandler.removeCallbacks(it) }
        sleepTimerMinutes = minutes
        if (minutes == null) {
            listeners.forEach { it.onSleepTimerChanged(null) }
            return
        }
        val r = Runnable {
            if (isPlaying) pauseSpeech()
            sleepTimerMinutes = null
            listeners.forEach { it.onSleepTimerChanged(null) }
        }
        sleepRunnable = r
        sleepHandler.postDelayed(r, minutes * 60_000L)
        listeners.forEach { it.onSleepTimerChanged(minutes) }
    }

    fun loadBook(document: SavedDocument) {
        // If playing another book, stop first
        if (isPlaying) {
            stopSpeech()
        }
        activeDocument = document
        currentChapterIndex = document.cursor.chapterIndex
        currentParagraphIndex = document.cursor.paragraphIndex
        
        // Try to restore custom voice matching standard locale
        val systemVoices = getAvailableVoices()
        if (selectedVoice == null && systemVoices.isNotEmpty()) {
            selectedVoice = tts?.defaultVoice ?: systemVoices.first()
        }
        
        listeners.forEach {
            it.onParagraphChanged(currentChapterIndex, currentParagraphIndex)
        }
        updatePlaybackState()
    }

    fun playPause() {
        if (isPlaying) {
            pauseSpeech()
        } else {
            resumeSpeech()
        }
    }

    private fun resumeSpeech() {
        if (activeDocument == null || !isTtsInitialized) return
        
        if (requestAudioFocus()) {
            isPlaying = true
            startForeground(NOTIFICATION_ID, buildNotification())
            speakCurrentParagraph()
            listeners.forEach { it.onPlaybackStateChanged(true) }
            updatePlaybackState()
        }
    }

    private fun pauseSpeech() {
        isPlaying = false
        stopSpeech()
        listeners.forEach { it.onPlaybackStateChanged(false) }
        updatePlaybackState()
        stopForeground(STOP_FOREGROUND_DETACH)
        // Update notification to normal pause notification
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIFICATION_ID, buildNotification())
        abandonAudioFocus()
    }

    fun stopPlayback() {
        isPlaying = false
        stopSpeech()
        abandonAudioFocus()
        listeners.forEach { it.onPlaybackStateChanged(false) }
        updatePlaybackState()
        stopSelf()
    }

    fun skipForward() {
        val doc = activeDocument ?: return
        val chapter = doc.chapters.getOrNull(currentChapterIndex) ?: return
        
        if (currentParagraphIndex + 1 < chapter.paragraphs.size) {
            currentParagraphIndex++
            if (isPlaying) {
                speakCurrentParagraph()
            } else {
                listeners.forEach { it.onParagraphChanged(currentChapterIndex, currentParagraphIndex) }
                saveCursorPosition()
            }
        } else if (currentChapterIndex + 1 < doc.chapters.size) {
            currentChapterIndex++
            currentParagraphIndex = 0
            if (isPlaying) {
                speakCurrentParagraph()
            } else {
                listeners.forEach { it.onParagraphChanged(currentChapterIndex, currentParagraphIndex) }
                saveCursorPosition()
            }
        }
    }

    fun skipBackward() {
        val doc = activeDocument ?: return
        
        if (currentParagraphIndex > 0) {
            currentParagraphIndex--
            if (isPlaying) {
                speakCurrentParagraph()
            } else {
                listeners.forEach { it.onParagraphChanged(currentChapterIndex, currentParagraphIndex) }
                saveCursorPosition()
            }
        } else if (currentChapterIndex > 0) {
            currentChapterIndex--
            val prevChapter = doc.chapters[currentChapterIndex]
            currentParagraphIndex = (prevChapter.paragraphs.size - 1).coerceAtLeast(0)
            if (isPlaying) {
                speakCurrentParagraph()
            } else {
                listeners.forEach { it.onParagraphChanged(currentChapterIndex, currentParagraphIndex) }
                saveCursorPosition()
            }
        }
    }

    fun seekTo(chapterIdx: Int, paragraphIdx: Int) {
        val doc = activeDocument ?: return
        val chapter = doc.chapters.getOrNull(chapterIdx) ?: return
        val validParaIdx = paragraphIdx.coerceIn(0, (chapter.paragraphs.size - 1).coerceAtLeast(0))
        
        currentChapterIndex = chapterIdx
        currentParagraphIndex = validParaIdx
        
        if (isPlaying) {
            speakCurrentParagraph()
        } else {
            listeners.forEach { it.onParagraphChanged(currentChapterIndex, currentParagraphIndex) }
            saveCursorPosition()
        }
    }

    private fun speakCurrentParagraph() {
        val doc = activeDocument ?: return
        val chapter = doc.chapters.getOrNull(currentChapterIndex) ?: return
        val text = chapter.paragraphs.getOrNull(currentParagraphIndex) ?: return
        
        val utteranceId = "para_${currentChapterIndex}_${currentParagraphIndex}"
        
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun stopSpeech() {
        tts?.stop()
    }

    private fun moveToNextParagraph() {
        if (!isPlaying) return
        val doc = activeDocument ?: return
        val chapter = doc.chapters.getOrNull(currentChapterIndex) ?: return
        
        if (currentParagraphIndex + 1 < chapter.paragraphs.size) {
            currentParagraphIndex++
            speakCurrentParagraph()
        } else if (currentChapterIndex + 1 < doc.chapters.size) {
            currentChapterIndex++
            currentParagraphIndex = 0
            speakCurrentParagraph()
        } else {
            // End of book!
            stopPlayback()
        }
    }

    private fun saveCursorPosition() {
        val doc = activeDocument ?: return
        doc.cursor.chapterIndex = currentChapterIndex
        doc.cursor.paragraphIndex = currentParagraphIndex
        doc.lastOpenedAt = System.currentTimeMillis()
        libraryService?.saveDocument(doc)
    }

    // Audio Focus management
    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            if (isPlaying) pauseSpeech()
                        }
                    }
                }
                .build()
            audioFocusRequest = focusRequest
            return audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            if (isPlaying) pauseSpeech()
                        }
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus { }
        }
    }

    // MediaSession and Notifications
    private fun setupMediaSession() {
        mediaSession = MediaSession(getContext(), "LysnBoxMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    playPause()
                }

                override fun onPause() {
                    playPause()
                }

                override fun onSkipToNext() {
                    skipForward()
                }

                override fun onSkipToPrevious() {
                    skipBackward()
                }

                override fun onStop() {
                    stopPlayback()
                }
            })
            isActive = true
        }
    }

    private fun updatePlaybackState() {
        val stateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_STOP
            )
            .setState(
                if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                speed
            )
        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LysnBox Audiobook Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for LysnBox audiobook playback"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val doc = activeDocument
        val title = doc?.title ?: "No Audiobook Loaded"
        val subtitle = doc?.chapters?.getOrNull(currentChapterIndex)?.title ?: "Chapter $currentChapterIndex"

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pause",
                getServicePendingIntent("ACTION_PLAY_PAUSE")
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                "Play",
                getServicePendingIntent("ACTION_PLAY_PAUSE")
            ).build()
        }

        val nextAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_next,
            "Next",
            getServicePendingIntent("ACTION_NEXT")
        ).build()

        val prevAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_previous,
            "Previous",
            getServicePendingIntent("ACTION_PREVIOUS")
        ).build()

        val openActivityIntent = Intent(getContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            getContext(),
            0,
            openActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(getContext(), CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(android.support.v4.media.session.MediaSessionCompat.Token.fromToken(mediaSession?.sessionToken))
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(isPlaying)
            .build()
    }

    private fun getServicePendingIntent(action: String): PendingIntent {
        val intent = Intent(getContext(), PlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            getContext(),
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        mediaSession?.release()
    }
}
