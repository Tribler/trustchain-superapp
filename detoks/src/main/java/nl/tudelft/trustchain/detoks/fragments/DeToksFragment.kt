package nl.tudelft.trustchain.detoks.fragments

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.navigation.findNavController
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.fragment_detoks.*
import kotlinx.android.synthetic.main.item_video.*
import kotlinx.android.synthetic.main.item_video.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.detoks.DeToksCommunity
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.TorrentManager
import nl.tudelft.trustchain.detoks.VideosAdapter
import java.io.File
import java.io.FileOutputStream

class DeToksFragment : BaseFragment(R.layout.fragment_detoks) {
    private lateinit var networkSize: TextView
    private lateinit var torrentManager: TorrentManager
    private lateinit var adapter: VideosAdapter
    private val logger = KotlinLogging.logger {}
    private var previousVideoAdapterIndex = 0

    private val torrentDir: String
        get() = "${requireActivity().cacheDir.absolutePath}/torrent"
    private val mediaCacheDir: String
        get() = "${requireActivity().cacheDir.absolutePath}/media"

    private fun updateNetworkSize(size: Int) {
        try {
            networkSize.text = "Network: " + size.toString()
        } catch (_: NumberFormatException) {
            Log.d("DeToks", "Could not update the network size.")
        }
    }

    fun update() {
        val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
        updateNetworkSize(community.getPeers().size)

        if (this::adapter.isInitialized) {
            val position = viewPagerVideos.currentItem

            CoroutineScope(Dispatchers.Main).launch {
                val content = adapter.mVideoItems[position].content(position, 10000)

                try {
                    if (viewPagerVideos != null) viewPagerVideos.like_count.text = community.getLikes(content.fileName, content.torrentName).size.toString()
                } catch (_: NumberFormatException) {
                    Log.d("DeToks", "Could not update the like counter.")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        update()
    }

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
    @Suppress()
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cacheDefaultTorrent()
        torrentManager = TorrentManager(
            File("${requireActivity().cacheDir.absolutePath}/media"),
            File("${requireActivity().cacheDir.absolutePath}/torrent"),
            DEFAULT_CACHING_AMOUNT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = VideosAdapter(torrentManager)
        viewPagerVideos.adapter = adapter
        viewPagerVideos.setCurrentItem(lastIndex, false)
        onPageChangeCallback()

        networkSize = view.findViewById(R.id.networkSize)
        networkSize.setOnClickListener {
            lastIndex = viewPagerVideos.currentItem
            it.findNavController().navigate(TabBarFragmentDirections.actionTabBarFragmentToNetworkFragment())
        }
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

                    val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
                    updateNetworkSize(community.getPeers().size)
                }
            }
        })
    }

    companion object {
        const val DEFAULT_CACHING_AMOUNT = 2
        const val DEFAULT_TORRENT_FILE = "detoks.torrent"
        lateinit var torrentManager: TorrentManager
        var lastIndex: Int = 0
    }
}
