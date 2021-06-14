package nl.tudelft.trustchain.ssi.attestations

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.wallet.cryptography.WalletAttestation


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
