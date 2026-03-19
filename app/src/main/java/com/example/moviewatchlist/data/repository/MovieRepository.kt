package com.example.moviewatchlist.data.repository

import com.example.moviewatchlist.data.db.MovieDao
import com.example.moviewatchlist.data.model.Movie
import com.example.moviewatchlist.data.model.WatchStatus
import kotlinx.coroutines.flow.Flow

class MovieRepository(
    private val movieDao: MovieDao,
) {
    val allMovies: Flow<List<Movie>> = movieDao.getAll()

    fun moviesByStatus(status: WatchStatus): Flow<List<Movie>> = movieDao.getByStatus(status)

    fun movieById(id: Long): Flow<Movie?> = movieDao.getById(id)

    suspend fun insert(movie: Movie): Long = movieDao.insert(movie)

    suspend fun update(movie: Movie) = movieDao.update(movie)

    suspend fun delete(movie: Movie) = movieDao.delete(movie)
}

