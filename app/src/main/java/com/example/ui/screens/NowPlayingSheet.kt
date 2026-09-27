package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.LyricLine
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.model.SuggestionOptionMode
import com.example.model.SuggestionSettings
import com.example.model.SuggestionTrack
import com.example.player.VisualizerSource
import com.example.player.VisualizerStyle
import com.example.ui.components.AudioVisualizer
import com.example.ui.components.RealtimeFrequencyCanvasVisualizer
import com.example.ui.components.SuggestionsView
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DividerDark
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    queue: List<Song>,
    isFavorite: Boolean,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    playbackSpeed: Float,
    sleepTimerRemainingSec: Int?,
    frequencyBands: FloatArray,
    peakBands: FloatArray,
    waveformData: FloatArray,
    visualizerStyle: VisualizerStyle,
    visualizerSource: VisualizerSource,
    visualizerSensitivity: Float,
    onStyleChange: (VisualizerStyle) -> Unit,
    onSourceChange: (VisualizerSource) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onRequestPermission: () -> Unit,
    hasPermission: Boolean,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onFavoriteToggle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onChangeSpeed: (Float) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onSelectSongFromQueue: (Song) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onArtistClick: (String) -> Unit,
    suggestions: List<SuggestionTrack> = emptyList(),
    suggestionSettings: SuggestionSettings = SuggestionSettings(),
    favoriteSongIds: Set<String> = emptySet(),
    onOpenSuggestionsOptions: () -> Unit = {},
    onStartRadio: (Song) -> Unit = {},
    onSuggestionModeChange: (SuggestionOptionMode) -> Unit = {},
    onAutoplayToggle: (Boolean) -> Unit = {},
    onAddToPlaylist: (Song) -> Unit = {},
    onPlayNextSong: (Song) -> Unit = {}
) {
    if (song == null) return

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Player, 1: Suggestions, 2: Visualizer, 3: Lyrics, 4: Queue
    val tabs = listOf("Player", "Suggestions", "Visualizer", "Lyrics", "Queue")

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekSliderPosition by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
    val displayFraction = if (isUserSeeking) seekSliderPosition else currentFraction

    val albumScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.94f,
        animationSpec = spring(),
        label = "albumScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF131826),
                        ObsidianBg,
                        ObsidianBg
                    )
                )
            )
            .statusBarsPadding()
            .testTag("now_playing_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_now_playing_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyanPrimary,
                            letterSpacing = 1.2.sp,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Equalizer",
                            tint = CyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(onClick = onOpenSleepTimer) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerRemainingSec != null) CyanAccent else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = CyanPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyanPrimary,
                        height = 3.dp
                    )
                },
                divider = { Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerDark)) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) CyanPrimary else TextSecondary
                                )
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // MAIN PLAYER VIEW
                    var showVisualizerHero by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Hero Art / Visualizer toggle
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            if (showVisualizerHero) {
                                RealtimeFrequencyCanvasVisualizer(
                                    frequencyBands = frequencyBands,
                                    peakBands = peakBands,
                                    waveformData = waveformData,
                                    style = visualizerStyle,
                                    source = visualizerSource,
                                    isPlaying = isPlaying,
                                    sensitivity = visualizerSensitivity,
                                    onStyleChange = onStyleChange,
                                    onSourceChange = onSourceChange,
                                    onSensitivityChange = onSensitivityChange,
                                    onRequestPermission = onRequestPermission,
                                    hasPermission = hasPermission,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .scale(albumScale)
                                        .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = CyanPrimary, spotColor = CyanPrimary)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(SurfaceElevated)
                                        .clickable { showVisualizerHero = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = song.albumArtUrl,
                                        contentDescription = "${song.title} artwork",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Tap to switch to Canvas Visualizer pill
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SurfaceElevated)
                                        .clickable { showVisualizerHero = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AudioVisualizer(
                                        isPlaying = isPlaying,
                                        barCount = 8,
                                        maxHeight = 16.dp,
                                        barWidth = 3.dp,
                                        barSpacing = 3.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Open Real-time Canvas",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyanPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        // Title, Artist, Like
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 22.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = CyanAccent,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { onArtistClick(song.artistId) }
                                )
                            }

                            IconButton(
                                onClick = onFavoriteToggle,
                                modifier = Modifier.size(48.dp).testTag("full_player_favorite_btn")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) CoralAccent else TextSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Seekbar & Timestamps
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = displayFraction.coerceIn(0f, 1f),
                                onValueChange = { fraction ->
                                    isUserSeeking = true
                                    seekSliderPosition = fraction
                                },
                                onValueChangeFinished = {
                                    val targetMs = (seekSliderPosition * durationMs).toLong()
                                    onSeekTo(targetMs)
                                    isUserSeeking = false
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanPrimary,
                                    activeTrackColor = CyanPrimary,
                                    inactiveTrackColor = SurfaceElevated
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("seek_slider")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val currentShownMs = if (isUserSeeking) (seekSliderPosition * durationMs).toLong() else currentPositionMs
                                Text(
                                    text = formatMs(currentShownMs),
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                                )
                                Text(
                                    text = formatMs(durationMs),
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                                )
                            }
                        }

                        // Playback Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shuffle
                            IconButton(onClick = onToggleShuffle) {
                                Icon(
                                    imageVector = Icons.Filled.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffle) CyanPrimary else TextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Previous
                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipPrevious,
                                    contentDescription = "Previous Track",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Big Play / Pause FAB
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(CyanPrimary)
                                    .clickable(onClick = onTogglePlayPause)
                                    .testTag("full_player_play_pause_fab"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = ObsidianBg,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            // Next
                            IconButton(
                                onClick = onNext,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = "Next Track",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Repeat
                            IconButton(onClick = onToggleRepeat) {
                                Icon(
                                    imageVector = when (repeatMode) {
                                        RepeatMode.ONE -> Icons.Filled.RepeatOne
                                        RepeatMode.ALL, RepeatMode.OFF -> Icons.Filled.Repeat
                                    },
                                    contentDescription = "Repeat",
                                    tint = if (repeatMode != RepeatMode.OFF) CyanPrimary else TextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Bottom Extra Bar: Speed selector and Audio Quality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speed Pill
                            val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceElevated)
                                    .clickable {
                                        val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                                        onChangeSpeed(speeds[nextIdx])
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${playbackSpeed}x",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            // Genre / Audio Quality Badge
                            Text(
                                text = "HQ 320kbps • ${song.genre}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                1 -> {
                    // SUGGESTIONS & SMART RADIO TAB
                    SuggestionsView(
                        currentSong = song,
                        suggestions = suggestions,
                        suggestionSettings = suggestionSettings,
                        favoriteSongIds = favoriteSongIds,
                        onSelectSong = onSelectSongFromQueue,
                        onPlayNextSong = onPlayNextSong,
                        onAddToPlaylist = onAddToPlaylist,
                        onFavoriteToggle = { onFavoriteToggle() },
                        onStartRadio = onStartRadio,
                        onSuggestionModeChange = onSuggestionModeChange,
                        onAutoplayToggle = onAutoplayToggle,
                        onOpenSuggestionsOptions = onOpenSuggestionsOptions
                    )
                }

                2 -> {
                    // DEDICATED FULL REAL-TIME CANVAS VISUALIZER STUDIO
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("full_visualizer_tab_view"),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        RealtimeFrequencyCanvasVisualizer(
                            frequencyBands = frequencyBands,
                            peakBands = peakBands,
                            waveformData = waveformData,
                            style = visualizerStyle,
                            source = visualizerSource,
                            isPlaying = isPlaying,
                            sensitivity = visualizerSensitivity,
                            onStyleChange = onStyleChange,
                            onSourceChange = onSourceChange,
                            onSensitivityChange = onSensitivityChange,
                            onRequestPermission = onRequestPermission,
                            hasPermission = hasPermission,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Real-time Frequency Spectrum Band Meter Readouts
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceDark)
                                .border(1.dp, SurfaceElevated, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Acoustic Band Analysis",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val bands = listOf(
                                Triple("Sub-Bass (20-60Hz)", frequencyBands.getOrElse(1) { 0f }, CoralAccent),
                                Triple("Bass (60-250Hz)", frequencyBands.getOrElse(4) { 0f }, CoralAccent),
                                Triple("Low-Mid (250-500Hz)", frequencyBands.getOrElse(8) { 0f }, CyanAccent),
                                Triple("Midrange (500-2kHz)", frequencyBands.getOrElse(14) { 0f }, CyanPrimary),
                                Triple("High-Mid (2-4kHz)", frequencyBands.getOrElse(20) { 0f }, CyanPrimary),
                                Triple("Presence (4-6kHz)", frequencyBands.getOrElse(26) { 0f }, MintSecondary),
                                Triple("Brilliance (6-20kHz)", frequencyBands.getOrElse(32) { 0f }, MintSecondary)
                            )

                            bands.forEach { (label, value, barColor) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.width(135.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SurfaceElevated)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(value.coerceIn(0.02f, 1f))
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(barColor)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${(value * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.width(32.dp)
                                    )
                                }
                            }
                        }

                        // Playback Mini Controls inside Visualizer Studio
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceDark)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(color = CyanAccent),
                                    maxLines = 1
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onPrevious) {
                                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev", tint = TextPrimary)
                                }
                                IconButton(
                                    onClick = onTogglePlayPause,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(CyanPrimary)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = ObsidianBg,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = onNext) {
                                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = TextPrimary)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // SYNCHRONIZED LYRICS VIEW
                    LyricsView(
                        lyrics = song.lyrics,
                        currentPositionMs = currentPositionMs,
                        onSeekTo = onSeekTo
                    )
                }

                4 -> {
                    // UPCOMING QUEUE VIEW
                    QueueView(
                        currentSong = song,
                        queue = queue,
                        onSelectSong = onSelectSongFromQueue,
                        onRemoveFromQueue = onRemoveFromQueue
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit
) {
    if (lyrics.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Lyrics,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No synchronized lyrics available for this track",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val activeIndex = remember(currentPositionMs, lyrics) {
        val idx = lyrics.indexOfLast { it.timeMs <= currentPositionMs }
        if (idx >= 0) idx else 0
    }

    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            listState.animateScrollToItem(maxOf(0, activeIndex - 2))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("lyrics_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(20.dp)) }

        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSeekTo(line.timeMs) }
                    .background(if (isActive) CyanGlow else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = if (isActive) 20.sp else 16.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) CyanPrimary else TextSecondary.copy(alpha = 0.65f),
                        lineHeight = 28.sp
                    )
                )
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
private fun QueueView(
    currentSong: Song,
    queue: List<Song>,
    onSelectSong: (Song) -> Unit,
    onRemoveFromQueue: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().testTag("queue_view")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming Tracks (${queue.size})",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(queue) { index, song ->
                val isCurrent = song.id == currentSong.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCurrent) CyanGlow else SurfaceElevated)
                        .clickable { onSelectSong(song) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.albumArtUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) CyanPrimary else TextPrimary
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            maxLines = 1
                        )
                    }

                    IconButton(onClick = { onRemoveFromQueue(index) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove from Queue",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
