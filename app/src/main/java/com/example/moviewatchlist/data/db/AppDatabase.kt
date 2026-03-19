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
                                    CoroutineScope(Dispatchers.IO).launch {
                                        getInstance(appContext).movieDao().apply {
                                            insert(
                                                Movie(
                                                    title = "Interstellar",
                                                    status = WatchStatus.PLAN_TO_WATCH,
                                                    synopsis = "A team travels through a wormhole to ensure humanity’s survival.",
                                                    runtimeMinutes = 169,
                                                ),
                                            )
                                            insert(
                                                Movie(
                                                    title = "The Dark Knight",
                                                    status = WatchStatus.COMPLETED,
                                                    rating = 5f,
                                                    review = "A standout superhero crime thriller with incredible performances.",
                                                    runtimeMinutes = 152,
                                                ),
                                            )
                                            insert(
                                                Movie(
                                                    title = "Breaking Bad (Pilot)",
                                                    status = WatchStatus.CURRENTLY_WATCHING,
                                                    rating = 4.5f,
                                                    review = "Hooked already—great setup and characters.",
                                                    runtimeMinutes = 58,
                                                ),
                                            )
                                            insert(
                                                Movie(
                                                    title = "Some Long Series",
                                                    status = WatchStatus.DROPPED,
                                                    rating = 2f,
                                                    review = "Couldn’t stay engaged after a few episodes.",
                                                ),
                                            )
                                        }
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

