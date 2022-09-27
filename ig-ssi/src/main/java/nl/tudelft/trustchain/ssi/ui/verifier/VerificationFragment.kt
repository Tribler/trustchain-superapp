package nl.tudelft.trustchain.ssi.ui.verifier

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.*
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.identity.consts.Metadata.ID
import nl.tudelft.ipv8.attestation.identity.consts.Metadata.NAME
import nl.tudelft.ipv8.attestation.identity.consts.Metadata.SCHEMA
import nl.tudelft.ipv8.attestation.identity.consts.Metadata.VALUE
import nl.tudelft.ipv8.attestation.identity.datastructures.Metadata
import nl.tudelft.ipv8.attestation.wallet.consts.Metadata.PUBLIC_KEY
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.attestations.Metadata.ATTESTATION
import nl.tudelft.trustchain.ssi.attestations.Metadata.ATTESTATION_HASH
import nl.tudelft.trustchain.ssi.attestations.Metadata.ATTESTORS
import nl.tudelft.trustchain.ssi.attestations.Metadata.AUTHORITY
import nl.tudelft.trustchain.ssi.attestations.Metadata.CHALLENGE
import nl.tudelft.trustchain.ssi.attestations.Metadata.KEY_HASH
import nl.tudelft.trustchain.ssi.attestations.Metadata.METADATA
import nl.tudelft.trustchain.ssi.attestations.Metadata.POINTER
import nl.tudelft.trustchain.ssi.attestations.Metadata.PRESENTATION
import nl.tudelft.trustchain.ssi.attestations.Metadata.PRESENTATION_REQUEST
import nl.tudelft.trustchain.ssi.attestations.Metadata.RENDEZVOUS
import nl.tudelft.trustchain.ssi.attestations.Metadata.SIGNATURE
import nl.tudelft.trustchain.ssi.attestations.Metadata.SUBJECT
import nl.tudelft.trustchain.ssi.attestations.Metadata.TIMESTAMP
import nl.tudelft.trustchain.ssi.ui.dialogs.attestation.AttestationVerificationDialog
import nl.tudelft.trustchain.ssi.ui.dialogs.attestation.RequestAttestationDialog
import nl.tudelft.trustchain.ssi.ui.dialogs.authority.AuthorityConfirmationDialog
import nl.tudelft.trustchain.ssi.ui.dialogs.misc.ScanIntentDialog
import nl.tudelft.trustchain.ssi.util.decodeB64
import org.json.JSONObject

const val REQUEST_ATTESTATION_INTENT = 0
const val ADD_AUTHORITY_INTENT = 1
const val SCAN_ATTESTATION_INTENT = 2

