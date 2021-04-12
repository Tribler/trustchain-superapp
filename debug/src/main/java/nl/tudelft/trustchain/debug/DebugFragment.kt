package nl.tudelft.trustchain.debug

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.debug.databinding.FragmentDebugBinding
import java.util.*


class DebugFragment : BaseFragment(R.layout.fragment_debug) {
    private val binding by viewBinding(FragmentDebugBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.debug_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.wanLog -> {
                findNavController().navigate(R.id.wanLogFragment)
                true
            }
            R.id.multiPunch -> {
                findNavController().navigate(R.id.punctureFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenStarted {
            while (isActive) {
                updateView()
                delay(1000)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateView() {
        val ipv8 = getIpv8()
        val demo = getDemoCommunity()
        binding.txtBootstrap.text = Community.DEFAULT_ADDRESSES.joinToString("\n")
        binding.txtLanAddress.text = demo.myEstimatedLan.toString()
        binding.txtWanAddress.text = demo.myEstimatedWan.toString()
        binding.txtPeerId.text = ipv8.myPeer.mid
        binding.txtPublicKey.text = ipv8.myPeer.publicKey.keyToBin().toHex()
        binding.txtOverlays.text = ipv8.overlays.values.toList().joinToString("\n") {
            it.javaClass.simpleName + " (" + it.getPeers().size + " peers)"
        }

        binding.txtOverlays.text = buildSpannedString {
            ipv8.overlays.values.forEachIndexed { index, overlay ->
                if (index > 0) append("\n")
                append(overlay.javaClass.simpleName)
                append(" (")
                val textColorResId =
                    if (overlay.getPeers().isNotEmpty()) R.color.green else R.color.red
                val textColor = ResourcesCompat.getColor(resources, textColorResId, null)
                inSpans(ForegroundColorSpan(textColor)) {
                    val peers = overlay.getPeers()
                    val peersCountStr = resources.getQuantityString(
                        R.plurals.x_peers, peers.size,
                        peers.size
                    )
                    append(peersCountStr)
                }
                append(")")
            }

            append("\n")
            bold {
                append("Total: ")
            }
            val totalPeersCount = ipv8.network.verifiedPeers.size
            val textColorResId = if (totalPeersCount > 0) R.color.green else R.color.red
            val textColor = ResourcesCompat.getColor(resources, textColorResId, null)
            inSpans(ForegroundColorSpan(textColor)) {
                append(
                    resources.getQuantityString(
                        R.plurals.x_peers,
                        totalPeersCount,
                        totalPeersCount
                    )
                )
            }
        }

        updateBootstrapList()

        lifecycleScope.launchWhenCreated {
            val blockCount = withContext(Dispatchers.IO) {
                getTrustChainCommunity().database.getBlockCount(null)
            }
            if (view != null) {
                binding.txtBlockCount.text = blockCount.toString()
            }
        }

        lifecycleScope.launchWhenCreated {
            val chainLength = withContext(Dispatchers.IO) {
                getTrustChainCommunity().getChainLength()
            }
            if (view != null) {
                binding.txtChainLength.text = chainLength.toString()
            }
        }

        binding.txtConnectionType.text = getDemoCommunity().network.wanLog
            .estimateConnectionType().value

        try {
            val pInfo: PackageInfo =
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            @Suppress("DEPRECATION")
            binding.txtAppVersion.text = pInfo.versionName + " (Build " + pInfo.versionCode + ")"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun updateBootstrapList() {
        val demo = getDemoCommunity()
        binding.bootstrapContainer.removeAllViews()
        Community.DEFAULT_ADDRESSES.forEach { address ->
            val lastResponse = demo.lastTrackerResponses[address]
            val isAlive = lastResponse != null && Date().time - lastResponse.time < 120_000
            val view = TextView(requireContext())
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            view.layoutParams = layoutParams
            view.text = address.toString()
            val resId = if (isAlive) R.drawable.indicator_online else
                R.drawable.indicator_offline
            val drawable = resources.getDrawable(resId, null)
            view.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
            view.compoundDrawablePadding =
                resources.getDimensionPixelSize(R.dimen.indicator_padding)
            view.typeface = Typeface.MONOSPACE
            view.setTextColor(Color.BLACK)
            binding.bootstrapContainer.addView(view)
        }
    }
}
