package nl.tudelft.trustchain.detoks

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.frostwire.jlibtorrent.TorrentHandle
import kotlinx.android.synthetic.main.fragment_strategy.*
import nl.tudelft.trustchain.common.ui.BaseFragment


class SeedingDebugFragment :  BaseFragment(R.layout.fragment_debug_seeding) {

    private lateinit var torrentManager: TorrentManager

    private lateinit var seedingDebugAdapter: SeedingDebugAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        torrentManager = TorrentManager.getInstance(requireActivity())
        seedingDebugAdapter = SeedingDebugAdapter(torrentManager.getListOfTorrents())

        val handler = Handler((Looper.getMainLooper()))
        val runnable : Runnable = object : Runnable {
            override fun run() {
                seedingDebugAdapter.updateView()
                handler.postDelayed(this, 2000)
            }
        }
        handler.postDelayed(runnable,2000)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val strategyRecycleView = view.findViewById<RecyclerView>(R.id.debugView)
        strategyRecycleView.adapter = seedingDebugAdapter
        strategyRecycleView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }
}

class SeedingDebugAdapter(private val strategyData: List<TorrentHandle>) : RecyclerView.Adapter<SeedingDebugAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val hashTextView: TextView
        val downloadTextView: TextView
        val uploadTextView: TextView
        val seederTextView: TextView
        val leecherTextView: TextView

        init {
            hashTextView = view.findViewById(R.id.hash)
            downloadTextView = view.findViewById(R.id.status)
            uploadTextView = view.findViewById(R.id.seeding)
            seederTextView = view.findViewById(R.id.nrSeeds)
            leecherTextView = view.findViewById(R.id.nrLeechers)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.debug_recycle_view, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val handler = strategyData[position]
        val status = handler.status()
        holder.hashTextView.text = strategyData[position].name()
        val bundle = Bundle()
        bundle.putString("torrent_name", holder.hashTextView.text.toString())

        holder.downloadTextView.text = status.state().name
        holder.uploadTextView.text = status.isSeeding.toString()
        holder.seederTextView.text = status.listSeeds().toString()
        holder.leecherTextView.text = status.listPeers().toString()
    }

    override fun getItemCount(): Int = strategyData.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateView() {
        this.notifyDataSetChanged()
    }
}
