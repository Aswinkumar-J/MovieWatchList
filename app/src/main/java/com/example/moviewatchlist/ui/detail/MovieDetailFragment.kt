package com.example.moviewatchlist.ui.detail

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.moviewatchlist.R
import com.example.moviewatchlist.data.model.WatchStatus
import com.example.moviewatchlist.databinding.FragmentMovieDetailBinding
import com.example.moviewatchlist.di.ServiceLocator
import kotlinx.coroutines.launch

class MovieDetailFragment : Fragment() {
    private var _binding: FragmentMovieDetailBinding? = null
    private val binding: FragmentMovieDetailBinding
        get() = _binding!!

    private val args: MovieDetailFragmentArgs by navArgs()

    private val viewModel: MovieDetailViewModel by lazy {
        val repo = ServiceLocator.provideMovieRepository(requireContext())
        ViewModelProvider(
            this,
            MovieDetailViewModel.Factory(repo, args.movieId),
        )[MovieDetailViewModel::class.java]
    }

    private var lastPosterUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMovieDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val statusValues = WatchStatus.entries.toTypedArray()
        val statusLabels = statusValues.map { statusToLabel(it) }
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            statusLabels,
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.statusSpinner.adapter = statusAdapter

        binding.statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                viewModel.setStatus(statusValues[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.titleInput.addTextChangedListener(simpleWatcher { viewModel.setTitle(it) })
        binding.runtimeInput.addTextChangedListener(simpleWatcher { text ->
            val runtime = text.toIntOrNull()
            viewModel.setRuntimeMinutes(runtime)
        })

        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            viewModel.setRating(rating)
        }

        binding.reviewInput.addTextChangedListener(simpleWatcher { viewModel.setReview(it) })
        binding.synopsisInput.addTextChangedListener(simpleWatcher { viewModel.setSynopsis(it) })

        binding.saveButton.setOnClickListener {
            viewModel.save {
                findNavController().popBackStack()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.isVisible = !state.isLoaded

                    if (lastPosterUrl != state.posterUrl) {
                        lastPosterUrl = state.posterUrl
                        Glide.with(binding.poster)
                            .load(state.posterUrl)
                            .placeholder(R.drawable.poster_placeholder)
                            .into(binding.poster)
                    }

                    // Avoid overwriting user typing continuously; only push values when first loaded.
                    if (state.isLoaded && binding.titleInput.text?.toString() != state.title) {
                        binding.titleInput.setText(state.title)
                    }

                    val selectedIndex = statusValues.indexOf(state.status).coerceAtLeast(0)
                    if (binding.statusSpinner.selectedItemPosition != selectedIndex) {
                        binding.statusSpinner.setSelection(selectedIndex)
                    }

                    if (binding.ratingBar.rating != state.rating) {
                        binding.ratingBar.rating = state.rating
                    }

                    val runtimeText = state.runtimeMinutes?.toString().orEmpty()
                    if (binding.runtimeInput.text?.toString() != runtimeText) {
                        binding.runtimeInput.setText(runtimeText)
                    }

                    val showSynopsis = state.status == WatchStatus.PLAN_TO_WATCH
                    binding.synopsisLayout.isVisible = showSynopsis
                    binding.reviewLayout.isVisible = !showSynopsis

                    val reviewText = state.review.orEmpty()
                    if (binding.reviewInput.text?.toString() != reviewText) {
                        binding.reviewInput.setText(reviewText)
                    }

                    val synopsisText = state.synopsis.orEmpty()
                    if (binding.synopsisInput.text?.toString() != synopsisText) {
                        binding.synopsisInput.setText(synopsisText)
                    }

                    binding.saveButton.isEnabled = !state.isSaving && state.title.isNotBlank()

                    val titleRes = if (viewModel.isCreateMode) R.string.add_movie else R.string.edit_movie
                    binding.screenTitle.text = getString(titleRes)
                }
            }
        }
    }

    private fun statusToLabel(status: WatchStatus): String =
        when (status) {
            WatchStatus.PLAN_TO_WATCH -> "Plan to Watch"
            WatchStatus.CURRENTLY_WATCHING -> "Currently Watching"
            WatchStatus.COMPLETED -> "Completed"
            WatchStatus.DROPPED -> "Dropped"
        }

    private fun simpleWatcher(onChanged: (String) -> Unit): TextWatcher =
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                onChanged(s?.toString().orEmpty())
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

