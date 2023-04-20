package nl.tudelft.trustchain.detoks_engine

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.fragment_detoks2.*
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.detoks_engine.manage_tokens.TokenBenchmarkActivity
import nl.tudelft.trustchain.detoks_engine.manage_tokens.TokenManageActivity
import java.io.File
import java.io.FileOutputStream
import java.util.*

class DeToksEngineFragment : BaseFragment(R.layout.fragment_detoks2) {
    private lateinit var torrentManager: TorrentManager
    private lateinit var transactionCommunity: TransactionCommunity
    private val logger = KotlinLogging.logger {}
    private var previousVideoAdapterIndex = 0
    private val torrentDir: String
        get() = "${requireActivity().cacheDir.absolutePath}/torrent"
    private val mediaCacheDir: String
        get() = "${requireActivity().cacheDir.absolutePath}/media"

    private fun cacheDefaultTorrent() {
        try {
            val dir1 = File(mediaCacheDir)
            if (!dir1.exists()) {
                dir1.mkdirs()
            }
            val dir2 = File(torrentDir)
            if (!dir2.exists()) {
                dir2.mkdirs()
            }
            val file = File("$torrentDir/$DEFAULT_TORRENT_FILE")
            if (!file.exists()) {
                val outputStream = FileOutputStream(file)
                val ins = requireActivity().resources.openRawResource(R.raw.detoks)
                outputStream.write(ins.readBytes())
                ins.close()
                outputStream.close()
            }
        } catch (e: Exception) {
            Log.e("DeToks", "Failed to cache default torrent: $e")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cacheDefaultTorrent()

        torrentManager = TorrentManager(
            File("${requireActivity().cacheDir.absolutePath}/media"),
            File("${requireActivity().cacheDir.absolutePath}/torrent"),
            DEFAULT_CACHING_AMOUNT
        )
        transactionCommunity = IPv8Android.getInstance().getOverlay<TransactionCommunity>()!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPagerVideos.adapter = VideosAdapter(torrentManager)
        viewPagerVideos.currentItem = 0

        val button = view.findViewById<Button>(R.id.openbenchmark)
        button.setOnClickListener {
            startActivity(Intent(requireActivity(), TokenBenchmarkActivity::class.java))
        }

        val button2 = view.findViewById<Button>(R.id.opentk)
        button2.setOnClickListener {
            startActivity(Intent(requireActivity(), TokenManageActivity::class.java))
        }

        val textView = view.findViewById<TextView>(R.id.textView)
        transactionCommunity.setHandler {
                msg: String ->
            logger.debug("Detoks_engine", "handler in fragment")
            textView.text = msg
        }
        onPageChangeCallback()
    }

    /**
     * This functions allows for looping back to start of the video pool.
     */
    private fun onPageChangeCallback() {
        viewPagerVideos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)

                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    when (viewPagerVideos.currentItem - previousVideoAdapterIndex) {
                        1 -> torrentManager.notifyIncrease()
                        -1 -> torrentManager.notifyDecrease()
                        0 -> {} // Do nothing
                        else -> {
                            logger.error { "Something went wrong with the video adapter index" }
                        }
                    }
                    previousVideoAdapterIndex = viewPagerVideos.currentItem
                }
            }
        })
    }


    private fun broadcast() {
        lifecycleScope.launch {
            transactionCommunity.broadcastGreeting()
        }
    }

    companion object {
        const val DEFAULT_CACHING_AMOUNT = 2
        const val DEFAULT_TORRENT_FILE = "detoks.torrent"
    }
}
