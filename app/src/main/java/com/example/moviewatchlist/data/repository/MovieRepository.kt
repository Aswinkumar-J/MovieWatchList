package com.example.moviewatchlist.data.repository

import com.example.moviewatchlist.data.db.MovieDao
import com.example.moviewatchlist.data.remote.tmdb.TmdbApiService
import com.example.moviewatchlist.data.remote.tmdb.TmdbException
import com.example.moviewatchlist.data.model.Movie
import com.example.moviewatchlist.data.model.WatchStatus
import com.example.moviewatchlist.util.TmdbConstants
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException

class MovieRepository(
    private val movieDao: MovieDao,
    private val tmdbApiService: TmdbApiService,
) {
    val allMovies: Flow<List<Movie>> = movieDao.getAll()

    fun moviesByStatus(status: WatchStatus): Flow<List<Movie>> = movieDao.getByStatus(status.name)

    fun movieById(id: Long): Flow<Movie?> = movieDao.getById(id)

    suspend fun insert(movie: Movie): Long {
        val tmdbId = movie.tmdbId
        if (tmdbId != null) {
            val existing = movieDao.getByTmdbIds(listOf(tmdbId)).firstOrNull()
            if (existing != null) {
                // Movie already in watchlist, update it while preserving the local primary key (id)
                val movieToUpdate = movie.copy(id = existing.id)
                movieDao.update(movieToUpdate)
                return existing.id
            }
        }
        return movieDao.insert(movie)
    }

    suspend fun update(movie: Movie) = movieDao.update(movie)

    suspend fun delete(movie: Movie) = movieDao.delete(movie)
    
    fun searchLocalMovies(query: String): Flow<List<Movie>> = movieDao.search(query)

    suspend fun searchMovies(query: String): List<Movie> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()

        val response = try {
            tmdbApiService.searchMovies(
                query = trimmed,
                apiKey = TmdbConstants.TMDB_API_KEY,
            )
        } catch (e: HttpException) {
            throw TmdbException("TMDB request failed (HTTP ${e.code()}).", e)
        } catch (e: IOException) {
            throw TmdbException("Network error while searching TMDB.", e)
        }

        if (response.results.isEmpty()) return emptyList()

        val moviesToCache = response.results.map { dto ->
            Movie(
                title = dto.title.orEmpty().ifBlank { "Untitled" },
                status = WatchStatus.PLAN_TO_WATCH,
                rating = 0f,
                review = null,
                synopsis = dto.overview,
                runtimeMinutes = null,
                posterUrl = dto.poster_path?.let { "${TmdbConstants.TMDB_IMAGE_BASE_URL}$it" },
                tmdbId = dto.id,
                tmdbVoteAverage = dto.vote_average?.toFloat(),
            )
        }

        val tmdbIds = moviesToCache.mapNotNull { it.tmdbId }.distinct()
        if (tmdbIds.isEmpty()) return emptyList()

        val cached = movieDao.getByTmdbIds(tmdbIds)
        val cachedByTmdbId = cached.associateBy { it.tmdbId }

        // Return cached if available, else return the new uninserted movie
        return moviesToCache.map { movie ->
            cachedByTmdbId[movie.tmdbId] ?: movie
        }
    }

    suspend fun getOrInsertMovie(movie: Movie): Long {
        // If it already has a local ID, it's already in the DB
        if (movie.id != 0L) return movie.id
        
        val tmdbId = movie.tmdbId
        if (tmdbId != null) {
            val existing = movieDao.getByTmdbIds(listOf(tmdbId)).firstOrNull()
            if (existing != null) return existing.id
        }
        
        // Not found by ID or TMDB ID, perform a fresh insert
        return movieDao.insert(movie)
    }

    suspend fun getMovieDetails(tmdbId: Long): Movie? {
        return try {
            val dto = tmdbApiService.getMovieDetails(
                movieId = tmdbId,
                apiKey = TmdbConstants.TMDB_API_KEY,
            )
            Movie(
                title = dto.title.orEmpty(),
                status = WatchStatus.PLAN_TO_WATCH,
                synopsis = dto.overview,
                posterUrl = dto.poster_path?.let { "${TmdbConstants.TMDB_IMAGE_BASE_URL}$it" },
                tmdbId = dto.id,
                tmdbVoteAverage = dto.vote_average?.toFloat(),
                runtimeMinutes = dto.runtime,
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getGenres(): List<com.example.moviewatchlist.data.model.Genre> {
        return try {
            val response = tmdbApiService.getGenres(TmdbConstants.TMDB_API_KEY)
            response.genres.map { com.example.moviewatchlist.data.model.Genre(it.id, it.name) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun discoverMovies(genreId: Int?): List<Movie> {
        return try {
            val response = tmdbApiService.discoverMovies(
                genreId = genreId,
                apiKey = TmdbConstants.TMDB_API_KEY,
            )
            response.results.map { dto ->
                Movie(
                    title = dto.title.orEmpty(),
                    status = WatchStatus.PLAN_TO_WATCH,
                    synopsis = dto.overview,
                    posterUrl = dto.poster_path?.let { "${TmdbConstants.TMDB_IMAGE_BASE_URL}$it" },
                    tmdbId = dto.id,
                    tmdbVoteAverage = dto.vote_average?.toFloat(),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
//