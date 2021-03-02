package nl.tudelft.trustchain.ssi

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import com.jaredrummler.blockingdialog.BlockingDialogManager
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.WalletAttestation
import nl.tudelft.ipv8.attestation.schema.*
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.ssi.dialogs.AttestationValueDialog
import org.json.JSONObject

class SSIMainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val community = IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
        community.setAttestationRequestCallback(::attestationRequestCallback)
        community.setAttestationRequestCompleteCallback(::attestationRequestCompleteCallbackWrapper)
        community.setAttestationChunkCallback(::attestationChunkCallback)

        // Register own key as trusted authority.
        community.trustedAuthorityManager.addTrustedAuthority(IPv8Android.getInstance().myPeer.publicKey)
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

    // Default callback, currently overwritten in DatabaseFragment.
    private fun attestationRequestCompleteCallbackWrapper(
        forPeer: Peer,
        attributeName: String,
        attestation: WalletAttestation,
        attributeHash: ByteArray,
        idFormat: String,
        fromPeer: Peer?,
        metaData: String?,
        signature: ByteArray?
    ) {
        attestationRequestCompleteCallback(
            forPeer,
            attributeName,
            attestation,
            attributeHash,
            idFormat,
            fromPeer,
            metaData,
            signature,
            applicationContext
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun attestationRequestCallback(
        peer: Peer,
        attributeName: String,
        metadata: String
    ): ByteArray {
        Log.i("ig-ssi", "Attestation: called")
        val parsedMetadata = JSONObject(metadata)
        val idFormat = parsedMetadata.optString("id_format", ID_METADATA)
        val input =
            BlockingDialogManager.getInstance()
                .showAndWait<String?>(this, AttestationValueDialog(attributeName, idFormat))
                ?: throw RuntimeException("User cancelled dialog.")
        Log.i("ig-ssi", "Signing attestation with value $input with format $idFormat.")
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                applicationContext,
                "Signing attestation for $attributeName for peer ${peer.mid} ...",
                Toast.LENGTH_LONG
            )
                .show()
        }
        return when (idFormat) {
            "id_metadata_range_18plus" -> byteArrayOf(input.toByte())
            else -> input.toByteArray()
        }
    }

    override val navigationGraph = R.navigation.nav_graph_ssi
    override val bottomNavigationMenu = R.menu.bottom_navigation_menu2
}

@Suppress("UNUSED_PARAMETER")
fun attestationRequestCompleteCallback(
    forPeer: Peer,
    attributeName: String,
    attestation: WalletAttestation,
    attributeHash: ByteArray,
    idFormat: String,
    fromPeer: Peer?,
    metaData: String?,
    signature: ByteArray?,
    context: Context
) {
    if (fromPeer == null) {
        Log.i(
            "ig-ssi",
            "Signed attestation for attribute $attributeName for peer ${forPeer.mid}."
        )
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "Successfully sent attestation for $attributeName to peer ${forPeer.mid}",
                Toast.LENGTH_LONG
            )
                .show()
        }
    } else {
        Log.i(
            "ig-ssi",
            "Received attestation for attribute $attributeName with metadata: $metaData."
        )
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "Received Attestation for $attributeName",
                Toast.LENGTH_LONG
            )
                .show()
        }
    }
}
