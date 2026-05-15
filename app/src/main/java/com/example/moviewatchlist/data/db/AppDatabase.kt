package com.example.moviewatchlist.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.moviewatchlist.data.model.Movie
import com.example.moviewatchlist.data.model.WatchStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Movie::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(appContext: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: Room.databaseBuilder(
                        appContext.applicationContext,
                        AppDatabase::class.java,
                        "movie_watchlist.db",
                    )
                        .addCallback(
                            object : Callback() {
                                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                                    super.onCreate(db)
                                    // Pre-populate database on a background thread
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val dao = getInstance(appContext).movieDao()
                                        dao.insert(
                                            Movie(
                                                title = "Interstellar",
                                                status = WatchStatus.PLAN_TO_WATCH,
                                                synopsis = "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.",
                                                runtimeMinutes = 169,
                                                tmdbId = 157336,
                                                tmdbVoteAverage = 8.4f
                                            )
                                        )
                                        dao.insert(
                                            Movie(
                                                title = "The Dark Knight",
                                                status = WatchStatus.COMPLETED,
                                                rating = 5f,
                                                review = "A masterpiece of the superhero genre. Ledger's performance is legendary.",
                                                runtimeMinutes = 152,
                                                tmdbId = 155,
                                                tmdbVoteAverage = 8.5f
                                            )
                                        )
                                        dao.insert(
                                            Movie(
                                                title = "Inception",
                                                status = WatchStatus.CURRENTLY_WATCHING,
                                                synopsis = "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea.",
                                                runtimeMinutes = 148,
                                                tmdbId = 27205,
                                                tmdbVoteAverage = 8.3f
                                            )
                                        )
                                    }
                                }
                            },
                        )
                        .fallbackToDestructiveMigration()
                        .build()
                        .also { INSTANCE = it }
            }
    }
}

