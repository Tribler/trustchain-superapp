package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.fragment_detoks.*
import mu.KotlinLogging
import nl.tudelft.trustchain.common.ui.BaseFragment
import java.io.File
import java.io.FileOutputStream

class DeToksFragment : BaseFragment(R.layout.fragment_detoks) {
    private lateinit var torrentManager: TorrentManager
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPagerVideos.adapter = VideosAdapter(torrentManager)
        viewPagerVideos.currentItem = 0
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

    companion object {
        const val DEFAULT_CACHING_AMOUNT = 2
        const val DEFAULT_TORRENT_FILE = "detoks.torrent"
    }
}
