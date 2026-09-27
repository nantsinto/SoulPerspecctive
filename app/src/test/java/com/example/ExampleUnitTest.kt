package com.example

import com.example.data.ProjectHubRegistry
import com.example.model.MusicCatalog
import com.example.model.SuggestionOptionMode
import com.example.model.SuggestionsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test suggestions engine similarity and modes`() {
        val seed = MusicCatalog.songs.first()
        val allSuggestions = SuggestionsEngine.generateSuggestions(seed, SuggestionOptionMode.ALL)
        assertTrue(allSuggestions.isNotEmpty())
        assertFalse(allSuggestions.any { it.song.id == seed.id }) // Should not suggest itself

        val artistSuggestions = SuggestionsEngine.generateSuggestions(seed, SuggestionOptionMode.SAME_ARTIST)
        assertTrue(artistSuggestions.isNotEmpty())

        val sonicSuggestions = SuggestionsEngine.generateSuggestions(seed, SuggestionOptionMode.SONIC_MATCH)
        assertTrue(sonicSuggestions.isNotEmpty())
        assertTrue(sonicSuggestions.first().matchScore >= 50)
    }

    @Test
    fun `test suggestions radio generation`() {
        val seed = MusicCatalog.songs.first()
        val radio = SuggestionsEngine.generateRadioStation(seed, count = 8)
        assertEquals(8, radio.size)
        assertEquals(seed.id, radio.first().id) // First track is the seed
    }

    @Test
    fun `test search suggestions autocomplete`() {
        val emptyQuerySuggestions = SuggestionsEngine.getSearchSuggestions("")
        assertTrue(emptyQuerySuggestions.isNotEmpty())

        val neonSuggestions = SuggestionsEngine.getSearchSuggestions("neon")
        assertTrue(neonSuggestions.any { it.contains("Neon", ignoreCase = true) })
    }

    @Test
    fun `test integrated project files in hub registry`() {
        val docs = ProjectHubRegistry.documents
        assertTrue(docs.size >= 8)

        // Verify key uploaded files are integrated
        val filenames = docs.map { it.filename }
        assertTrue(filenames.contains("START_HERE.txt"))
        assertTrue(filenames.contains("ARCHITECTURE.md"))
        assertTrue(filenames.contains("dashboard.tsx"))
        assertTrue(filenames.contains("GETTING_STARTED.md"))
        assertTrue(filenames.contains("IMPLEMENTATION_SUMMARY.md"))
        assertTrue(filenames.contains("MEMORY.md"))
        assertTrue(filenames.contains("CLAUDE.md"))
        assertTrue(filenames.contains("conveyor.conf"))

        val metrics = ProjectHubRegistry.metrics
        assertTrue(metrics.isNotEmpty())
        assertTrue(metrics.any { it.label.contains("Audio Pipeline") })
        assertTrue(metrics.any { it.label.contains("Suggestions Engine") })
    }
}
