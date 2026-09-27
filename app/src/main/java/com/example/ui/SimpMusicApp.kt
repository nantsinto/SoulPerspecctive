package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.MusicCatalog
import com.example.model.Song
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.AddToPlaylistDialog
import com.example.ui.screens.ArtistDetailScreen
import com.example.ui.screens.AudioStudioDashboardScreen
import com.example.ui.screens.EqualizerDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingSheet
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SleepTimerDialog
import com.example.ui.screens.SuggestionsOptionsDialog
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SimpMusicApp(
    viewModel: MainViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsState()
    val showEqualizer by viewModel.showEqualizer.collectAsState()
    val showSleepTimer by viewModel.showSleepTimer.collectAsState()
    val songToAddToPlaylist by viewModel.songToAddToPlaylist.collectAsState()
    val suggestionSettings by viewModel.suggestionSettings.collectAsState()
    val showSuggestionsOptions by viewModel.showSuggestionsOptions.collectAsState()
    val currentSongSuggestions by viewModel.currentSongSuggestions.collectAsState()

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val sleepTimerRemainingSec by viewModel.sleepTimerRemainingSec.collectAsState()
    val equalizerPreset by viewModel.equalizerPreset.collectAsState()

    val playlists by viewModel.playlists.collectAsState()
    val favoriteSongIds by viewModel.favoriteSongIds.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val historySongs by viewModel.historySongs.collectAsState()

    // Real-time frequency visualizer state
    val frequencyBands by viewModel.frequencyBands.collectAsState()
    val peakBands by viewModel.peakBands.collectAsState()
    val waveformData by viewModel.waveformData.collectAsState()
    val visualizerStyle by viewModel.visualizerStyle.collectAsState()
    val visualizerSource by viewModel.visualizerSource.collectAsState()
    val visualizerSensitivity by viewModel.visualizerSensitivity.collectAsState()

    val context = LocalContext.current
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordAudioPermission = isGranted
        if (isGranted) {
            viewModel.onAudioPermissionGranted()
        }
    }

    BackHandler(enabled = true) {
        if (!viewModel.navigateBack()) {
            // Let activity handle finish
        }
    }

    val progressFraction = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
    val isCurrentFavorite = currentSong?.let { it.id in favoriteSongIds } ?: false

    Box(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        Scaffold(
            bottomBar = {
                // Bottom Bar showing Navigation Bar and Mini Player
                Column(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Mini Player
                    if (currentSong != null && !isPlayerExpanded) {
                        MiniPlayer(
                            song = currentSong,
                            isPlaying = isPlaying,
                            progressFraction = progressFraction,
                            isFavorite = isCurrentFavorite,
                            onExpand = { viewModel.setPlayerExpanded(true) },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.playNext() },
                            onFavoriteToggle = { currentSong?.let { viewModel.toggleFavorite(it.id) } }
                        )
                    }

                    // Navigation Bar (only on root tabs)
                    if (currentScreen is Screen.Home || currentScreen is Screen.Search || currentScreen is Screen.Library) {
                        NavigationBar(
                            containerColor = SurfaceDark,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("main_bottom_nav")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen is Screen.Home,
                                onClick = { viewModel.navigateTo(Screen.Home) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen is Screen.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = "Home"
                                    )
                                },
                                label = {
                                    Text(
                                        "Home",
                                        fontWeight = if (currentScreen is Screen.Home) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPrimary,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_home_tab")
                            )

                            NavigationBarItem(
                                selected = currentScreen is Screen.Search,
                                onClick = { viewModel.navigateTo(Screen.Search) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen is Screen.Search) Icons.Filled.Search else Icons.Outlined.Search,
                                        contentDescription = "Explore"
                                    )
                                },
                                label = {
                                    Text(
                                        "Explore",
                                        fontWeight = if (currentScreen is Screen.Search) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPrimary,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_search_tab")
                            )

                            NavigationBarItem(
                                selected = currentScreen is Screen.Library,
                                onClick = { viewModel.navigateTo(Screen.Library) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen is Screen.Library) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                                        contentDescription = "Library"
                                    )
                                },
                                label = {
                                    Text(
                                        "Library",
                                        fontWeight = if (currentScreen is Screen.Library) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPrimary,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_library_tab")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val screen = currentScreen) {
                    is Screen.Home -> {
                        HomeScreen(
                            currentSongId = currentSong?.id,
                            isPlaying = isPlaying,
                            favoriteSongIds = favoriteSongIds,
                            playlists = playlists,
                            onSongClick = { song, list -> viewModel.playSong(song, list) },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            onArtistClick = { viewModel.navigateTo(Screen.ArtistDetail(it)) },
                            onPlaylistClick = { viewModel.navigateTo(Screen.PlaylistDetail(it)) },
                            onPlayPlaylist = { pl ->
                                val songs = MusicCatalog.songs.filter { it.album == pl.title || true }.take(5)
                                if (songs.isNotEmpty()) viewModel.playSong(songs.first(), songs)
                            },
                            onOpenEqualizer = { viewModel.openEqualizer() },
                            onOpenSleepTimer = { viewModel.openSleepTimer() },
                            onAddToPlaylist = { viewModel.openAddToPlaylist(it) },
                            onPlayNext = { viewModel.playNextInQueue(it) },
                            onOpenStudioDashboard = { viewModel.navigateTo(Screen.StudioDashboard) },
                            onOpenSuggestionsOptions = { viewModel.openSuggestionsOptions() },
                            onStartRadio = { viewModel.startRadioForSong(it) }
                        )
                    }

                    is Screen.Search -> {
                        SearchScreen(
                            currentSongId = currentSong?.id,
                            isPlaying = isPlaying,
                            favoriteSongIds = favoriteSongIds,
                            onSongClick = { song, list -> viewModel.playSong(song, list) },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            onArtistClick = { viewModel.navigateTo(Screen.ArtistDetail(it)) },
                            onAddToPlaylist = { viewModel.openAddToPlaylist(it) },
                            onPlayNext = { viewModel.playNextInQueue(it) }
                        )
                    }

                    is Screen.Library -> {
                        LibraryScreen(
                            favoriteSongs = favoriteSongs,
                            playlists = playlists,
                            historySongs = historySongs,
                            currentSongId = currentSong?.id,
                            isPlaying = isPlaying,
                            favoriteSongIds = favoriteSongIds,
                            onSongClick = { song, list -> viewModel.playSong(song, list) },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            onPlaylistClick = { viewModel.navigateTo(Screen.PlaylistDetail(it)) },
                            onOpenFavorites = { viewModel.navigateTo(Screen.FavoritesDetail) },
                            onCreatePlaylist = { title, desc -> viewModel.createPlaylist(title, desc) },
                            onClearHistory = { viewModel.clearHistory() },
                            onAddToPlaylist = { viewModel.openAddToPlaylist(it) },
                            onPlayNext = { viewModel.playNextInQueue(it) },
                            onOpenStudioDashboard = { viewModel.navigateTo(Screen.StudioDashboard) }
                        )
                    }

                    is Screen.ArtistDetail -> {
                        val artistSongs = MusicCatalog.getSongsByArtist(screen.artist.id)
                        ArtistDetailScreen(
                            artist = screen.artist,
                            songs = artistSongs,
                            currentSongId = currentSong?.id,
                            isPlaying = isPlaying,
                            favoriteSongIds = favoriteSongIds,
                            onBack = { viewModel.navigateBack() },
                            onSongClick = { viewModel.playSong(it, artistSongs) },
                            onPlayAll = { if (it.isNotEmpty()) viewModel.playSong(it.first(), it) },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { viewModel.openAddToPlaylist(it) },
                            onPlayNext = { viewModel.playNextInQueue(it) }
                        )
                    }

                    is Screen.PlaylistDetail -> {
                        // Gather songs for playlist
                        val playlistSongs by viewModel.repository.getSongsForPlaylist(screen.playlist.id).collectAsState(initial = emptyList())
                        val displaySongs = if (playlistSongs.isNotEmpty()) playlistSongs else MusicCatalog.songs.take(3)
                        PlaylistDetailScreen(
                            title = screen.playlist.title,
                            description = screen.playlist.description,
                            coverUrl = screen.playlist.coverUrl,
                            songs = displaySongs,
                            currentSongId = currentSong?.id,
                            isPlaying = isPlaying,
                            favoriteSongIds = favoriteSongIds,
                            canDelete = true,
                            onBack = { viewModel.navigateBack() },
                            onPlayAll = { if (it.isNotEmpty()) viewModel.playSong(it.first(), it) },
                            onShufflePlay = { if (it.isNotEmpty()) { viewModel.toggleShuffle(); viewModel.playSong(it.random(), it) } },
                            onSongClick = { viewModel.playSong(it, displaySongs) },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            onDeletePlaylist = { viewModel.deletePlaylist(screen.playlist.id) },
                            onAddToPlaylist = { viewModel.openAddToPlaylist(it) },
                            onPlayNext = { viewModel.playNextInQueue(it) }
                        )
                    }

                    is Screen.FavoritesDetail -> {
                        PlaylistDetailScreen(
                            title = "Liked Songs",
                            description = "Your favorite saved tracks across all genres.",
                            coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
                            songs = favoriteSongs,
                            currentSongId = currentSong?.id,
                            isPlaying = isPlaying,
                            favoriteSongIds = favoriteSongIds,
                            canDelete = false,
                            onBack = { viewModel.navigateBack() },
                            onPlayAll = { if (it.isNotEmpty()) viewModel.playSong(it.first(), it) },
                            onShufflePlay = { if (it.isNotEmpty()) { viewModel.toggleShuffle(); viewModel.playSong(it.random(), it) } },
                            onSongClick = { viewModel.playSong(it, favoriteSongs) },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { viewModel.openAddToPlaylist(it) },
                            onPlayNext = { viewModel.playNextInQueue(it) }
                        )
                    }

                    is Screen.StudioDashboard -> {
                        AudioStudioDashboardScreen(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            suggestionSettings = suggestionSettings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSuggestionsOptions = { viewModel.openSuggestionsOptions() },
                            onStartRadio = { viewModel.startRadioForSong(it) }
                        )
                    }
                }
            }
        }

        // Full Screen Player Sheet
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            NowPlayingSheet(
                song = currentSong,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                queue = queue,
                isFavorite = isCurrentFavorite,
                repeatMode = repeatMode,
                isShuffle = isShuffle,
                playbackSpeed = playbackSpeed,
                sleepTimerRemainingSec = sleepTimerRemainingSec,
                frequencyBands = frequencyBands,
                peakBands = peakBands,
                waveformData = waveformData,
                visualizerStyle = visualizerStyle,
                visualizerSource = visualizerSource,
                visualizerSensitivity = visualizerSensitivity,
                onStyleChange = { viewModel.setVisualizerStyle(it) },
                onSourceChange = { viewModel.setVisualizerSource(it) },
                onSensitivityChange = { viewModel.setVisualizerSensitivity(it) },
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                hasPermission = hasRecordAudioPermission,
                onClose = { viewModel.setPlayerExpanded(false) },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onSeekTo = { viewModel.seekTo(it) },
                onFavoriteToggle = { currentSong?.let { viewModel.toggleFavorite(it.id) } },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onChangeSpeed = { viewModel.setSpeed(it) },
                onOpenEqualizer = { viewModel.openEqualizer() },
                onOpenSleepTimer = { viewModel.openSleepTimer() },
                onSelectSongFromQueue = { viewModel.playSong(it, queue) },
                onRemoveFromQueue = { viewModel.removeFromQueue(it) },
                onArtistClick = { artistId ->
                    val artist = MusicCatalog.artists.find { it.id == artistId }
                    if (artist != null) {
                        viewModel.setPlayerExpanded(false)
                        viewModel.navigateTo(Screen.ArtistDetail(artist))
                    }
                },
                suggestions = currentSongSuggestions,
                suggestionSettings = suggestionSettings,
                favoriteSongIds = favoriteSongIds,
                onOpenSuggestionsOptions = { viewModel.openSuggestionsOptions() },
                onStartRadio = { viewModel.startRadioForSong(it) },
                onSuggestionModeChange = { viewModel.setSuggestionMode(it) },
                onAutoplayToggle = { viewModel.setAutoplaySuggestions(it) },
                onAddToPlaylist = { viewModel.openAddToPlaylist(it) },
                onPlayNextSong = { viewModel.playNextInQueue(it) }
            )
        }

        // Equalizer Dialog
        if (showEqualizer) {
            EqualizerDialog(
                currentPreset = equalizerPreset,
                presets = viewModel.player.equalizerPresets,
                onSelectPreset = { viewModel.setEqualizerPreset(it) },
                onDismiss = { viewModel.closeEqualizer() }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimer) {
            SleepTimerDialog(
                remainingSeconds = sleepTimerRemainingSec,
                onStartTimer = { viewModel.startSleepTimer(it) },
                onCancelTimer = { viewModel.cancelSleepTimer() },
                onDismiss = { viewModel.closeSleepTimer() }
            )
        }

        // Add to Playlist Dialog
        songToAddToPlaylist?.let { song ->
            AddToPlaylistDialog(
                song = song,
                playlists = playlists,
                onAddToPlaylist = { plId -> viewModel.addSongToPlaylist(plId, song.id) },
                onCreateAndAdd = { title -> viewModel.createPlaylistAndAddSong(title, song.id) },
                onDismiss = { viewModel.closeAddToPlaylist() }
            )
        }

        // Suggestions Options Dialog
        if (showSuggestionsOptions) {
            SuggestionsOptionsDialog(
                currentSettings = suggestionSettings,
                currentSong = currentSong,
                onUpdateSettings = { viewModel.updateSuggestionSettings(it) },
                onStartRadio = { viewModel.startRadioForSong(it) },
                onDismiss = { viewModel.closeSuggestionsOptions() }
            )
        }
    }
}
