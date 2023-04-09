package nl.tudelft.trustchain.detoks

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.frostwire.jlibtorrent.TorrentHandle
import kotlinx.android.synthetic.main.fragment_strategy.*
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.detoks.TorrentManager.TorrentHandler

class StrategyFragment :  BaseFragment(R.layout.fragment_strategy) {

    private lateinit var torrentManager: TorrentManager

    private lateinit var strategyRecyclerViewAdapter: StrategyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        torrentManager = TorrentManager.getInstance(requireActivity())
        strategyRecyclerViewAdapter = StrategyAdapter(torrentManager.seedingTorrents)

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

        val leechingStrategySpinner = view.findViewById<Spinner>(R.id.leechingStrategy)
        leechingStrategySpinner.adapter = arrayAdapter
        leechingStrategySpinner.post {
            leechingStrategySpinner.setSelection(torrentManager.strategies.leechingStrategy)
        }
        setSpinnerActions(leechingStrategySpinner) {
            if (it != torrentManager.strategies.leechingStrategy) {
                return@setSpinnerActions
            }
            torrentManager.updateLeechingStrategy(it)
        }

        val seedingSwitch = view.findViewById<SwitchCompat>(R.id.isSeeding)
        seedingSwitch.isChecked = torrentManager.strategies.isSeeding

        val seedingStrategySpinner = view.findViewById<Spinner>(R.id.seedingStrategy)
        seedingStrategySpinner.isEnabled = torrentManager.strategies.isSeeding
        seedingStrategySpinner.adapter = arrayAdapter
        seedingStrategySpinner.post {
            seedingStrategySpinner.setSelection(torrentManager.strategies.seedingStrategy)
        }
        setSpinnerActions(seedingStrategySpinner) {
            if (it != torrentManager.strategies.seedingStrategy) {
                return@setSpinnerActions
            }
            torrentManager.updateSeedingStrategy(strategyId = it)
            strategyRecyclerViewAdapter.updateView()
        }

        val seedingBandwidthLimit = view.findViewById<EditText>(R.id.seedingLimit)
        seedingBandwidthLimit.isEnabled = torrentManager.strategies.isSeeding
        seedingBandwidthLimit.setText((torrentManager.strategies.seedingBandwidthLimit).toString())
        setEditTextOnEnter(seedingBandwidthLimit) {
            if (seedingBandwidthLimit.text.toString() != "") {
                val newBandwidthLimit = seedingBandwidthLimit.text.toString().toInt()
                torrentManager.strategies.seedingBandwidthLimit = newBandwidthLimit
                torrentManager.setUploadRateLimit((newBandwidthLimit  * 1000000L / 86400).toInt())
            }
        }

        val storageLimit = view.findViewById<EditText>(R.id.storageLimit)
        storageLimit.isEnabled = torrentManager.strategies.isSeeding
        storageLimit.setText(torrentManager.strategies.storageLimit.toString())
        setEditTextOnEnter(storageLimit) {
            if (storageLimit.text.toString() != "") {
                val newStorageLimit = storageLimit.text.toString().toInt()
                torrentManager.updateSeedingStrategy(storageLimit = newStorageLimit)
            }
        }

        seedingSwitch.setOnCheckedChangeListener { _, p1 ->
            torrentManager.strategies.isSeeding = p1
            seedingStrategySpinner.isEnabled = p1
            seedingBandwidthLimit.isEnabled = p1
            storageLimit.isEnabled = p1

            if (p1) {
                torrentManager.updateSeedingStrategy(isSeeding = true)
            } else {
                torrentManager.stopSeeding()
            }
        }
    }

    private fun setEditTextOnEnter(txt: EditText, callback: () -> Unit) {
        txt.setOnKeyListener { _, i, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_ENTER) {
                callback()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
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

class StrategyAdapter(private val strategyData: List<TorrentHandler>) : RecyclerView.Adapter<StrategyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val hashTextView: TextView
        val downloadTextView: TextView
        val uploadTextView: TextView
        val balanceTextView: TextView

        init {
            hashTextView = view.findViewById(R.id.name)
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
        val handler = strategyData[position]
        val convBtoMB = 1000000
        val status = handler.handle.status()

        holder.hashTextView.text = strategyData[position].handle.name()

        holder.downloadTextView.text = (status.allTimeDownload()
            / convBtoMB).toString()

        holder.uploadTextView.text = (status.allTimeUpload()
            / convBtoMB).toString()

        //TODO: replace by actual token balance
        holder.balanceTextView.text = (
            (status.allTimeUpload() - status.allTimeDownload())
            / convBtoMB).toString()
    }

    override fun getItemCount(): Int = strategyData.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateView() {
        this.notifyDataSetChanged()
    }
}