val REQUIRED_QR_FIELDS =
    arrayOf(
        PRESENTATION,
        ATTESTATION_HASH,
        METADATA,
        SUBJECT,
        CHALLENGE,
        ATTESTORS,
        VALUE
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
                when (val format = attestationPresentation.get(PRESENTATION)) {
                    AUTHORITY -> {
                        handleAuthority(attestationPresentation)
                    }
                    ATTESTATION -> {
                        handleAttestation(attestationPresentation)
                    }
                    PRESENTATION_REQUEST -> {
                        handlePresentationRequest(attestationPresentation)
                    }
                    else -> throw RuntimeException("Encountered invalid presentation format $format.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Invalid data found", Toast.LENGTH_LONG).show()
                Log.d("ig-ssi", "STRING: $attestationPresentationString")
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
                val dialog = RequestAttestationDialog()
                dialog.show(parentFragmentManager, "ig-ssi")
                GlobalScope.launch {
                    val channel =
                        Communication.load(rendezvous = rendezvousToken)
                    try {
                        var peer: Peer? = null
                        withTimeout(30_000) {
                            while (peer == null) {
                                peer =
                                    channel.peers.find { it1 ->
                                        it1.publicKey.keyToHash()
                                            .contentEquals(authorityKey.keyToHash())
                                    }
                                delay(100)
                            }
                        }
                        dialog.setPeer(peer!!)
                    } catch (e: TimeoutCancellationException) {
                        dialog.cancel()
                    }
                }
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

    private fun handlePresentationRequest(attestationPresentation: JSONObject) {
        val authorityKey =
            defaultCryptoProvider.keyFromPublicBin(
                decodeB64(
                    attestationPresentation.getString(PUBLIC_KEY)
                )
            )

        val rendezvousToken: String? = attestationPresentation.optString(RENDEZVOUS)
        val id = attestationPresentation.getString(ID)
        val requestedAttribute = attestationPresentation.getString(NAME)

        AlertDialog.Builder(context)
            .setTitle("Attribute Disclosure Request")
            .setMessage(
                "Authority ${
                    authorityKey.keyToHash().toHex()
                } has requested your attribute $requestedAttribute"
            )
            .setPositiveButton(
                "17039379"
            ) { _, _ ->
                val channel = Communication.load(rendezvous = rendezvousToken)
                val result = channel.getAttributeByName(requestedAttribute)
                if (result != null) {
                    val (hash, credential, value) = result
                    GlobalScope.launch {
                        var peer: Peer? = null
                        try {
                            withTimeout(30_000) {
                                while (peer == null) {
                                    peer =
                                        channel.peers.find { it1 ->
                                            it1.publicKey.keyToHash()
                                                .contentEquals(authorityKey.keyToHash())
                                        }
                                    delay(100)
                                }
                            }
                            channel.presentAttestation(
                                peer!!,
                                id,
                                hash,
                                value,
                                credential
                            )
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    requireContext(),
                                    "Successfully sent attestation",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: TimeoutCancellationException) {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to find client.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            requireContext(),
                            "Attribute missing.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("17039369", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun handleAttestation(data: JSONObject) {
        val challengeJSON = data.getJSONObject(CHALLENGE)
        val timestamp = challengeJSON.getLong(TIMESTAMP)
        val rendezvousToken = data.optString(RENDEZVOUS)
        val presentations = VerificationHelper.getInstance().presentation

        if (!presentations.containsKey(timestamp)) {
            presentations[timestamp] = data
        }
        data.keys().forEach {
            presentations[timestamp]!!.put(it, data.get(it))
        }

        if (REQUIRED_QR_FIELDS.any { !presentations[timestamp]!!.has(it) }) {
            Toast.makeText(requireContext(), "Please scan the next QR Code.", Toast.LENGTH_LONG)
                .show()
            return
        } else {
            @Suppress("NAME_SHADOWING") val data = presentations.remove(timestamp)!!
            // Attestation hash
            val attestationHash = decodeB64(data.getString(ATTESTATION_HASH))

            // Metadata
            val mdJSON = data.getJSONObject(METADATA)
            val mdPointer = decodeB64(mdJSON.getString(POINTER))
            val mdSign = decodeB64(mdJSON.getString(SIGNATURE))
            val mdMetadata = mdJSON.getString(METADATA).toByteArray()
            val metadata = Metadata.fromDatabaseTuple(mdPointer, mdSign, mdMetadata)

            // Subject
            val subjectKey = defaultCryptoProvider.keyFromPublicBin(
                decodeB64(
                    data.getString(
                        SUBJECT
                    )
                )
            )

            // Challenge
            @Suppress("NAME_SHADOWING") val challengeJSON = data.getJSONObject(CHALLENGE)
            val challengePair = Pair(
                decodeB64(challengeJSON.getString(SIGNATURE)),
                challengeJSON.getLong(TIMESTAMP)
            )

            // Attestors
            val attestors = mutableListOf<Pair<ByteArray, ByteArray>>()
            val attestorsArray = data.getJSONArray(ATTESTORS)
            for (i in 0 until attestorsArray.length()) {
                val entry = attestorsArray.getJSONObject(i)
                val keyHash = decodeB64(entry.getString(KEY_HASH))
                val signature = decodeB64(entry.getString(SIGNATURE))
                attestors.add(Pair(keyHash, signature))
            }

            val attMetadata = JSONObject(String(metadata.serializedMetadata))
            val attributeName = attMetadata.getString(NAME)
            val idFormat = attMetadata.getString(SCHEMA)

            var proposedAttributeValue: ByteArray? = null
            if (data.has(VALUE)) {
                proposedAttributeValue = decodeB64(data.getString(VALUE))
            }

            AttestationVerificationDialog(
                attestationHash,
                attributeName,
                proposedAttributeValue,
                idFormat,
                metadata,
                subjectKey,
                challengePair,
                attestors,
                rendezvousToken
            ).show(parentFragmentManager, this.tag)
        }
    }
}
