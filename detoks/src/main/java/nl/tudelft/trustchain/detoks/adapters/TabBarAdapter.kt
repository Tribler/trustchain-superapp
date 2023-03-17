package nl.tudelft.trustchain.detoks.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class TabBarAdapter(fragment: Fragment, fragments: List<Fragment>) : FragmentStateAdapter(fragment) {
    private val fragments = fragments

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}
