package com.example.moviewatchlist.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.moviewatchlist.R
import com.example.moviewatchlist.databinding.FragmentDiscoverBinding
import com.example.moviewatchlist.di.ServiceLocator
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class DiscoverFragment : Fragment() {
    private var _binding: FragmentDiscoverBinding? = null
    private val binding: FragmentDiscoverBinding get() = _binding!!

    private val viewModel: DiscoverViewModel by lazy {
        val repo = ServiceLocator.provideMovieRepository(requireContext())
        ViewModelProvider(this, DiscoverViewModel.Factory(repo))[DiscoverViewModel::class.java]
    }

    private lateinit var adapter: DiscoverAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DiscoverAdapter { movie ->
            // Open detail screen for the selected TMDB movie
            movie.tmdbId?.let { tmdbId ->
                findNavController().navigate(
                    DiscoverFragmentDirections.actionDiscoverFragmentToMovieDetailFragment(movieId = -1L, tmdbIdArg = tmdbId)
                )
            }
        }

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = adapter

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.searchMovies(s?.toString().orEmpty())
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.isVisible = state.isLoading
                    binding.errorText.isVisible = !state.errorMessage.isNullOrBlank()
                    binding.errorText.text = state.errorMessage.orEmpty()
                    
                    adapter.submitList(state.movies)

                    if (binding.genreChips.childCount == 0 && state.genres.isNotEmpty()) {
                        setupGenreChips(state.genres)
                    }
                }
            }
        }
    }

    private fun setupGenreChips(genres: List<com.example.moviewatchlist.data.model.Genre>) {
        binding.genreChips.removeAllViews()
        
        val chipBackgroundColor = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                resources.getColor(R.color.platinum, null),
                resources.getColor(R.color.dark_charcoal, null)
            )
        )

        val chipTextColor = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                resources.getColor(R.color.deep_obsidian, null),
                resources.getColor(R.color.white, null)
            )
        )

        // "All" chip
        val allChip = Chip(requireContext()).apply {
            text = "All"
            isCheckable = true
            isChecked = true
            setChipBackgroundColor(chipBackgroundColor)
            setTextColor(chipTextColor)
            setOnClickListener { viewModel.selectGenre(null) }
        }
        binding.genreChips.addView(allChip)

        genres.forEach { genre ->
            val chip = Chip(requireContext()).apply {
                text = genre.name
                isCheckable = true
                setChipBackgroundColor(chipBackgroundColor)
                setTextColor(chipTextColor)
                setOnClickListener { viewModel.selectGenre(genre.id) }
            }
            binding.genreChips.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
