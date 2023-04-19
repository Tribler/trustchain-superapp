package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.fragment_detoks.*
import mu.KotlinLogging
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.detoks.db.OwnedTokenManager
import nl.tudelft.trustchain.detoks.recommendation.Recommender
import nl.tudelft.trustchain.detoks.token.ProposalToken
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import nl.tudelft.trustchain.detoks.trustchain.Balance
import java.io.File
import java.io.FileOutputStream

class DeToksFragment : BaseFragment(R.layout.fragment_detoks) {
    private lateinit var torrentManager: TorrentManager
    private lateinit var balance: Balance
    private lateinit var upvoteToken: UpvoteToken
    private lateinit var proposalToken: ProposalToken
    private val logger = KotlinLogging.logger {}
    private var previousVideoAdapterIndex = 0

    private val torrentDir: String
        get() = "${requireActivity().cacheDir.absolutePath}/torrent"
    private val mediaCacheDir: String
        get() = "${requireActivity().cacheDir.absolutePath}/media"
    private val seedableTorrentsDir: String
        get() = "${requireActivity().cacheDir.absolutePath}/seedableTorrents"

    private fun cacheDefaultTorrent() {
        try {
            val mediaCacheDirectory = File(mediaCacheDir)
            if (!mediaCacheDirectory.exists()) {
                mediaCacheDirectory.mkdirs()
            }
            val torrentDirectory = File(torrentDir)
            if (!torrentDirectory.exists()) {
                torrentDirectory.mkdirs()
            } else {
                torrentDirectory.delete()
            }
            val defaultTorrentFile = File("$torrentDir/$DEFAULT_TORRENT_FILE")
            if (defaultTorrentFile.exists())  {
                defaultTorrentFile.delete()
            }
            addFile(torrentDir, BIGBUCKBUNNY, R.raw.big_buck_bunny)
            addFile(torrentDir, DEFAULT_POST_VIDEO, R.raw.sintel)
        } catch (e: Exception) {
            Log.e("DeToks", "Failed to cache default torrent: $e")
        }
    }

    private fun makeSeedableTorrentDirAndPopulate(){
        try{
            val seedableTorrentsDirectory = File(seedableTorrentsDir)

            if (!seedableTorrentsDirectory.exists()) {
                seedableTorrentsDirectory.mkdirs()
            } else {
                seedableTorrentsDirectory.delete()
            }
            addFile(seedableTorrentsDir, BIGBUCKBUNNY, R.raw.big_buck_bunny)
            deleteFile(seedableTorrentsDir, DEFAULT_POST_VIDEO)
            deleteFile(seedableTorrentsDir, LAUNDROMAT)
        } catch (e: Exception) {
            Log.e("Detoks", "Failed to make a cache for seedable torrents")
        }
    }

    private fun addFile(dir: String, nameOfFileToAdd: String, fileRawInt: Int) {
        val file = File("$dir/$nameOfFileToAdd")
        if (!file.exists()) {
            val outputStream = FileOutputStream(file)
            val ins = requireActivity().resources.openRawResource(fileRawInt)
            outputStream.write(ins.readBytes())
            ins.close()
            outputStream.close()
        }
    }

    private fun deleteFile(dir: String, nameOfFileToAdd: String) {
        val file = File("$dir/$nameOfFileToAdd")
        if (file.exists()) {
            file.delete()
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cacheDefaultTorrent()
        makeSeedableTorrentDirAndPopulate()

        torrentManager = TorrentManager(
            File("${requireActivity().cacheDir.absolutePath}/media"),
            File("${requireActivity().cacheDir.absolutePath}/torrent"),
            File("${requireActivity().cacheDir.absolutePath}/seedableTorrents"),
            DEFAULT_CACHING_AMOUNT
        )
        Recommender.initialize()

        upvoteToken = UpvoteToken()
        proposalToken = ProposalToken()
        balance = Balance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPagerVideos.adapter = VideosAdapter(torrentManager, upvoteToken, proposalToken, balance)
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
        const val DEFAULT_CACHING_AMOUNT = 1
        const val DEFAULT_TORRENT_FILE = "detoks.torrent"
        const val LAUNDROMAT = "cosmos_laundromat.torrent"
        const val BIGBUCKBUNNY = "big_buck_bunny.torrent"
        const val DEFAULT_POST_VIDEO = "sintel.torrent"
    }
}
