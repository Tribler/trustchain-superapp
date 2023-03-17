package nl.tudelft.trustchain.detoks.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.DetoksCommunity
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.adapters.TabBarAdapter
import kotlin.random.Random

class ProfileFragment : Fragment() {
    private lateinit var viewPager: ViewPager2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val community = IPv8Android.getInstance().getOverlay<DetoksCommunity>()!!

        val author = community.myPeer.publicKey.toString()
        val videos = community.getPostedVideos(author)
//        TODO: Set the correct values of the the following three text views
        val publicKeyLabel = view.findViewById<TextView>(R.id.publicKeyLabel)
        publicKeyLabel.text = author // The current user's public key

        val numVideosLabel = view.findViewById<TextView>(R.id.numVideosLabel)
        numVideosLabel.text = videos.size.toString() // The current user's total number of uploaded videos

        val numLikesLabel = view.findViewById<TextView>(R.id.numLikesLabel)
        numLikesLabel.text = videos.sumOf { it.second }.toString() // The current user's total number of received likes

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
}
