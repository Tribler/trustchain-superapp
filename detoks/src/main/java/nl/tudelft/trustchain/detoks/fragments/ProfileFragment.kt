package nl.tudelft.trustchain.detoks.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.adapters.TabBarAdapter
import kotlin.random.Random

class ProfileFragment : Fragment() {
    private lateinit var viewPager: ViewPager2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        TODO: Set the correct values of the the following three text views
//        val publicKeyLabel = view.findViewById<TextView>(R.id.publicKeyLabel)
//        publicKeyLabel.text = "" // The current user's public key

//        val numVideosLabel = view.findViewById<TextView>(R.id.numVideosLabel)
//        numVideosLabel.text = "Test 2" // The current user's total number of uploaded videos

//        val numLikesLabel = view.findViewById<TextView>(R.id.numLikesLabel)
//        numLikesLabel.text = "Test 3" // The current user's total number of received likes

//        TODO: Get the actual number of likes for every video uploaded by the current user.
//        TODO: Store the number of likes per video in a list of pairs of string (the ID of a video) and int (the number of likes for that video).
//        TODO: (optionally, if possible) Sort the list by the date and time when the video was uploaded.
        val likesPerVideo = (100 .. 199).map { x -> Pair("Video $x", Random.nextInt(0, 1_000)) }
        val videosListFragment = VideosListFragment(likesPerVideo)

//        TODO: Get the actual list of videos liked by the current user and store their IDs.
//        TODO: (optionally, if possible) Sort the list by the date and time when the video was liked.
        val likedVideos = (200 .. 299).map { x -> "Video $x" }
        val likedListFragment = LikedListFragment(likedVideos)

//        TODO: Get the actual list of notifications received by the current user.
//        TODO: (optionally, if possible) Sort the list by the date and time when the notification was received.
        val notifications = (1 .. 50).map { x -> "Notification $x" }
        val notificationsListFragment = NotificationsListFragment(notifications)

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
