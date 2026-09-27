package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import com.example.model.EqualizerPreset
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.model.SuggestionsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SimpPlayer(private val context: Context) {

    private val tag = "SimpPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepTimerRemainingSec = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSec: StateFlow<Int?> = _sleepTimerRemainingSec.asStateFlow()

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val _autoplaySuggestions = MutableStateFlow(true)
    val autoplaySuggestions: StateFlow<Boolean> = _autoplaySuggestions.asStateFlow()

    val equalizerPresets = listOf(
        EqualizerPreset("Flat", 0.0f, 0.0f, 0.0f),
        EqualizerPreset("Bass Boost", 0.8f, 0.2f, -0.2f),
        EqualizerPreset("Vocal Clarity", -0.2f, 0.8f, 0.5f),
        EqualizerPreset("Electronic / EDM", 0.9f, 0.1f, 0.7f),
        EqualizerPreset("Chill Acoustic", 0.2f, 0.4f, 0.3f),
        EqualizerPreset("Rock Anthem", 0.6f, 0.3f, 0.8f)
    )

    private val _equalizerPreset = MutableStateFlow(equalizerPresets[0])
    val equalizerPreset: StateFlow<EqualizerPreset> = _equalizerPreset.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var onSongPlayedListener: ((Song) -> Unit)? = null

    init {
        startProgressTracker()
    }

    fun setOnSongPlayedListener(listener: (Song) -> Unit) {
        onSongPlayedListener = listener
    }

    fun playSong(song: Song, newQueue: List<Song> = emptyList()) {
        if (newQueue.isNotEmpty()) {
            _queue.value = newQueue
        } else if (!_queue.value.any { it.id == song.id }) {
            _queue.value = _queue.value + song
        }

        _currentSong.value = song
        _currentPositionMs.value = 0L
        _durationMs.value = song.durationMs
        onSongPlayedListener?.invoke(song)

        startPlayback(song)
    }

    private fun startPlayback(song: Song) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(song.audioUrl)
                setOnPreparedListener { mp ->
                    _durationMs.value = mp.duration.toLong().takeIf { it > 0 } ?: song.durationMs
                    _audioSessionId.value = mp.audioSessionId
                    applyPlaybackSpeed(_playbackSpeed.value)
                    mp.start()
                    _isPlaying.value = true
                }
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(tag, "MediaPlayer error: what=$what, extra=$extra. Fallback to simulated playback.")
                    // Fallback simulation: keep playing with progress so UI still functions
                    _isPlaying.value = true
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(tag, "Failed to start media player", e)
            _isPlaying.value = true
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player != null && (player.isPlaying || _isPlaying.value)) {
            try {
                if (player.isPlaying) {
                    player.pause()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error pausing", e)
            }
            _isPlaying.value = false
        } else {
            if (_currentSong.value == null && _queue.value.isNotEmpty()) {
                playSong(_queue.value.first())
                return
            }
            try {
                player?.start()
            } catch (e: Exception) {
                Log.e(tag, "Error resuming", e)
            }
            _isPlaying.value = true
        }
    }

    fun playNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        val current = _currentSong.value
        val currentIndex = q.indexOfFirst { it.id == current?.id }

        val nextSong = if (_isShuffle.value) {
            val candidates = q.filter { it.id != current?.id }
            if (candidates.isNotEmpty()) candidates.random() else q.first()
        } else {
            if (currentIndex in 0 until q.size - 1) {
                q[currentIndex + 1]
            } else if (_repeatMode.value != RepeatMode.OFF) {
                q.first()
            } else if (_autoplaySuggestions.value && current != null) {
                // Auto-suggest next tracks when queue completes
                val suggestedTracks = SuggestionsEngine.generateRadioStation(current, count = 6)
                    .filter { song -> !q.any { it.id == song.id } }
                if (suggestedTracks.isNotEmpty()) {
                    _queue.value = _queue.value + suggestedTracks
                    suggestedTracks.first()
                } else {
                    null
                }
            } else {
                null
            }
        }

        if (nextSong != null) {
            playSong(nextSong)
        } else {
            _isPlaying.value = false
            _currentPositionMs.value = 0L
        }
    }

    fun setAutoplaySuggestions(enabled: Boolean) {
        _autoplaySuggestions.value = enabled
    }

    fun startRadio(seedSong: Song) {
        val radioStation = SuggestionsEngine.generateRadioStation(seedSong, count = 12)
        playSong(seedSong, radioStation)
    }

    fun playPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return

        // If played more than 3 seconds, replay current song
        if (_currentPositionMs.value > 3000L) {
            seekTo(0L)
            return
        }

        val current = _currentSong.value
        val currentIndex = q.indexOfFirst { it.id == current?.id }
        val prevSong = if (currentIndex > 0) {
            q[currentIndex - 1]
        } else {
            q.lastOrNull()
        }

        if (prevSong != null) {
            playSong(prevSong)
        }
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
        } catch (e: Exception) {
            Log.e(tag, "Error seeking", e)
        }
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applyPlaybackSpeed(speed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        mp.playbackParams = mp.playbackParams.setSpeed(speed)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to apply playback speed", e)
            }
        }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _equalizerPreset.value = preset
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        var remainingSec = minutes * 60
        _sleepTimerRemainingSec.value = remainingSec

        sleepTimerJob = scope.launch {
            while (remainingSec > 0 && isActive) {
                delay(1000L)
                remainingSec--
                _sleepTimerRemainingSec.value = remainingSec
            }
            // Timer expired -> pause playback
            if (_isPlaying.value) {
                togglePlayPause()
            }
            _sleepTimerRemainingSec.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSec.value = null
    }

    fun addToQueue(song: Song) {
        _queue.value = _queue.value + song
    }

    fun removeFromQueue(index: Int) {
        val list = _queue.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _queue.value = list
        }
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0L)
                mediaPlayer?.start()
                _isPlaying.value = true
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                playNext()
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(200L)
                if (_isPlaying.value) {
                    val player = mediaPlayer
                    if (player != null && player.isPlaying) {
                        try {
                            _currentPositionMs.value = player.currentPosition.toLong()
                            if (player.duration > 0) {
                                _durationMs.value = player.duration.toLong()
                            }
                        } catch (e: Exception) {
                            // ignore transient states
                        }
                    } else {
                        // Simulated progress fallback
                        val dur = _durationMs.value
                        val newPos = _currentPositionMs.value + (200L * _playbackSpeed.value).toLong()
                        if (dur > 0 && newPos >= dur) {
                            handleTrackCompletion()
                        } else {
                            _currentPositionMs.value = newPos
                        }
                    }
                }
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
