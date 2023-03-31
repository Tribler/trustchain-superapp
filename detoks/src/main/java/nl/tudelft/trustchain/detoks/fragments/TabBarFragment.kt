package nl.tudelft.trustchain.detoks.fragments

import android.Manifest
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.adapters.TabBarAdapter

class TabBarFragment : Fragment() {
    private lateinit var viewPager: ViewPager2

    @RequiresApi(TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)
        viewPager.isUserInputEnabled = false
        viewPager.adapter = TabBarAdapter(this, listOf(DeToksFragment(), Fragment(), ProfileFragment()))

        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                DeToksFragment.torrentManager.createTorrentInfo(uri, requireContext())
                Toast.makeText(requireContext(), "Successfully uploaded.", Toast.LENGTH_LONG).show()
            }
        }

        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getContent.launch("video/*")
            } else {
                Toast.makeText(this.requireContext(), "Permission has been denied!", Toast.LENGTH_LONG).show()
                Log.i("AndroidRuntime", "Rejected :(")
            }
        }

        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 1) {
                    val previousTabIndex = viewPager.currentItem

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

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tab_bar, container, false)
    }
}
