package nl.tudelft.trustchain.detoks.fragments

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.adapters.TabBarAdapter
import kotlin.system.exitProcess

class TabBarFragment : Fragment() {
    private val HOME_INDEX = 0
    private val UPLOAD_VIDEO_INDEX = 1
    private val PROFILE_INDEX = 2

    private lateinit var viewPager: ViewPager2
    private lateinit var detoksFragment: DeToksFragment
    private lateinit var profileFragment: ProfileFragment

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detoksFragment = DeToksFragment()
        profileFragment = ProfileFragment()

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.getTabAt(UPLOAD_VIDEO_INDEX)?.icon?.setTint(Color.RED)

        viewPager = view.findViewById(R.id.viewPager)
        viewPager.isUserInputEnabled = false
        viewPager.adapter = TabBarAdapter(this, listOf(detoksFragment, Fragment(), profileFragment))

        // Request Android content selection for video
        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                // Video selected -> seed it
                detoksFragment.torrentManager.createTorrentInfo(uri, requireContext())
                Toast.makeText(requireContext(), "Successfully uploaded.", Toast.LENGTH_LONG).show()

                detoksFragment.refresh()
            }
        }
        // On the fly request for permissions
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Get videos only
                getContent.launch("video/*")
            } else {
                Toast.makeText(this.requireContext(), "Permission has been denied!", Toast.LENGTH_LONG).show()
                Log.i("AndroidRuntime", "Rejected :(")
            }
        }

        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tabLayout.getTabAt(UPLOAD_VIDEO_INDEX)?.icon?.setTint(Color.RED)

                if (tab.position == UPLOAD_VIDEO_INDEX) {
                    val previousTabIndex = viewPager.currentItem

                    // Different permission depending on version :/
                    if (Build.VERSION.SDK_INT > 32) {
                        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }

                    tabLayout.selectTab(tabLayout.getTabAt(previousTabIndex))
                    return
                }

                viewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tabLayout.getTabAt(UPLOAD_VIDEO_INDEX)?.icon?.setTint(Color.RED)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                tabLayout.getTabAt(UPLOAD_VIDEO_INDEX)?.icon?.setTint(Color.RED)

                if (tab.position == HOME_INDEX) {
                    detoksFragment.refresh()
                } else if (tab.position == PROFILE_INDEX) {
                    profileFragment.update()
                }
            }
        })

        requireActivity().onBackPressedDispatcher.addCallback {
            exitProcess(0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tab_bar, container, false)
    }
}
