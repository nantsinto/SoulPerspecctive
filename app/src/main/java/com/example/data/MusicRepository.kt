package com.example.data

import com.example.model.MusicCatalog
import com.example.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class MusicRepository(private val dao: MusicDao) {

    val allPlaylists: Flow<List<PlaylistEntity>> = dao.getAllPlaylists()

    val favoriteSongIds: Flow<Set<String>> = dao.getAllFavorites().map { list ->
        list.map { it.songId }.toSet()
    }

    val favoriteSongs: Flow<List<Song>> = favoriteSongIds.map { idSet ->
        MusicCatalog.songs.filter { it.id in idSet }
    }

    val historySongs: Flow<List<Song>> = dao.getRecentHistorySongIds().map { ids ->
        val distinctIds = ids.distinct()
        distinctIds.mapNotNull { id -> MusicCatalog.getSongById(id) }
    }

    fun isFavorite(songId: String): Flow<Boolean> = dao.isFavorite(songId)

    suspend fun toggleFavorite(songId: String) {
        val exists = dao.isFavorite(songId).firstOrNull() ?: false
        if (exists) {
            dao.removeFavorite(songId)
        } else {
            dao.addFavorite(FavoriteSongEntity(songId))
        }
    }

    suspend fun createPlaylist(title: String, description: String, coverUrl: String? = null): Long {
        val finalCover = coverUrl?.takeIf { it.isNotBlank() }
            ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
        return dao.insertPlaylist(
            PlaylistEntity(
                title = title,
                description = description,
                coverUrl = finalCover
            )
        )
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: String) {
        dao.addSongToPlaylist(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                orderIndex = System.currentTimeMillis().toInt()
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        dao.removeSongFromPlaylist(playlistId, songId)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return dao.getSongIdsForPlaylist(playlistId).map { ids ->
            ids.mapNotNull { id -> MusicCatalog.getSongById(id) }
        }
    }

    suspend fun recordHistory(songId: String) {
        dao.insertHistory(HistoryEntity(songId = songId))
    }

    fun search(query: String): List<Song> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return MusicCatalog.songs
        return MusicCatalog.songs.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q) ||
            it.genre.lowercase().contains(q)
        }
    }
}
