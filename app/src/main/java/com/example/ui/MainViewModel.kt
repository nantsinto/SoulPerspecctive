package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.data.PlaylistEntity
import com.example.data.SimpDatabase
import com.example.model.Artist
import com.example.model.EqualizerPreset
import com.example.model.MusicCatalog
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.model.SuggestionOptionMode
import com.example.model.SuggestionSettings
import com.example.model.SuggestionTrack
import com.example.model.SuggestionsEngine
import com.example.player.RealtimeAudioProcessor
import com.example.player.SimpPlayer
import com.example.player.VisualizerSource
import com.example.player.VisualizerStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    data object Home : Screen()
    data object Search : Screen()
    data object Library : Screen()
    data class ArtistDetail(val artist: Artist) : Screen()
    data class PlaylistDetail(val playlist: PlaylistEntity) : Screen()
    data object FavoritesDetail : Screen()
    data object StudioDashboard : Screen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SimpDatabase.getDatabase(application)
    val repository = MusicRepository(database.musicDao())
    val player = SimpPlayer(application)
    val audioProcessor = RealtimeAudioProcessor(application)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenBackStack = mutableListOf<Screen>(Screen.Home)

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _showEqualizer = MutableStateFlow(false)
    val showEqualizer: StateFlow<Boolean> = _showEqualizer.asStateFlow()

    private val _showSleepTimer = MutableStateFlow(false)
    val showSleepTimer: StateFlow<Boolean> = _showSleepTimer.asStateFlow()

    private val _songToAddToPlaylist = MutableStateFlow<Song?>(null)
    val songToAddToPlaylist: StateFlow<Song?> = _songToAddToPlaylist.asStateFlow()

    private val _suggestionSettings = MutableStateFlow(SuggestionSettings())
    val suggestionSettings: StateFlow<SuggestionSettings> = _suggestionSettings.asStateFlow()

    private val _showSuggestionsOptions = MutableStateFlow(false)
    val showSuggestionsOptions: StateFlow<Boolean> = _showSuggestionsOptions.asStateFlow()

    val currentSongSuggestions: StateFlow<List<SuggestionTrack>> = combine(
        player.currentSong,
        _suggestionSettings
    ) { song, settings ->
        if (song != null) {
            SuggestionsEngine.generateSuggestions(
                seedSong = song,
                mode = settings.mode,
                limit = 15
            )
        } else {
            emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Realtime Frequency Visualizer flows
    val frequencyBands: StateFlow<FloatArray> = audioProcessor.frequencyBands
    val peakBands: StateFlow<FloatArray> = audioProcessor.peakBands
    val waveformData: StateFlow<FloatArray> = audioProcessor.waveformData
    val visualizerStyle: StateFlow<VisualizerStyle> = audioProcessor.currentStyle
    val visualizerSource: StateFlow<VisualizerSource> = audioProcessor.activeSource
    val visualizerSensitivity: StateFlow<Float> = audioProcessor.sensitivity

    val playlists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteSongIds: StateFlow<Set<String>> = repository.favoriteSongIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val historySongs: StateFlow<List<Song>> = repository.historySongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentSong: StateFlow<Song?> = player.currentSong
    val isPlaying: StateFlow<Boolean> = player.isPlaying
    val currentPositionMs: StateFlow<Long> = player.currentPositionMs
    val durationMs: StateFlow<Long> = player.durationMs
    val queue: StateFlow<List<Song>> = player.queue
    val repeatMode: StateFlow<RepeatMode> = player.repeatMode
    val isShuffle: StateFlow<Boolean> = player.isShuffle
    val playbackSpeed: StateFlow<Float> = player.playbackSpeed
    val sleepTimerRemainingSec: StateFlow<Int?> = player.sleepTimerRemainingSec
    val equalizerPreset: StateFlow<EqualizerPreset> = player.equalizerPreset

    init {
        // Auto-play / queue first song so player is ready on launch
        val defaultSong = MusicCatalog.songs.first()
        player.playSong(defaultSong, MusicCatalog.songs)
        player.togglePlayPause() // initial pause state until user taps

        player.setOnSongPlayedListener { song ->
            viewModelScope.launch {
                repository.recordHistory(song.id)
            }
        }

        // Synchronize player state with real-time audio processor
        viewModelScope.launch {
            player.isPlaying.collect { playing ->
                audioProcessor.setPlaybackState(playing, player.audioSessionId.value)
            }
        }
        viewModelScope.launch {
            player.audioSessionId.collect { sessionId ->
                audioProcessor.setPlaybackState(player.isPlaying.value, sessionId)
            }
        }
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        audioProcessor.setStyle(style)
    }

    fun setVisualizerSource(source: VisualizerSource) {
        audioProcessor.switchSource(source)
    }

    fun setVisualizerSensitivity(gain: Float) {
        audioProcessor.setSensitivity(gain)
    }

    fun onAudioPermissionGranted() {
        audioProcessor.onPermissionGranted()
    }

    fun hasAudioPermission(): Boolean {
        return audioProcessor.hasRecordPermission()
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenBackStack.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (_isPlayerExpanded.value) {
            _isPlayerExpanded.value = false
            return true
        }
        if (screenBackStack.size > 1) {
            screenBackStack.removeAt(screenBackStack.size - 1)
            _currentScreen.value = screenBackStack.last()
            return true
        }
        return false
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
    }

    fun playSong(song: Song, queueList: List<Song> = emptyList()) {
        player.playSong(song, queueList)
        viewModelScope.launch {
            repository.recordHistory(song.id)
        }
    }

    fun togglePlayPause() {
        player.togglePlayPause()
    }

    fun playNext() {
        player.playNext()
    }

    fun playPrevious() {
        player.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun toggleFavorite(songId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(songId)
        }
    }

    fun toggleRepeat() {
        player.toggleRepeatMode()
    }

    fun toggleShuffle() {
        player.toggleShuffle()
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    fun openEqualizer() {
        _showEqualizer.value = true
    }

    fun closeEqualizer() {
        _showEqualizer.value = false
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        player.setEqualizerPreset(preset)
    }

    fun openSleepTimer() {
        _showSleepTimer.value = true
    }

    fun closeSleepTimer() {
        _showSleepTimer.value = false
    }

    fun startSleepTimer(minutes: Int) {
        player.startSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        player.cancelSleepTimer()
    }

    fun openAddToPlaylist(song: Song) {
        _songToAddToPlaylist.value = song
    }

    fun closeAddToPlaylist() {
        _songToAddToPlaylist.value = null
    }

    fun addSongToPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun createPlaylistAndAddSong(title: String, songId: String) {
        viewModelScope.launch {
            val plId = repository.createPlaylist(title, "Created playlist")
            repository.addSongToPlaylist(plId, songId)
        }
    }

    fun createPlaylist(title: String, description: String) {
        viewModelScope.launch {
            repository.createPlaylist(title, description)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            navigateBack()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            database.musicDao().clearHistory()
        }
    }

    fun playNextInQueue(song: Song) {
        player.addToQueue(song)
    }

    fun removeFromQueue(index: Int) {
        player.removeFromQueue(index)
    }

    fun openSuggestionsOptions() {
        _showSuggestionsOptions.value = true
    }

    fun closeSuggestionsOptions() {
        _showSuggestionsOptions.value = false
    }

    fun updateSuggestionSettings(settings: SuggestionSettings) {
        _suggestionSettings.value = settings
        player.setAutoplaySuggestions(settings.autoplaySuggestions)
    }

    fun setAutoplaySuggestions(enabled: Boolean) {
        _suggestionSettings.value = _suggestionSettings.value.copy(autoplaySuggestions = enabled)
        player.setAutoplaySuggestions(enabled)
    }

    fun setSuggestionMode(mode: SuggestionOptionMode) {
        _suggestionSettings.value = _suggestionSettings.value.copy(mode = mode)
    }

    fun startRadioForSong(seedSong: Song) {
        player.startRadio(seedSong)
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
        audioProcessor.release()
    }
}
