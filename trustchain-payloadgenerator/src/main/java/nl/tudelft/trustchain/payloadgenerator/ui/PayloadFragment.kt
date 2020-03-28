package nl.tudelft.trustchain.payloadgenerator.ui

import nl.tudelft.trustchain.common.ui.BaseFragment
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nl.tudelft.trustchain.payloadgenerator.R
import nl.tudelft.trustchain.payloadgenerator.R.layout.activity_main
import nl.tudelft.trustchain.payloadgenerator.R.layout.fragment_payload
import nl.tudelft.trustchain.payloadgenerator.ui.dummy.DummyContent
import nl.tudelft.trustchain.payloadgenerator.ui.dummy.DummyContent.DummyItem

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [PayloadFragment.OnListFragmentInteractionListener] interface.
 */



class PayloadFragment : BaseFragment() {
    private var isSending = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payload, container, false)
    }

    @ExperimentalUnsignedTypes
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}
