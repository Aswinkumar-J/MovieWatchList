package com.example.moviewatchlist.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moviewatchlist.data.model.Movie
import com.example.moviewatchlist.data.model.WatchStatus
import com.example.moviewatchlist.databinding.ItemMovieBinding
import com.example.moviewatchlist.R

class MovieAdapter(
    private val onMovieClick: (movieId: Long) -> Unit,
) : ListAdapter<Movie, MovieAdapter.MovieViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieViewHolder(binding, onMovieClick)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MovieViewHolder(
        private val binding: ItemMovieBinding,
        private val onMovieClick: (movieId: Long) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: Movie) {
            Glide.with(binding.poster)
                .load(movie.posterUrl)
                .placeholder(R.drawable.poster_placeholder)
                .into(binding.poster)

            binding.title.text = movie.title
            binding.runtime.text = movie.runtimeMinutes?.let { "${it} min" } ?: "—"
            binding.ratingBar.rating = movie.rating
            binding.ratingBar.visibility = if (movie.rating > 0f) View.VISIBLE else View.GONE

            val showSynopsis = movie.status == WatchStatus.PLAN_TO_WATCH
            binding.synopsisOrReview.text = if (showSynopsis) {
                movie.synopsis ?: "No synopsis"
            } else {
                movie.review ?: "No review"
            }

            binding.root.setOnClickListener { onMovieClick(movie.id) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}

