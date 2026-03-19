package com.example.moviewatchlist.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moviewatchlist.di.ServiceLocator
import com.example.moviewatchlist.R
import com.example.moviewatchlist.databinding.FragmentSearchBinding
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding: FragmentSearchBinding
        get() = _binding!!

    private val viewModel: SearchViewModel by lazy {
        val repo = ServiceLocator.provideMovieRepository(requireContext())
        val factory = SearchViewModel.Factory(repo)
        ViewModelProvider(this, factory)[SearchViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = SearchAdapter { movie ->
            viewModel.onMovieSelected(movie) { movieId ->
                val args = Bundle().apply { putLong("movieId", movieId) }
                findNavController().navigate(R.id.movieDetailFragment, args)
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.search(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.errorText.visibility = if (!state.errorMessage.isNullOrBlank()) View.VISIBLE else View.GONE
                    binding.errorText.text = state.errorMessage.orEmpty()

                    binding.emptyState.visibility =
                        if (!state.isLoading && state.errorMessage == null && state.results.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    adapter.submitList(state.results)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

