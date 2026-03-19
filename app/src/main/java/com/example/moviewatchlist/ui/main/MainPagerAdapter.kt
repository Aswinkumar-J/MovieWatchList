package com.example.moviewatchlist.ui.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.moviewatchlist.ui.list.MovieListFragment

class MainPagerAdapter(
    fragment: Fragment,
    private val tabs: List<MovieTab>,
) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        val tab = tabs[position]
        return MovieListFragment.newInstance(tab.statusFilter)
    }
}

