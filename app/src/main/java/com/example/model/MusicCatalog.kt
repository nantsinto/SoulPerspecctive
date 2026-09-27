package com.example.model

object MusicCatalog {

    val artists = listOf(
        Artist(
            id = "art_1",
            name = "Neon Horizon",
            avatarUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop&q=80",
            bio = "Synthwave and retrowave producer blending analog synthesizers with modern cyberpunk beats.",
            monthlyListeners = "1,842,500"
        ),
        Artist(
            id = "art_2",
            name = "Kira Moon",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80",
            bio = "Ethereal dream-pop vocalist and producer creating intimate midnight soundscapes.",
            monthlyListeners = "2,310,000"
        ),
        Artist(
            id = "art_3",
            name = "Tokyo Rain",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&auto=format&fit=crop&q=80",
            bio = "Lo-fi hip-hop curator making cozy study beats, rain aesthetics, and warm Rhodes chords.",
            monthlyListeners = "3,120,400"
        ),
        Artist(
            id = "art_4",
            name = "Aero Pulse",
            avatarUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=500&auto=format&fit=crop&q=80",
            bio = "Electronic dance and future bass artist known for high-octane festival anthems.",
            monthlyListeners = "985,000"
        ),
        Artist(
            id = "art_5",
            name = "Maya Lin",
            avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=500&auto=format&fit=crop&q=80",
            bio = "Acoustic indie folk songwriter with soul-stirring fingerpicking and poetic lyrics.",
            monthlyListeners = "1,450,000"
        )
    )

    val albums = listOf(
        Album(
            id = "alb_1",
            title = "Midnight Odyssey",
            artist = "Neon Horizon",
            coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
            year = "2024",
            trackIds = listOf("song_1", "song_2", "song_3")
        ),
        Album(
            id = "alb_2",
            title = "Starlight Confessions",
            artist = "Kira Moon",
            coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            year = "2024",
            trackIds = listOf("song_4", "song_5")
        ),
        Album(
            id = "alb_3",
            title = "Coffee & Raindrops",
            artist = "Tokyo Rain",
            coverUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80",
            year = "2023",
            trackIds = listOf("song_6", "song_7")
        ),
        Album(
            id = "alb_4",
            title = "Hyperdrive Velocity",
            artist = "Aero Pulse",
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            year = "2024",
            trackIds = listOf("song_8", "song_9")
        )
    )

