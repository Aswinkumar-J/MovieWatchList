package com.example.moviewatchlist.ui.list

import androidx.lifecycle.viewModelScope
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moviewatchlist.data.model.Movie
import com.example.moviewatchlist.data.model.WatchStatus
import com.example.moviewatchlist.databinding.FragmentMovieListBinding
import com.example.moviewatchlist.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MovieListFragment : Fragment() {
    private var _binding: FragmentMovieListBinding? = null
    private val binding: FragmentMovieListBinding
        get() = _binding!!

    private val statusFilter: WatchStatus?
        get() = arguments?.getString(ARG_STATUS)?.let { WatchStatus.valueOf(it) }

    private val viewModel: MovieListViewModel by lazy {
        val repo = ServiceLocator.provideMovieRepository(requireContext())
        ViewModelProvider(
            this,
            MovieListViewModel.Factory(repo, statusFilter),
        )[MovieListViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMovieListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = MovieAdapter { movieId ->
            val direction = com.example.moviewatchlist.ui.main.MainFragmentDirections
                .actionMainFragmentToMovieDetailFragment(movieId)
            findNavController().navigate(direction)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.movies.collect { movies ->
                    adapter.submitList(movies)
                    binding.emptyState.visibility = if (movies.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_STATUS = "arg_status"

        fun newInstance(statusFilter: WatchStatus?): MovieListFragment =
            MovieListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_STATUS, statusFilter?.name)
                }
            }
    }
}

class MovieListViewModel(
    repository: com.example.moviewatchlist.data.repository.MovieRepository,
    statusFilter: WatchStatus?,
) : ViewModel() {
    val movies: StateFlow<List<Movie>> =
        (statusFilter?.let { repository.moviesByStatus(it) } ?: repository.allMovies)
            .map { it }
            .stateIn(
//                scope = androidx.lifecycle.viewModelScope,
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    class Factory(
        private val repository: com.example.moviewatchlist.data.repository.MovieRepository,
        private val statusFilter: WatchStatus?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MovieListViewModel::class.java)) {
                return MovieListViewModel(repository, statusFilter) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

