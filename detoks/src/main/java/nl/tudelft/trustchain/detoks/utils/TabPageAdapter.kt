package nl.tudelft.trustchain.detoks.utils

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import nl.tudelft.trustchain.detoks.fragments.DeToksFragment
import nl.tudelft.trustchain.detoks.fragments.ProfileFragment

class TabPageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private val fragments = listOf(DeToksFragment(), ProfileFragment())

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}
