package com.example.moviewatchlist.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.moviewatchlist.R
import com.example.moviewatchlist.data.model.WatchStatus
import com.example.moviewatchlist.databinding.FragmentMainBinding
import com.example.moviewatchlist.di.ServiceLocator
import com.example.moviewatchlist.ui.auth.LoginActivity
import android.content.Intent
import com.google.android.material.tabs.TabLayoutMediator
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch



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

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    findNavController().navigate(
                        MainFragmentDirections.actionMainFragmentToSearchFragment(),
                    )
                    true
                }
                R.id.action_logout -> {
                    ServiceLocator.provideAuthRepository().signOut()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false

            }
        }

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(
                MainFragmentDirections.actionMainFragmentToDiscoverFragment(),
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                ServiceLocator.provideMovieRepository(requireContext()).isSyncing.collect { isSyncing ->
                    binding.syncProgress.visibility = if (isSyncing) View.VISIBLE else View.GONE
                }
            }
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

