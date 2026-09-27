package com.example.model

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val album: String,
    val albumId: String,
    val albumArtUrl: String,
    val audioUrl: String,
    val durationMs: Long,
    val genre: String,
    val lyrics: List<LyricLine> = emptyList(),
    val isLiked: Boolean = false,
    val bpm: Int = 120,
    val energy: Float = 0.7f,
    val mood: String = "Energetic",
    val tags: List<String> = emptyList(),
    val year: String = "2024"
) {
    fun formattedDuration(): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}

data class Artist(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val bio: String,
    val monthlyListeners: String
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val year: String,
    val trackIds: List<String>
)

enum class RepeatMode {
    OFF, ALL, ONE
}

data class EqualizerPreset(
    val name: String,
    val bass: Float, // -1.0 to 1.0
    val mid: Float,
    val treble: Float
)
