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
import nl.tudelft.ipv8.attestation.identity.Metadata
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.decodeB64
import nl.tudelft.trustchain.ssi.dialogs.attestation.AttestationConfirmationDialog
import nl.tudelft.trustchain.ssi.dialogs.attestation.FireMissilesDialog
import nl.tudelft.trustchain.ssi.dialogs.authority.AuthorityConfirmationDialog
import nl.tudelft.trustchain.ssi.dialogs.misc.ScanIntentDialog
import org.json.JSONObject

const val REQUEST_ATTESTATION_INTENT = 0
const val ADD_AUTHORITY_INTENT = 1
const val SCAN_ATTESTATION_INTENT = 2

val REQUIRED_QR_FIELDS =
    arrayOf(
        "presentation",
        "attestationHash",
        "metadata",
        "subject",
        "challenge",
        "attestors",
        "value"
    )

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
                val attestationPresentation = JSONObject(attestationPresentationString)
                Log.d("ig-ssi", "Found the following data: $attestationPresentation")
                when (val format = attestationPresentation.get("presentation")) {
                    "authority" -> {
                        handleAuthority(attestationPresentation)
                    }
                    "attestation" -> {
                        handleAttestation(attestationPresentation)
                    }
                    else -> throw RuntimeException("Encountered invalid presentation format $format.")
                }
                // qrCodeUtils.startQRScanner(this, args.qrCodeHint, vertical = true)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Invalid data found", Toast.LENGTH_LONG).show()
                Log.d("ig-ssi", "STRING: $attestationPresentationString")
                // Log.d(
                //     "ig-ssi", "STRING: ${
                //         String(decodeB64(attestationPresentationString))
                //     }"
                // )
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
                decodeB64(
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

    private fun handleAttestation(data: JSONObject) {
        val challengeJSON = data.getJSONObject("challenge")
        val timestamp = challengeJSON.getLong("timestamp")
        val presentations = VerificationHelper.getInstance().presentation

        if (!presentations.containsKey(timestamp)) {
            presentations[timestamp] = data
        }
        data.keys().forEach {
            presentations[timestamp]!!.put(it, data.get(it))
        }

        // if (this.presentations[timestamp]!!.asMap().filterKeys { it !in REQUIRED_QR_FIELDS }
        //         .isNotEmpty())

        if (REQUIRED_QR_FIELDS.any { !presentations[timestamp]!!.has(it) }) {
            Toast.makeText(requireContext(), "Please scan the next QR Code.", Toast.LENGTH_LONG)
                .show()
            return
        } else {
            @Suppress("NAME_SHADOWING") val data = presentations.remove(timestamp)!!
            // Attestation hash
            val attestationHash = decodeB64(data.getString("attestationHash"))

            // Metadata
            val mdJSON = data.getJSONObject("metadata")
            val mdPointer = decodeB64(mdJSON.getString("pointer"))
            val mdSign = decodeB64(mdJSON.getString("signature"))
            val mdMetadata = mdJSON.getString("metadata").toByteArray()
            val metadata = Metadata.fromDatabaseTuple(mdPointer, mdSign, mdMetadata)

            // Subject
            val subjectKey = defaultCryptoProvider.keyFromPublicBin(
                decodeB64(
                    data.getString(
                        "subject"
                    )
                )
            )

            // Challenge
            @Suppress("NAME_SHADOWING") val challengeJSON = data.getJSONObject("challenge")
            val challengePair = Pair(
                decodeB64(challengeJSON.getString("signature")),
                challengeJSON.getLong("timestamp")
            )

            // Attestors
            val attestors = mutableListOf<Pair<ByteArray, ByteArray>>()
            val attestorsArray = data.getJSONArray("attestors")
            for (i in 0 until attestorsArray.length()) {
                val entry = attestorsArray.getJSONObject(i)
                val keyHash = decodeB64(entry.getString("keyHash"))
                val signature = decodeB64(entry.getString("signature"))
                attestors.add(Pair(keyHash, signature))
            }

            val attributeName =
                JSONObject(String(metadata.serializedMetadata)).getString("name")
            val proposedAttributeValue = decodeB64(data.optString("value", ""))

            AttestationConfirmationDialog(
                attestationHash,
                attributeName,
                proposedAttributeValue,
                metadata,
                subjectKey,
                challengePair,
                attestors
            ).show(parentFragmentManager, this.tag)
        }
    }
}