    val songs = listOf(
        Song(
            id = "song_1",
            title = "Neon Dreams",
            artist = "Neon Horizon",
            artistId = "art_1",
            album = "Midnight Odyssey",
            albumId = "alb_1",
            albumArtUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3",
            durationMs = 175000L,
            genre = "Synthwave",
            lyrics = listOf(
                LyricLine(0L, "[Instrumental Intro - Analog Synths]"),
                LyricLine(12000L, "City lights reflect across the glass"),
                LyricLine(24000L, "Speeding through the night, shadows moving fast"),
                LyricLine(36000L, "Electric signals guiding every turn"),
                LyricLine(48000L, "In the neon glow, the memories burn"),
                LyricLine(60000L, "Neon dreams, carry me away tonight"),
                LyricLine(74000L, "Through the cyber grid and blinding light"),
                LyricLine(88000L, "Hold the wire, never letting go"),
                LyricLine(102000L, "Lost inside the retro wave below"),
                LyricLine(120000L, "[Synth Solo & Bass Drop]"),
                LyricLine(145000L, "Neon dreams, we are alive tonight"),
                LyricLine(160000L, "Fading softly into morning light")
            )
        ),
        Song(
            id = "song_2",
            title = "Cyber City Highway",
            artist = "Neon Horizon",
            artistId = "art_1",
            album = "Midnight Odyssey",
            albumId = "alb_1",
            albumArtUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f77e68.mp3",
            durationMs = 188000L,
            genre = "Synthwave",
            lyrics = listOf(
                LyricLine(0L, "[Pulsing Arpeggiator Beats]"),
                LyricLine(14000L, "Zero hour on the elevated lane"),
                LyricLine(28000L, "Wipers sweeping through holographic rain"),
                LyricLine(42000L, "Miles and miles of fiber optic glow"),
                LyricLine(56000L, "Rushing forward where the currents flow"),
                LyricLine(72000L, "Push the pedal, feel the engines roar"),
                LyricLine(90000L, "Heading somewhere we have never been before"),
                LyricLine(110000L, "[Guitar Synthesizer Lead]"),
                LyricLine(140000L, "Through the highway into endless skies"),
                LyricLine(165000L, "Reflections gleaming in your eyes")
            )
        ),
        Song(
            id = "song_3",
            title = "Analog Echoes",
            artist = "Neon Horizon",
            artistId = "art_1",
            album = "Midnight Odyssey",
            albumId = "alb_1",
            albumArtUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3",
            durationMs = 162000L,
            genre = "Chillwave",
            lyrics = listOf(
                LyricLine(0L, "[Warm Tape Saturation Intro]"),
                LyricLine(15000L, "Rewind the spool, let the tape unwind"),
                LyricLine(32000L, "Echoes of a summer left behind"),
                LyricLine(50000L, "Vintage reverbs dancing on the wall"),
                LyricLine(68000L, "Waiting for the evening breeze to fall"),
                LyricLine(90000L, "Analog heart, digital soul"),
                LyricLine(110000L, "Broken frequencies make us whole"),
                LyricLine(135000L, "[Ambient Synth Outro]")
            )
        ),
        Song(
            id = "song_4",
            title = "Starlight Confession",
            artist = "Kira Moon",
            artistId = "art_2",
            album = "Starlight Confessions",
            albumId = "alb_2",
            albumArtUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3",
            durationMs = 195000L,
            genre = "Dream Pop",
            lyrics = listOf(
                LyricLine(0L, "[Ambient piano and soft strings]"),
                LyricLine(16000L, "Looking up beneath a velvet sky"),
                LyricLine(30000L, "Counting all the wishes drifting by"),
                LyricLine(46000L, "If I told you what was in my mind"),
                LyricLine(62000L, "Would it be a secret we could find?"),
                LyricLine(80000L, "Under the starlight, nothing feels the same"),
                LyricLine(96000L, "Every whisper gently calls your name"),
                LyricLine(114000L, "Hold my hand before the dawn appears"),
                LyricLine(132000L, "Wash away the shadows and the fears"),
                LyricLine(155000L, "Stars will keep our promises intact"),
                LyricLine(175000L, "[Harmonic vocal fade]")
            )
        ),
        Song(
            id = "song_5",
            title = "Moonlit Whispers",
            artist = "Kira Moon",
            artistId = "art_2",
            album = "Starlight Confessions",
            albumId = "alb_2",
            albumArtUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2021/08/09/audio_2289c25cf6.mp3",
            durationMs = 168000L,
            genre = "Chillout",
            lyrics = listOf(
                LyricLine(0L, "[Gentle acoustic pads]"),
                LyricLine(14000L, "Silent waters shimmer in the breeze"),
                LyricLine(30000L, "Rustling leaves upon the willow trees"),
                LyricLine(48000L, "Time stands still when nobody is near"),
                LyricLine(65000L, "Only peaceful melodies I hear"),
                LyricLine(85000L, "Moonlit whispers, take away the pain"),
                LyricLine(105000L, "Dancing softly in the silver rain"),
                LyricLine(130000L, "Rest your head and let tomorrow wait"),
                LyricLine(150000L, "[Gentle fade out]")
            )
        ),
        Song(
            id = "song_6",
            title = "Rainy Afternoon in Shibuya",
            artist = "Tokyo Rain",
            artistId = "art_3",
            album = "Coffee & Raindrops",
            albumId = "alb_3",
            albumArtUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/05/16/audio_db6591201e.mp3",
            durationMs = 145000L,
            genre = "Lo-Fi",
            lyrics = listOf(
                LyricLine(0L, "[Vinyl crackle & rain sounds]"),
                LyricLine(10000L, "Steam rises from the porcelain mug"),
                LyricLine(24000L, "Wrapped in thoughts and an oversized rug"),
                LyricLine(38000L, "People walking by with umbrellas bright"),
                LyricLine(52000L, "Safe indoors in the cozy amber light"),
                LyricLine(70000L, "[Rhodes electric piano solo]"),
                LyricLine(95000L, "Notes that linger on the window pane"),
                LyricLine(115000L, "Just another chapter in the rain"),
                LyricLine(132000L, "[Rain sound fades to silence]")
            )
        ),
        Song(
            id = "song_7",
            title = "Midnight Coffee Break",
            artist = "Tokyo Rain",
            artistId = "art_3",
            album = "Coffee & Raindrops",
            albumId = "alb_3",
            albumArtUrl = "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/03/10/audio_c3527b1406.mp3",
            durationMs = 158000L,
            genre = "Lo-Fi",
            lyrics = listOf(
                LyricLine(0L, "[Mellow trumpet & jazz chords]"),
                LyricLine(15000L, "Midnight study session by the lamp"),
                LyricLine(30000L, "Distant sound of sneakers on the ramp"),
                LyricLine(45000L, "Ideas flowing easy as the beat"),
                LyricLine(60000L, "Tapping rhythm softly with my feet"),
                LyricLine(80000L, "One more sip to keep the focus tight"),
                LyricLine(100000L, "We can finish this before morning light"),
                LyricLine(125000L, "[Chill trumpet improvisation]"),
                LyricLine(145000L, "[Soft tape click]")
            )
        ),
        Song(
            id = "song_8",
            title = "Hyperdrive Overload",
            artist = "Aero Pulse",
            artistId = "art_4",
            album = "Hyperdrive Velocity",
            albumId = "alb_4",
            albumArtUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/11/06/audio_05bc03f901.mp3",
            durationMs = 210000L,
            genre = "Electronic",
            lyrics = listOf(
                LyricLine(0L, "[Heavy 4/4 Kick and Filter Sweep]"),
                LyricLine(18000L, "System ready, powering the core"),
                LyricLine(32000L, "Feel the frequency vibrating through the floor"),
                LyricLine(46000L, "Three, two, one, initiate the rise"),
                LyricLine(58000L, "[MASSIVE BASS DROP]"),
                LyricLine(85000L, "Can you feel the energy collide?"),
                LyricLine(105000L, "No more hesitation on this ride"),
                LyricLine(128000L, "[Second Build up]"),
                LyricLine(145000L, "[Future Bass Drop & Staccato Chords]"),
                LyricLine(180000L, "Hyperdrive maximum capacity reached!"),
                LyricLine(198000L, "[Outro synth decay]")
            )
        ),
        Song(
            id = "song_9",
            title = "Sunset Coast Acoustic",
            artist = "Maya Lin",
            artistId = "art_5",
            album = "Golden Horizons",
            albumId = "alb_5",
            albumArtUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/01/26/audio_d0c6ff1e01.mp3",
            durationMs = 182000L,
            genre = "Acoustic",
            lyrics = listOf(
                LyricLine(0L, "[Warm acoustic guitar fingerpicking]"),
                LyricLine(12000L, "Waves crashing softly on the sand"),
                LyricLine(24000L, "Watching the sunset across the land"),
                LyricLine(38000L, "All the worries of the week are gone"),
                LyricLine(50000L, "Singing with the ocean until dawn"),
                LyricLine(66000L, "Take a breath, feel the salty air"),
                LyricLine(80000L, "Golden sunlight tangled in your hair"),
                LyricLine(98000L, "Simple moments are the ones that last"),
                LyricLine(114000L, "Letting go of shadows from the past"),
                LyricLine(135000L, "[Harmonica & Guitar Duet]"),
                LyricLine(160000L, "Golden horizons shining over you"),
                LyricLine(172000L, "[Gentle strum to finish]")
            )
        )
    )

    val moodCategories = listOf(
        "All",
        "Synthwave",
        "Lo-Fi",
        "Dream Pop",
        "Electronic",
        "Acoustic",
        "Chillout"
    )

    fun getSongById(id: String): Song? = songs.find { it.id == id }
    fun getSongsByArtist(artistId: String): List<Song> = songs.filter { it.artistId == artistId }
    fun getSongsByAlbum(albumId: String): List<Song> = songs.filter { it.albumId == albumId }
    fun getSongsByGenre(genre: String): List<Song> {
        if (genre == "All") return songs
        return songs.filter { it.genre.equals(genre, ignoreCase = true) }
    }
}
