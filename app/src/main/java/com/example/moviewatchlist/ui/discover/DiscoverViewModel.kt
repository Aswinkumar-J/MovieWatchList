package com.example.moviewatchlist.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moviewatchlist.data.model.Genre
import com.example.moviewatchlist.data.model.Movie
import com.example.moviewatchlist.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val genres: List<Genre> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val selectedGenreId: Int? = null,
    val errorMessage: String? = null,
)

class DiscoverViewModel(private val repository: MovieRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var currentGenreId: Int? = null

    init {
        fetchGenres()
        
        viewModelScope.launch {
            queryFlow
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        discoverMovies(currentGenreId)
                    } else {
                        performSearch(query)
                    }
                }
        }
    }

    fun searchMovies(query: String) {
        queryFlow.value = query
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val movies = repository.searchMovies(query)
            _uiState.update { it.copy(movies = movies, isLoading = false, errorMessage = null) }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    isLoading = false, 
                    errorMessage = e.localizedMessage ?: "Search failed. Please try again."
                ) 
            }
        }
    }

    private fun fetchGenres() {
        viewModelScope.launch {
            try {
                val genres = repository.getGenres()
                _uiState.update { it.copy(genres = genres) }
            } catch (e: Exception) {
                // Ignore genre errors for now
            }
        }
    }

    fun selectGenre(genreId: Int?) {
        currentGenreId = genreId
        _uiState.update { it.copy(selectedGenreId = genreId, errorMessage = null) }
        // Clear search when changing genre
        if (queryFlow.value.isNotBlank()) {
            queryFlow.value = ""
        } else {
            viewModelScope.launch {
                discoverMovies(genreId)
            }
        }
    }

    private suspend fun discoverMovies(genreId: Int?) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val movies = repository.discoverMovies(genreId)
            _uiState.update { it.copy(movies = movies, isLoading = false, errorMessage = null) }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    isLoading = false, 
                    errorMessage = e.localizedMessage ?: "Failed to load movies." 
                ) 
            }
        }
    }

    class Factory(private val repository: MovieRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiscoverViewModel(repository) as T
        }
    }
}
