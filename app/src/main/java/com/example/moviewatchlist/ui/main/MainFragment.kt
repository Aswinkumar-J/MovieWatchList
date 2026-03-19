package com.example.moviewatchlist.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.moviewatchlist.data.model.WatchStatus
import com.example.moviewatchlist.databinding.FragmentMainBinding
import com.google.android.material.tabs.TabLayoutMediator

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding: FragmentMainBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        val tabs = listOf(
            MovieTab(title = "All", statusFilter = null),
            MovieTab(title = "Plan to Watch", statusFilter = WatchStatus.PLAN_TO_WATCH),
            MovieTab(title = "Currently Watching", statusFilter = WatchStatus.CURRENTLY_WATCHING),
            MovieTab(title = "Completed", statusFilter = WatchStatus.COMPLETED),
            MovieTab(title = "Dropped", statusFilter = WatchStatus.DROPPED),
        )

        val pagerAdapter = MainPagerAdapter(this, tabs)
        binding.viewPager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabs[position].title
        }.attach()

        binding.fabAddMovie.setOnClickListener {
            // Create mode: movieId uses default -1 in the destination
            findNavController().navigate(
                MainFragmentDirections.actionMainFragmentToMovieDetailFragment(),
            )
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class MovieTab(
    val title: String,
    val statusFilter: WatchStatus?,
)

