package nl.tudelft.trustchain.detoks.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.DetoksCommunity
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.adapters.TabBarAdapter

class ProfileFragment : Fragment() {
    private lateinit var numVideosLabel: TextView
    private lateinit var numLikesLabel: TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var upload: Button

    private fun updatePersonalInformation(videos: List<Pair<String, Int>>) {
        numVideosLabel.text = videos.size.toString()
        numLikesLabel.text = videos.sumOf { it.second }.toString()
    }

    override fun onResume() {
        super.onResume()

        val community = IPv8Android.getInstance().getOverlay<DetoksCommunity>()!!
        val videos = community.getPostedVideos(community.myPeer.publicKey.toString())

        updatePersonalInformation(videos)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val community = IPv8Android.getInstance().getOverlay<DetoksCommunity>()!!
        val author = community.myPeer.publicKey.toString()
        val videos = community.getPostedVideos(author)

        val publicKeyLabel = view.findViewById<TextView>(R.id.publicKeyLabel)
        publicKeyLabel.text = author

        numVideosLabel = view.findViewById(R.id.numVideosLabel)
        numLikesLabel = view.findViewById(R.id.numLikesLabel)

        updatePersonalInformation(videos)

//        TODO: (optionally, if possible) Sort the list by the date and time when the video was uploaded.
        val videosListFragment = VideosListFragment(videos)

//        TODO: (optionally, if possible) Sort the list by the date and time when the video was liked.
        val likedListFragment = LikedListFragment(
            community.listOfLikedVideosAndTorrents(author).map { it.first + ", " + it.second }
        )

//        TODO: (optionally, if possible) Sort the list by the date and time when the notification was received.
        val notificationsListFragment = NotificationsListFragment(
            community.getBlocksByAuthor(author).map {
                "Received like for video: " + it.transaction["video"]
            }
        )

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)
        viewPager.adapter = TabBarAdapter(this, listOf(videosListFragment, likedListFragment, notificationsListFragment))

        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })

        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun mainPart(){
//        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
//
//            DeToksFragment.SingleTM.torrentManager.createTorrentInfo(uri!!, this.requireContext())
//        }
//        getContent.launch("video/*")
//    }
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onRequestPermissionsResult(requestCode: Int,
//                                            permissions: Array<String>, grantResults: IntArray) {
//        when (requestCode) {
//            111 -> {
//
//                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                    Log.d("AndroidRuntime", "REJECTED :(")
//
//                } else {
//                    mainPart()
//                }
//            }
//        }
//    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
}
