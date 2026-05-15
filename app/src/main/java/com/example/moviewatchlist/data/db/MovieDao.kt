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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movie: Movie): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreByTmdbId(movie: Movie): Long

    @Update
    suspend fun update(movie: Movie)

    @Delete
    suspend fun delete(movie: Movie)

    @Query("SELECT * FROM movies ORDER BY title COLLATE NOCASE ASC")
    fun getAll(): Flow<List<Movie>>

    @Query("SELECT * FROM movies WHERE status = :status ORDER BY title COLLATE NOCASE ASC")
    fun getByStatus(status: String): Flow<List<Movie>>

    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Movie?>

    @Query("SELECT * FROM movies WHERE tmdbId = :tmdbId LIMIT 1")
    fun getByTmdbId(tmdbId: Long): Flow<Movie?>

    @Query("SELECT * FROM movies WHERE tmdbId IN (:tmdbIds)")
    suspend fun getByTmdbIds(tmdbIds: List<Long>): List<Movie>

    @Query("SELECT * FROM movies WHERE title LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE ASC")
    fun search(query: String): Flow<List<Movie>>
}

