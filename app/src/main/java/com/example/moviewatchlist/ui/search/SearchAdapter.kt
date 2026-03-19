package com.example.moviewatchlist.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moviewatchlist.R
import com.example.moviewatchlist.data.model.Movie
import com.example.moviewatchlist.databinding.ItemSearchMovieBinding
import java.util.Locale

class SearchAdapter(
    private val onMovieClick: (movie: Movie) -> Unit,
) : ListAdapter<Movie, SearchAdapter.SearchViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemSearchMovieBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return SearchViewHolder(binding, onMovieClick)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SearchViewHolder(
        private val binding: ItemSearchMovieBinding,
        private val onMovieClick: (movie: Movie) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: Movie) {
            binding.title.text = movie.title

            val vote = movie.tmdbVoteAverage
            binding.voteAverage.text = vote?.let { String.format(Locale.US, "%.1f", it) } ?: "—"

            Glide.with(binding.poster)
                .load(movie.posterUrl)
                .placeholder(R.drawable.poster_placeholder)
                .into(binding.poster)

            binding.root.setOnClickListener { onMovieClick(movie) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return if (oldItem.id != 0L && newItem.id != 0L) {
                oldItem.id == newItem.id
            } else if (oldItem.tmdbId != null && newItem.tmdbId != null) {
                oldItem.tmdbId == newItem.tmdbId
            } else {
                oldItem == newItem
            }
        }
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}

