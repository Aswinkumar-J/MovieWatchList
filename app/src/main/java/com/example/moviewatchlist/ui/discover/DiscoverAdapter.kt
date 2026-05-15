package com.example.moviewatchlist.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moviewatchlist.R
import com.example.moviewatchlist.data.model.Movie
import com.example.moviewatchlist.databinding.ItemDiscoverMovieBinding

class DiscoverAdapter(
    private val onMovieClick: (Movie) -> Unit,
) : ListAdapter<Movie, DiscoverAdapter.DiscoverViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoverViewHolder {
        val binding = ItemDiscoverMovieBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return DiscoverViewHolder(binding, onMovieClick)
    }

    override fun onBindViewHolder(holder: DiscoverViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiscoverViewHolder(
        private val binding: ItemDiscoverMovieBinding,
        private val onMovieClick: (Movie) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: Movie) {
            binding.title.text = movie.title
            Glide.with(binding.poster)
                .load(movie.posterUrl)
                .placeholder(R.drawable.poster_placeholder)
                .into(binding.poster)
            binding.root.setOnClickListener { onMovieClick(movie) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie) = oldItem.tmdbId == newItem.tmdbId
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie) = oldItem == newItem
    }
}
