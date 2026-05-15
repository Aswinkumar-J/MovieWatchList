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
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val genres: List<Genre> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val selectedGenreId: Int? = null,
)

class DiscoverViewModel(private val repository: MovieRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var currentGenreId: Int? = null

    init {
        fetchGenres()
        discoverMovies(null)
    }

    fun searchMovies(query: String) {
        if (query.isBlank()) {
            discoverMovies(currentGenreId)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val movies = repository.searchMovies(query)
            _uiState.update { it.copy(movies = movies, isLoading = false) }
        }
    }

    private fun fetchGenres() {
        viewModelScope.launch {
            val genres = repository.getGenres()
            _uiState.update { it.copy(genres = genres) }
        }
    }

    fun selectGenre(genreId: Int?) {
        currentGenreId = genreId
        _uiState.update { it.copy(selectedGenreId = genreId) }
        discoverMovies(genreId)
    }

    private fun discoverMovies(genreId: Int?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val movies = repository.discoverMovies(genreId)
            _uiState.update { it.copy(movies = movies, isLoading = false) }
        }
    }

    class Factory(private val repository: MovieRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiscoverViewModel(repository) as T
        }
    }
}
