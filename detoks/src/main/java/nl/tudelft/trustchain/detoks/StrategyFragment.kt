package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import nl.tudelft.trustchain.common.ui.BaseFragment

class StrategyFragment :  BaseFragment(R.layout.fragment_upload) {

    private lateinit var torrentManager: TorrentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        torrentManager = TorrentManager.getInstance(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val leachingStrategySpinner = requireView().findViewById<Spinner>(R.id.leachingStrategy)
        val seedingStrategySpinner = requireView().findViewById<Spinner>(R.id.seedingStrategy)
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.strategies_array,
            R.layout.support_simple_spinner_dropdown_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        leachingStrategySpinner.adapter = adapter
        seedingStrategySpinner.adapter = adapter

        setSpinnerActions(leachingStrategySpinner){
            torrentManager.strategies.changeStrategy(it)
        }

        setSpinnerActions(seedingStrategySpinner){
            torrentManager.strategies.changeStrategy(it)
        }
    }

    private fun setSpinnerActions(spinner: Spinner, callback: (Int) -> Unit) {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                callback(p2)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                callback(0)
            }
        }

    }
}
