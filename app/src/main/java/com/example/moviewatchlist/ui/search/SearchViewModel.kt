package com.example.moviewatchlist.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moviewatchlist.data.model.Movie
import com.example.moviewatchlist.data.repository.MovieRepository
import com.example.moviewatchlist.data.remote.tmdb.TmdbException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import java.util.concurrent.CancellationException

data class SearchUiState(
    val isLoading: Boolean = false,
    val results: List<Movie> = emptyList(),
    val errorMessage: String? = null,
)

class SearchViewModel(
    private val repository: MovieRepository,
) : ViewModel() {
    private val queryFlow = MutableStateFlow("")

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    val trimmed = query.trim()
                    if (trimmed.isBlank()) {
                        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                        repository.allMovies.collect { movies ->
                            _uiState.update { it.copy(isLoading = false, results = movies, errorMessage = null) }
                        }
                        return@collectLatest
                    }

                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    
                    // Local search is reactive via Flow
                    repository.searchLocalMovies(trimmed).collect { movies ->
                        _uiState.update { it.copy(isLoading = false, results = movies, errorMessage = null) }
                    }
                }
        }
    }

    fun search(query: String) {
        queryFlow.value = query
    }

    fun onMovieSelected(movie: Movie, onInserted: (Long) -> Unit) {
        viewModelScope.launch {
            val movieId = repository.getOrInsertMovie(movie)
            onInserted(movieId)
        }
    }

    private fun Throwable.toUserMessage(): String {
        return when (this) {
            is TmdbException -> message ?: "Failed to fetch results from TMDB."
            else -> "Something went wrong while searching."
        }
    }

    class Factory(
        private val repository: MovieRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                return SearchViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

