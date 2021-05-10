package nl.tudelft.trustchain.ssi

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.common.BaseActivity

class SSIMainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_ssi
    override val bottomNavigationMenu = R.menu.bottom_navigation_menu2

    private lateinit var verificationFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val channel = Communication.load()

        // TODO: add callbacks for user update.
        // community.setAttestationRequestCallback(::attestationRequestCallback)
        // community.setAttestationRequestCompleteCallback(::attestationRequestCompleteCallbackWrapper)
        // community.setAttestationChunkCallback(::attestationChunkCallback)

        // Register own key as trusted authority.
        channel.attestationOverlay.authorityManager.addTrustedAuthority(channel.myPeer.publicKey)
        this.notificationHandler()
    }

    private fun notificationHandler() {
        Handler(Looper.getMainLooper()).post {
            lifecycleScope.launchWhenCreated {
                while (isActive) {
                    val channel = Communication.load()

                    val notificationAmount =
                        channel.verifyRequests.size + channel.attestationRequests.size
                    val notificationBadge = bottomNavigation.getOrCreateBadge(R.id.requestsFragment)
                    notificationBadge.number = notificationAmount
                    notificationBadge.isVisible = notificationBadge.number > 0

                    delay(100)
                }
            }
        }
    }

    private fun attestationChunkCallback(peer: Peer, i: Int) {
        Log.i("ig-ssi", "Received attestation chunk $i from ${peer.mid}.")
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                applicationContext,
                "Received attestation chunk $i from ${peer.mid}.",
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }
}
