package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        FavoriteSongEntity::class,
        HistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SimpDatabase : RoomDatabase() {

    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: SimpDatabase? = null

        fun getDatabase(context: Context): SimpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SimpDatabase::class.java,
                    "simpmusic.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate with default playlists
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getDatabase(context).musicDao()
                            val pl1Id = dao.insertPlaylist(
                                PlaylistEntity(
                                    title = "Cyber & Retrowave Vibes",
                                    description = "Fast arps, glowing synth leads and retro night drives.",
                                    coverUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=600&auto=format&fit=crop&q=80"
                                )
                            )
                            dao.addSongToPlaylist(PlaylistSongEntity(pl1Id, "song_1", 0))
                            dao.addSongToPlaylist(PlaylistSongEntity(pl1Id, "song_2", 1))
                            dao.addSongToPlaylist(PlaylistSongEntity(pl1Id, "song_8", 2))

                            val pl2Id = dao.insertPlaylist(
                                PlaylistEntity(
                                    title = "Coffee, Rain & Study",
                                    description = "Warm lo-fi beats, gentle pianos, and calming rain sounds.",
                                    coverUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80"
                                )
                            )
                            dao.addSongToPlaylist(PlaylistSongEntity(pl2Id, "song_6", 0))
                            dao.addSongToPlaylist(PlaylistSongEntity(pl2Id, "song_7", 1))
                            dao.addSongToPlaylist(PlaylistSongEntity(pl2Id, "song_3", 2))

                            // Initial favorites
                            dao.addFavorite(FavoriteSongEntity("song_1"))
                            dao.addFavorite(FavoriteSongEntity("song_4"))
                            dao.addFavorite(FavoriteSongEntity("song_6"))
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
