package nl.tudelft.trustchain.detoks.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.navigation.findNavController
import nl.tudelft.trustchain.detoks.R

class DiscoveryFragment : Fragment() {
    private lateinit var backButton: ImageButton
    private lateinit var reloadButton: ImageButton

    private fun update() {
//        TODO: When the functionality of this fragment is added, implement the update method here.
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backButton = view.findViewById(R.id.backButton)
        reloadButton = view.findViewById(R.id.reloadButton)

        backButton.setOnClickListener {
            it.findNavController().navigate(DiscoveryFragmentDirections.actionDiscoveryFragmentToTabBarFragment())
        }

        reloadButton.setOnClickListener {
            update()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_discovery, container, false)
    }
}
