package com.example.moviewatchlist.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moviewatchlist.data.model.Movie
import com.example.moviewatchlist.data.model.WatchStatus
import com.example.moviewatchlist.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class MovieDetailUiState(
    val movieId: Long = -1L,
    val title: String = "",
    val status: WatchStatus = WatchStatus.PLAN_TO_WATCH,
    val rating: Float = 0f,
    val review: String? = null,
    val synopsis: String? = null,
    val runtimeMinutes: Int? = null,
    val posterUrl: String? = null,
    val tmdbId: Long? = null,
    val tmdbVoteAverage: Float? = null,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
)

class MovieDetailViewModel(
    private val repository: MovieRepository,
    private val movieId: Long,
    private val tmdbIdArg: Long = -1L,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MovieDetailUiState(movieId = movieId))
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    val isCreateMode: Boolean
        get() = _uiState.value.movieId < 0

    init {
        if (!isCreateMode) {
            viewModelScope.launch {
                val movie = repository.movieById(movieId).filterNotNull().firstOrNull()
                if (movie != null) {
                    _uiState.value = movie.toUiState(isLoaded = true)
                    
                    // If runtime or synopsis are missing, fetch from TMDB.
                    if ((movie.runtimeMinutes == null || movie.synopsis.isNullOrBlank()) && movie.tmdbId != null) {
                        val fullMovie = repository.getMovieDetails(movie.tmdbId)
                        if (fullMovie != null) {
                            val updatedMovie = movie.copy(
                                runtimeMinutes = fullMovie.runtimeMinutes ?: movie.runtimeMinutes,
                                synopsis = if (movie.synopsis.isNullOrBlank()) fullMovie.synopsis else movie.synopsis
                            )
                            _uiState.value = updatedMovie.toUiState(isLoaded = true)
                            repository.update(updatedMovie)
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoaded = true)
                }
            }
        } else if (tmdbIdArg != -1L) {
            // New movie from Discovery/Search: fetch details first
            viewModelScope.launch {
                // Check if we already have this TMDB movie in our list
                val existing = repository.allMovies.first().find { it.tmdbId == tmdbIdArg }
                
                if (existing != null) {
                    // It's already in our list! Switch to edit mode for this existing ID.
                    _uiState.value = existing.toUiState(isLoaded = true)
                } else {
                    // Not in list, fetch fresh details from TMDB
                    val fullMovie = repository.getMovieDetails(tmdbIdArg)
                    if (fullMovie != null) {
                        _uiState.value = fullMovie.toUiState(isLoaded = true).copy(movieId = -1L)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoaded = true)
                    }
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(isLoaded = true)
        }
    }

    fun setTitle(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
    }

    fun setStatus(value: WatchStatus) {
        val newState = if (value == WatchStatus.PLAN_TO_WATCH) {
            _uiState.value.copy(status = value, rating = 0f, review = null)
        } else {
            _uiState.value.copy(status = value)
        }
        _uiState.value = newState
    }

    fun setRating(value: Float) {
        val newState = if (value > 0f) {
            _uiState.value.copy(rating = value, status = WatchStatus.COMPLETED)
        } else {
            _uiState.value.copy(rating = value)
        }
        _uiState.value = newState
    }

    fun setRuntimeMinutes(value: Int?) {
        _uiState.value = _uiState.value.copy(runtimeMinutes = value)
    }

    fun setReview(value: String?) {
        val trimmed = value?.takeIf { it.isNotBlank() }
        val newState = if (trimmed != null) {
            _uiState.value.copy(review = trimmed, status = WatchStatus.COMPLETED)
        } else {
            _uiState.value.copy(review = trimmed)
        }
        _uiState.value = newState
    }

    fun setSynopsis(value: String?) {
        _uiState.value = _uiState.value.copy(synopsis = value?.takeIf { it.isNotBlank() })
    }

    fun save(onSaved: () -> Unit) {
        val current = _uiState.value
        if (current.isSaving) return

        _uiState.value = current.copy(isSaving = true)

        viewModelScope.launch {
            val movie = current.toMovieForSave()
            if (isCreateMode) {
                repository.insert(movie)
            } else {
                repository.update(movie)
            }
            _uiState.value = _uiState.value.copy(isSaving = false)
            onSaved()
        }
    }

    fun deleteMovie(onDeleted: () -> Unit) {
        val currentMovieId = _uiState.value.movieId
        if (currentMovieId < 0) return

        viewModelScope.launch {
            val movie = repository.movieById(currentMovieId).filterNotNull().firstOrNull()
            if (movie != null) {
                repository.delete(movie)
                onDeleted()
            }
        }
    }

    private fun Movie.toUiState(isLoaded: Boolean): MovieDetailUiState =
        MovieDetailUiState(
            movieId = id,
            title = title,
            status = status,
            rating = rating,
            review = review,
            synopsis = synopsis,
            runtimeMinutes = runtimeMinutes,
            posterUrl = posterUrl,
            tmdbId = tmdbId,
            tmdbVoteAverage = tmdbVoteAverage,
            isLoaded = isLoaded,
        )

    private fun MovieDetailUiState.toMovieForSave(): Movie =
        Movie(
            id = if (movieId < 0) 0 else movieId,
            title = title.trim(),
            status = status,
            rating = rating,
            review = review,
            synopsis = synopsis,
            runtimeMinutes = runtimeMinutes,
            posterUrl = posterUrl,
            tmdbId = tmdbId,
            tmdbVoteAverage = tmdbVoteAverage,
        )

    class Factory(
        private val repository: MovieRepository,
        private val movieId: Long,
        private val tmdbIdArg: Long = -1L,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MovieDetailViewModel::class.java)) {
                return MovieDetailViewModel(repository, movieId, tmdbIdArg) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

