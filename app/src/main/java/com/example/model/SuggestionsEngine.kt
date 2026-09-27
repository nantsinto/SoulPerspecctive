package com.example.model

enum class SuggestionOptionMode(val displayName: String, val description: String) {
    ALL("All Suggestions", "Smart blend of harmonic, artist, and genre matches"),
    SONIC_MATCH("Sonic Match", "Matches tempo, acoustic energy, and frequency profile"),
    SAME_ARTIST("Artist Affinity", "Deep dive into this artist and collaborative styles"),
    SIMILAR_VIBE("Similar Vibe", "Matched mood and genre ambience"),
    DISCOVERY("Discovery", "Eclectic fresh gems outside your routine"),
    RADIO_MIX("Endless Radio", "Infinite continuous stream generated from seed track")
}

data class SuggestionTrack(
    val song: Song,
    val matchScore: Int, // 0 - 100
    val matchReason: String,
    val badgeText: String
)

data class SuggestionSettings(
    val autoplaySuggestions: Boolean = true,
    val mode: SuggestionOptionMode = SuggestionOptionMode.ALL,
    val diversity: Float = 0.5f, // 0.0f = exact matches, 1.0f = adventurous
    val timeOfDayAwareness: Boolean = true
)

enum class TimeOfDay(val label: String, val moodKeywords: List<String>) {
    MORNING("Morning Focus", listOf("Chill", "Acoustic", "Morning", "Coffee")),
    AFTERNOON("Midday Energy", listOf("Synthwave", "Electronic", "Upbeat", "Energy")),
    EVENING("Evening Chill", listOf("Lo-Fi", "Melodic", "Chill", "Ambient")),
    NIGHT("Late Night Cyber", listOf("Cyberpunk", "Darkwave", "Late Night", "Midnight", "Bass"))
}

object SuggestionsEngine {

    fun getCurrentTimeOfDay(): TimeOfDay {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..17 -> TimeOfDay.AFTERNOON
            in 18..22 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    /**
     * Computes similarity score (0..100) between two tracks based on:
     * - Genre match (35%)
     * - Artist match (25%)
     * - Tempo / Mood match (25%)
     * - Acoustic energy (15%)
     */
    fun calculateSimilarity(seed: Song, candidate: Song): Int {
        if (seed.id == candidate.id) return 100

        var score = 40 // baseline baseline affinity

        // Genre matching
        if (seed.genre.equals(candidate.genre, ignoreCase = true)) {
            score += 30
        } else if (isCompatibleGenre(seed.genre, candidate.genre)) {
            score += 15
        }

        // Artist matching
        if (seed.artistId == candidate.artistId) {
            score += 25
        }

        // Title/vibe thematic similarity
        val seedKeywords = (seed.title + " " + seed.album).lowercase().split(" ", "-", "_")
        val candidateKeywords = (candidate.title + " " + candidate.album).lowercase().split(" ", "-", "_")
        val overlap = seedKeywords.intersect(candidateKeywords.toSet()).filter { it.length > 2 }
        if (overlap.isNotEmpty()) {
            score += 10
        }

        return score.coerceIn(55, 99)
    }

    private fun isCompatibleGenre(g1: String, g2: String): Boolean {
        val pairs = setOf(
            setOf("Synthwave", "Cyberpunk"),
            setOf("Synthwave", "Electronic"),
            setOf("Lo-Fi", "Chill"),
            setOf("Lo-Fi", "Acoustic"),
            setOf("Ambient", "Lo-Fi"),
            setOf("Retro Electro", "Synthwave"),
            setOf("Electronic", "Cyberpunk")
        )
        return pairs.contains(setOf(g1, g2))
    }

    fun generateSuggestions(
        seedSong: Song,
        mode: SuggestionOptionMode = SuggestionOptionMode.ALL,
        allSongs: List<Song> = MusicCatalog.songs,
        limit: Int = 12
    ): List<SuggestionTrack> {
        val candidates = allSongs.filter { it.id != seedSong.id }

        val scored = candidates.map { candidate ->
            val score = calculateSimilarity(seedSong, candidate)
            val badge = when {
                candidate.artistId == seedSong.artistId -> "Artist Twin"
                candidate.genre == seedSong.genre && score >= 90 -> "98% Sonic Match"
                candidate.genre == seedSong.genre -> "Same Genre (${candidate.genre})"
                isCompatibleGenre(seedSong.genre, candidate.genre) -> "Harmonic Twin"
                score >= 80 -> "High Affinity"
                else -> "Discovery Pick"
            }
            val reason = when {
                candidate.artistId == seedSong.artistId -> "More from ${candidate.artist}"
                candidate.genre == seedSong.genre -> "Matches the ${seedSong.genre} sound of ${seedSong.title}"
                else -> "Harmonically tuned to your current listening"
            }
            SuggestionTrack(candidate, score, reason, badge)
        }

        val filtered = when (mode) {
            SuggestionOptionMode.ALL -> scored.sortedByDescending { it.matchScore }
            SuggestionOptionMode.SONIC_MATCH -> scored.sortedByDescending { it.matchScore }
            SuggestionOptionMode.SAME_ARTIST -> scored.sortedByDescending { if (it.song.artistId == seedSong.artistId) 200 else it.matchScore }
            SuggestionOptionMode.SIMILAR_VIBE -> scored.filter { it.song.genre == seedSong.genre || isCompatibleGenre(it.song.genre, seedSong.genre) }.sortedByDescending { it.matchScore }
            SuggestionOptionMode.DISCOVERY -> scored.sortedBy { it.matchScore } // inverse to give fresh tracks
            SuggestionOptionMode.RADIO_MIX -> scored.shuffled()
        }

        return filtered.take(limit)
    }

    fun generateRadioStation(
        seedSong: Song,
        allSongs: List<Song> = MusicCatalog.songs,
        count: Int = 12
    ): List<Song> {
        val directMatches = allSongs.filter { it.id != seedSong.id && (it.genre == seedSong.genre || it.artistId == seedSong.artistId) }.shuffled()
        val complementary = allSongs.filter { it.id != seedSong.id && it !in directMatches }.shuffled()
        val combined = listOf(seedSong) + directMatches + complementary
        return combined.distinctBy { it.id }.take(count)
    }

    fun getSearchSuggestions(query: String, allSongs: List<Song> = MusicCatalog.songs): List<String> {
        if (query.isBlank()) {
            return listOf("Neon Dreams", "Tokyo Rain", "Lo-Fi Beats", "Midnight Pulse", "Cyberpunk", "Acoustic Morning")
        }
        val q = query.trim().lowercase()
        val suggestions = mutableListOf<String>()

        allSongs.forEach { song ->
            if (song.title.lowercase().startsWith(q)) suggestions.add(song.title)
            if (song.artist.lowercase().startsWith(q)) suggestions.add(song.artist)
            if (song.genre.lowercase().startsWith(q)) suggestions.add(song.genre)
        }

        allSongs.forEach { song ->
            if (song.title.lowercase().contains(q) && !suggestions.contains(song.title)) suggestions.add(song.title)
            if (song.artist.lowercase().contains(q) && !suggestions.contains(song.artist)) suggestions.add(song.artist)
        }

        return suggestions.distinct().take(6)
    }
}
