package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.google.common.reflect.Reflection.getPackageName
import kotlinx.android.synthetic.main.fragment_detoks.*
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentDetoksBinding
import java.util.*


class DeToksFragment : BaseFragment(R.layout.fragment_detoks) {
    private val binding by viewBinding(FragmentDetoksBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val videoItems = mutableListOf<VideoItem>()
        val item = VideoItem()
        item.videoURL = videoFileNameToPath("video")
        item.videoTitle = "Taryn Elliott"
        item.videoDesc = "Man Dancing In The Street (2019)"
        videoItems.add(item)

        val item2 = VideoItem()
        item2.videoURL = videoFileNameToPath("video1")
        item2.videoTitle = "SwissHumanity Stories"
        item2.videoDesc = "Train Transporting Public Above The Mountains Alps Of Switzerland (2020)"
        videoItems.add(item2)

        val item3 = VideoItem()
        item3.videoURL = videoFileNameToPath("video2")
        item3.videoTitle = "alenta azwild"
        item3.videoDesc = "A Bullet Train Traveling Fast Above The Rail Tracks (2019)"
        videoItems.add(item3)

        viewPagerVideos.adapter = VideosAdapter(videoItems)
        viewPagerVideos.currentItem = 1
        onInfinitePageChangeCallback(videoItems.size + 2)
    }

    private fun videoFileNameToPath(fileName: String): String {
        return "android.resource://${activity?.packageName}/raw/$fileName"
    }

    private fun onInfinitePageChangeCallback(listSize: Int) {
        viewPagerVideos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)

                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    when (viewPagerVideos.currentItem) {
                        listSize - 1 -> viewPagerVideos.setCurrentItem(1, false)
                        0 -> viewPagerVideos.setCurrentItem(listSize - 2, false)
                    }
                }
            }
        })
    }


}
