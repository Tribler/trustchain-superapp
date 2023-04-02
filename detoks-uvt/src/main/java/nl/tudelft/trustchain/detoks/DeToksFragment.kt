package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.fragment_detoks.*
import mu.KotlinLogging
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.detoks.db.OwnedTokenManager
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
            val dir1 = File(mediaCacheDir)
            if (!dir1.exists()) {
                dir1.mkdirs()
            }
            val dir2 = File(torrentDir)
            if (!dir2.exists()) {
                dir2.mkdirs()
            }
            val file = File("$torrentDir/$DEFAULT_TORRENT_FILE")
            if (file.exists())  {
                file.delete()
            }
            addFile(torrentDir, BIGBUCKBUNNY, R.raw.big_buck_bunny)
            addFile(torrentDir, LAUNDROMAT, R.raw.cosmos_laundromat)
//            if (!file.exists()) {
//                val outputStream = FileOutputStream(file)
//                val ins = requireActivity().resources.openRawResource(R.raw.detoks)
//                outputStream.write(ins.readBytes())
//                ins.close()
//                outputStream.close()
//            }
        } catch (e: Exception) {
            Log.e("DeToks", "Failed to cache default torrent: $e")
        }
    }

    private fun makeSeedableTorrentDirAndPopulate(){
        try{
            val dir3 = File(seedableTorrentsDir)
            if (!dir3.exists()) {
                dir3.mkdirs()
            }
            // currently I only know how to add seedable torrents manually and one by one T_T
            // TODO: figure out how to add all seedable torrents all in one swoop and not one by one
            addFile(seedableTorrentsDir, DEFAULT_POST_VIDEO, R.raw.sintel)
            addFile(seedableTorrentsDir, DEFAULT_POST_VIDEO2, R.raw.tears_of_steel)
//            addFile(seedableTorrentsDir, DEFAULT_POST_VIDEO, R.raw.chicken_20230326_archive)
//            addFile(seedableTorrentsDir, DEFAULT_POST_VIDEO2, R.raw.file_20230326_archive)
//            addFile(seedableTorrentsDir, DEFAULT_POST_VIDEO3, R.raw.parrot_202303_archive)
//            addFile(seedableTorrentsDir, DEFAULT_POST_VIDEO, R.raw.cat)
//            addFile(seedableTorrentsDir, DEFAULT_POST_VIDEO2, R.raw.blueparrot)
//            addFile(seedableTorrentsDir, DEFAULT_POST_VIDEO3,R.raw.arcane)
            deleteFile(seedableTorrentsDir, "chicken_20230326_archive.torrent")
            deleteFile(seedableTorrentsDir, "file_20230326_archive.torrent")
            deleteFile(seedableTorrentsDir, "parrot_202303_archive.torrent")
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

        upvoteToken = UpvoteToken(-100, "", "", "", "") //TODO: make constructor with no parameters for initialisation
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
        const val DEFAULT_POST_VIDEO2 = "tears_of_steel.torrent"
//        const val DEFAULT_POST_VIDEO3 = "arcane.torrent"
//        const val DEFAULT_POST_VIDEO = "chicken_20230326_archive.torrent"
//        const val DEFAULT_POST_VIDEO2 = "file_20230326_archive.torrent"
//        const val DEFAULT_POST_VIDEO3 = "parrot_202303_archive.torrent"

    }
}
