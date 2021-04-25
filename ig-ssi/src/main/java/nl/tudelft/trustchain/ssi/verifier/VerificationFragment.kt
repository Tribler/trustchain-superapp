package nl.tudelft.trustchain.ssi.verifier

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.defaultEncodingUtils
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.dialogs.attestation.AttestationConfirmationDialog
import nl.tudelft.trustchain.ssi.dialogs.attestation.FireMissilesDialog
import nl.tudelft.trustchain.ssi.dialogs.authority.AuthorityConfirmationDialog
import nl.tudelft.trustchain.ssi.dialogs.misc.ScanIntentDialog
import org.json.JSONObject

const val REQUEST_ATTESTATION_INTENT = 0
const val ADD_AUTHORITY_INTENT = 1
const val SCAN_ATTESTATION_INTENT = 2

class VerificationFragment : BaseFragment(R.layout.fragment_verification) {

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    private val args: VerificationFragmentArgs by navArgs()
    private var intent = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.intent = args.intent
        qrCodeUtils.startQRScanner(this, args.qrCodeHint, vertical = true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val attestationPresentationString =
            qrCodeUtils.parseActivityResult(requestCode, resultCode, data)
        if (attestationPresentationString != null) {
            try {
                val attestationPresentation = JSONObject(
                    String(defaultEncodingUtils.decodeBase64FromString(attestationPresentationString))
                )
                Log.d("ig-ssi", "Found the following data: $attestationPresentation")
                when (val format = attestationPresentation.get("presentation")) {
                    "authority" -> {
                        handleAuthority(attestationPresentation)
                    }
                    "attestation" -> {
                        handleAttstation(attestationPresentation)
                    }
                    else -> throw RuntimeException("Encountered invalid presentation format $format.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Invalid data found", Toast.LENGTH_LONG).show()
                Log.d("ig-ssi", "STRING: ${attestationPresentationString}")
                Log.d(
                    "ig-ssi", "STRING: ${
                        String(defaultEncodingUtils.decodeBase64FromString(attestationPresentationString))
                    }"
                )
                qrCodeUtils.startQRScanner(this, args.qrCodeHint, vertical = true)
            }
        } else {
            Toast.makeText(requireContext(), "Scan cancelled", Toast.LENGTH_LONG).show()
            findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToDatabaseFragment())
        }
    }

    private fun handleAuthority(attestationPresentation: JSONObject) {
        val authorityKey =
            defaultCryptoProvider.keyFromPublicBin(
                defaultEncodingUtils.decodeBase64FromString(
                    attestationPresentation.getString("public_key")
                )
            )
        val rendezvousToken = attestationPresentation.optString("rendezvous")
        when (this.intent) {
            REQUEST_ATTESTATION_INTENT -> {
                GlobalScope.launch {
                    val channel = Communication.load(rendezvous = rendezvousToken)
                    var peer: Peer? = null
                    try {
                        withTimeout(30_000) {
                            while (peer == null) {
                                peer =
                                    channel.peers.find { it.publicKey == authorityKey }
                                delay(50L)
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        Toast.makeText(
                            requireContext(),
                            "Could not locate peer",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    FireMissilesDialog(
                        peer!!
                    ).show(parentFragmentManager, tag)
                }
                findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToDatabaseFragment())
            }
            ADD_AUTHORITY_INTENT -> AuthorityConfirmationDialog(authorityKey).show(
                parentFragmentManager,
                this.tag
            )
            else -> {
                ScanIntentDialog(authorityKey, rendezvousToken).show(
                    parentFragmentManager,
                    this.tag
                )
            }
        }
    }

    private fun handleAttstation(attestationPresentation: JSONObject) {
        val metadata = attestationPresentation.getString("metadata")
        val attestationHash =
            attestationPresentation.getString("attestationHash").hexToBytes()
        val signature =
            defaultEncodingUtils.decodeBase64FromString(attestationPresentation.getString("signature"))
        val subjectKey = defaultCryptoProvider.keyFromPublicBin(
            defaultEncodingUtils.decodeBase64FromString(
                attestationPresentation.getString(
                    "subject"
                )
            )
        )
        val attestorKey =
            attestationPresentation.getString("attestor").hexToBytes()
        val challenge = defaultEncodingUtils.decodeBase64FromString(attestationPresentation.getString("challenge"))
        val timestamp = attestationPresentation.getLong("timestamp")

        AttestationConfirmationDialog(
            challenge,
            timestamp,
            subjectKey,
            attestationHash,
            metadata,
            signature,
            attestorKey
        ).show(parentFragmentManager, this.tag)
    }
}
