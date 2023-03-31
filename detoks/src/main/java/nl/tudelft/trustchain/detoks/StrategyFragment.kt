package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.core.os.HandlerCompat.postDelayed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.frostwire.jlibtorrent.TorrentHandle
import nl.tudelft.trustchain.common.ui.BaseFragment

class StrategyFragment :  BaseFragment(R.layout.fragment_upload) {

    private lateinit var torrentManager: TorrentManager

    private lateinit var strategyRecyclerViewAdapter: StrategyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        torrentManager = TorrentManager.getInstance(requireActivity())
        strategyRecyclerViewAdapter = StrategyAdapter(torrentManager.getListOfTorrents())

        val handler = Handler((Looper.getMainLooper()))
        val runnable : Runnable = object : Runnable {
            override fun run() {
                strategyRecyclerViewAdapter.updateView()
                handler.postDelayed(this, 2000)
            }
        }
        handler.postDelayed(runnable,2000)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val strategyRecycleView = view.findViewById<RecyclerView>(R.id.strategyBalanceView)
        strategyRecycleView.adapter = strategyRecyclerViewAdapter
        strategyRecycleView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val arrayAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.strategies_array,
            R.layout.support_simple_spinner_dropdown_item
        )
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val leachingStrategySpinner = view.findViewById<Spinner>(R.id.leachingStrategy)
        leachingStrategySpinner.adapter = arrayAdapter
        setSpinnerActions(leachingStrategySpinner) {
            torrentManager.updateLeachingStrategy(it)
        }

        val seedingStrategySpinner = view.findViewById<Spinner>(R.id.seedingStrategy)
        seedingStrategySpinner.adapter = arrayAdapter
        setSpinnerActions(seedingStrategySpinner) {
            torrentManager.updateSeedingStrategy(it)
            strategyRecyclerViewAdapter.updateView()
        }
        seedingStrategySpinner.isEnabled = false
        view.findViewById<EditText>(R.id.seedingLimit).isEnabled = false

        val seedingSwitch = view.findViewById<SwitchCompat>(R.id.isSeeding)
        seedingSwitch.setOnCheckedChangeListener { _, p1 ->
            seedingStrategySpinner.isEnabled = p1
            view.findViewById<EditText>(R.id.seedingLimit).isEnabled = p1
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

class StrategyAdapter(private val strategyData: List<TorrentHandle>) : RecyclerView.Adapter<StrategyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val hashTextView: TextView
        val downloadTextView: TextView
        val uploadTextView: TextView
        val balanceTextView: TextView

        init {
            hashTextView = view.findViewById(R.id.hash)
            downloadTextView = view.findViewById(R.id.download)
            uploadTextView = view.findViewById(R.id.upload)
            balanceTextView = view.findViewById(R.id.balance)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.strategy_recycle_view, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.hashTextView.text = strategyData[position].infoHash().toString()
        holder.downloadTextView.text = (strategyData[position].status().totalPayloadDownload() / 1000).toString()
        holder.uploadTextView.text = (strategyData[position].status().totalPayloadUpload() / 1000).toString()

        //TODO: replace by actual token balance
        holder.balanceTextView.text = ((strategyData[position].status().totalPayloadUpload() - strategyData[position].status().totalPayloadDownload()) / 100000).toString()
    }

    override fun getItemCount(): Int = strategyData.size

    fun updateView() {
        //TODO: change to more efficient change
        Log.i(DeToksCommunity.LOGGING_TAG, "Updating view, download: ${strategyData[0].status().totalPayloadDownload()}")
        Log.i(DeToksCommunity.LOGGING_TAG, "Updating view, upload: ${strategyData[0].status().totalPayloadUpload()}")

        this.notifyDataSetChanged()
    }

}
