package nl.tudelft.ipv8.android.demo.ui

import androidx.fragment.app.Fragment

import nl.tudelft.ipv8.android.demo.Ipv8Application

abstract class BaseFragment : Fragment() {
    protected val ipv8 by lazy {
        (requireContext().applicationContext as Ipv8Application).ipv8
    }
}
