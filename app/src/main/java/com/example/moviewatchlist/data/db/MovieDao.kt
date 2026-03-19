package com.example.moviewatchlist.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.moviewatchlist.data.model.Movie
import com.example.moviewatchlist.data.model.WatchStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(movie: Movie): Long

    @Update
    suspend fun update(movie: Movie)

    @Delete
    suspend fun delete(movie: Movie)

    @Query("SELECT * FROM movies ORDER BY title COLLATE NOCASE ASC")
    fun getAll(): Flow<List<Movie>>

    @Query("SELECT * FROM movies WHERE status = :status ORDER BY title COLLATE NOCASE ASC")
    fun getByStatus(status: WatchStatus): Flow<List<Movie>>

    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Movie?>
}

