package com.example.moviewatchlist.data.repository

import com.example.moviewatchlist.data.db.MovieDao
import com.example.moviewatchlist.data.remote.tmdb.TmdbApiService
import com.example.moviewatchlist.data.remote.tmdb.TmdbException
import com.example.moviewatchlist.data.model.Movie
import com.example.moviewatchlist.data.model.WatchStatus
import com.example.moviewatchlist.util.TmdbConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.io.IOException

class MovieRepository(
    private val movieDao: MovieDao,
    private val tmdbApiService: TmdbApiService,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watchlistListener: ListenerRegistration? = null

    private val _isSyncing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSyncing: kotlinx.coroutines.flow.StateFlow<Boolean> = _isSyncing


    val allMovies: Flow<List<Movie>> = movieDao.getAll()

    fun moviesByStatus(status: WatchStatus): Flow<List<Movie>> = movieDao.getByStatus(status.name)

    fun movieById(id: Long): Flow<Movie?> = movieDao.getById(id)

    suspend fun insert(movie: Movie): Long {
        val tmdbId = movie.tmdbId
        val localId = if (tmdbId != null) {
            val existing = movieDao.getByTmdbIds(listOf(tmdbId)).firstOrNull()
            if (existing != null) {
                val movieToUpdate = movie.copy(id = existing.id)
                movieDao.update(movieToUpdate)
                existing.id
            } else {
                movieDao.insert(movie)
            }
        } else {
            movieDao.insert(movie)
        }
        
        // Sync to cloud
        syncToCloud(movie.copy(id = localId))
        return localId
    }

    suspend fun update(movie: Movie) {
        movieDao.update(movie)
        syncToCloud(movie)
    }

    suspend fun delete(movie: Movie) {
        movieDao.delete(movie)
        removeFromCloud(movie)
    }

    private fun getWatchlistRef(): com.google.firebase.firestore.CollectionReference? {
        val user = auth.currentUser ?: return null
        return firestore.collection("users").document(user.uid).collection("watchlist")
    }

    private fun syncToCloud(movie: Movie) {
        val tmdbId = movie.tmdbId ?: return
        repositoryScope.launch {
            try {
                getWatchlistRef()?.document(tmdbId.toString())?.set(movie)?.await()
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    private fun removeFromCloud(movie: Movie) {
        val tmdbId = movie.tmdbId ?: return
        repositoryScope.launch {
            try {
                getWatchlistRef()?.document(tmdbId.toString())?.delete()?.await()
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    fun startWatchlistSync() {
        watchlistListener?.remove()
        val user = auth.currentUser ?: return
        val watchlistRef = firestore.collection("users").document(user.uid).collection("watchlist")

        _isSyncing.value = true
        watchlistListener = watchlistRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                _isSyncing.value = false
                return@addSnapshotListener
            }

            snapshot?.let { querySnapshot ->
                val remoteMovies = querySnapshot.toObjects(Movie::class.java)
                repositoryScope.launch {
                    try {
                        val localMovies = movieDao.getAll().first()
                        val remoteTmdbIds = remoteMovies.mapNotNull { it.tmdbId }.toSet()

                        // Update/Insert from remote to local
                        remoteMovies.forEach { remoteMovie ->
                            val tmdbId = remoteMovie.tmdbId ?: return@forEach
                            val existing = movieDao.getByTmdbIds(listOf(tmdbId)).firstOrNull()
                            if (existing != null) {
                                movieDao.update(remoteMovie.copy(id = existing.id))
                            } else {
                                movieDao.insert(remoteMovie.copy(id = 0))
                            }
                        }
                    } finally {
                        _isSyncing.value = false
                    }

                }
            } ?: run {
                _isSyncing.value = false
            }
        }
    }


    fun stopWatchlistSync() {
        watchlistListener?.remove()
        watchlistListener = null
    }

    // Existing TMDB methods
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
            val errorBody = e.response()?.errorBody()?.string()
            val message = try {
                val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                json.get("status_message").asString
            } catch (parseEx: Exception) {
                "TMDB request failed (HTTP ${e.code()})."
            }
            throw TmdbException(message, e)
        } catch (e: IOException) {
            throw TmdbException("Network error while searching TMDB. Please check your internet connection.", e)
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

        return moviesToCache.map { movie ->
            cachedByTmdbId[movie.tmdbId] ?: movie
        }
    }

    suspend fun getOrInsertMovie(movie: Movie): Long {
        if (movie.id != 0L) return movie.id
        
        val tmdbId = movie.tmdbId
        if (tmdbId != null) {
            val existing = movieDao.getByTmdbIds(listOf(tmdbId)).firstOrNull()
            if (existing != null) return existing.id
        }
        
        val newId = movieDao.insert(movie)
        syncToCloud(movie.copy(id = newId))
        return newId
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