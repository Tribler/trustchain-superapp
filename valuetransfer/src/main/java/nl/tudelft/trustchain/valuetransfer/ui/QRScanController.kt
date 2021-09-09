package nl.tudelft.trustchain.valuetransfer.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.dialogs.*
import org.json.JSONObject

class QRScanController : BaseFragment() {
    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var peerChatCommunity: PeerChatCommunity
    private lateinit var attestationCommunity: AttestationCommunity
    private lateinit var contactStore: ContactStore

    fun initiateScan() {
        QRCodeUtils(requireContext()).startQRScanner(this, promptText = "Scan any QR Code to proceed", vertical = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentActivity = requireActivity() as ValueTransferMainActivity
        peerChatCommunity = parentActivity.getCommunity()!!
        attestationCommunity = parentActivity.getCommunity()!!
        contactStore = parentActivity.getStore()!!
    }

    private fun checkRequiredVariables(variables: List<String>, data: JSONObject): Boolean {
        variables.forEach { variable ->
            if (!data.has(variable)) {
                parentActivity.displaySnackbar(requireContext(), "Missing variable $variable", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                return false
            }
        }
        return true
    }

    fun addAuthority(publicKey: String) {
        Log.d("VTLOG", "ADD AUTHORITY")

        IdentityAttestationAuthorityDialog(defaultCryptoProvider.keyFromPublicBin(publicKey.hexToBytes())).show(parentFragmentManager, tag)
    }

    fun addAttestation(publicKey: String) {
        Log.d("VTLOG", "ADD ATTESTATION")

        val peer = attestationCommunity.getPeers().find { peer -> peer.publicKey.keyToBin().toHex() == publicKey }

        if (peer != null) {
            IdentityAttestationRequestDialog(peer).show(parentFragmentManager, tag)
        } else {
            parentActivity.displaySnackbar(requireContext(), "Peer could not be located in the network, please try again", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
        }
    }

    fun verifyAttestation(data: JSONObject) {
        Log.d("VTLOG", "VERIFY ATTESTATION")
        Log.d("VTLOG", data.toString())

        val variables = listOf("metadata", "attestationHash", "signature", "signee_key", "attestor_key")
        checkRequiredVariables(variables, data)

        val metadataVariables = listOf("attribute", "id_format")
        checkRequiredVariables(metadataVariables, JSONObject(data.getString("metadata")))

        val attesteeKey = data.getString("signee_key").hexToBytes() // Signee
        val attestationHash = data.getString("attestationHash").hexToBytes()
        val metadata = data.getString("metadata")
        val signature = data.getString("signature").hexToBytes()
        val authorityKey = data.getString("attestor_key").hexToBytes() // Attestor

        IdentityAttestationVerifyDialog(attesteeKey, attestationHash, metadata, signature, authorityKey).show(
            parentFragmentManager,
            this.tag
        )
    }

    fun addContact(data: JSONObject) {
        val variables = listOf("public_key")
        checkRequiredVariables(variables, data)

        Log.d("VTLOG", "ADD CONTACT")
        Log.d("VTLOG", data.toString())

        try {
            val publicKey = defaultCryptoProvider.keyFromPublicBin(data.optString("public_key").hexToBytes())
            val name = data.optString("name")

            ContactAddDialog(getTrustChainCommunity().myPeer.publicKey, publicKey, name).show(parentFragmentManager, tag)
        } catch (e: Exception) {
            e.printStackTrace()
            parentActivity.displaySnackbar(requireContext(), "Invalid public key in QR-code", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
        }
    }

    fun transferMoney(data: JSONObject) {
        val variables = listOf("public_key", "name", "amount")
        checkRequiredVariables(variables, data)

        Log.d("VTLOG", "TRANSFER MONEY")
        Log.d("VTLOG", data.toString())

        try {
            val publicKey = defaultCryptoProvider.keyFromPublicBin(data.optString("public_key").hexToBytes())

            if (publicKey == getTrustChainCommunity().myPeer.publicKey) {
                parentActivity.displaySnackbar(requireContext(), "Cannot transfer money to yourself", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                return
            }

            val amount = data.optString("amount")

            var contact = contactStore.getContactFromPublicKey(publicKey)
            if (contact == null) {
                contact = Contact(data.optString("name"), publicKey)
            }
            ExchangeTransferMoneyDialog(contact, amount, true).show(parentFragmentManager, tag)
        } catch (e: Exception) {
            e.printStackTrace()
            parentActivity.displaySnackbar(requireContext(), "Invalid public key in QR-code", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
        }
    }

    fun exchangeMoney(data: JSONObject, isCreation: Boolean) {
        val variables = when {
            isCreation -> listOf("payment_id", "public_key", "ip", "port", "name")
            else -> listOf("payment_id", "public_key", "ip", "port", "name", "amount")
        }

        checkRequiredVariables(variables, data)

        Log.d("VTLOG", "EXCHANGE MONEY")
        Log.d("VTLOG", data.toString())

        try {
            val publicKey = defaultCryptoProvider.keyFromPublicBin(data.optString("public_key").hexToBytes())
            val amount = if (isCreation) null else data.optLong("amount")

            ExchangeGatewayDialog(
                isCreation,
                publicKey,
                data.optString("payment_id"),
                data.optString("ip"),
                data.optInt("port"),
                data.optString("name"),
                amount
            ).show(parentFragmentManager, tag)
        } catch (e: Exception) {
            e.printStackTrace()
            parentActivity.displaySnackbar(requireContext(), "Invalid public key in QR-code", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        QRCodeUtils(requireContext()).parseActivityResult(requestCode, resultCode, data)?.let { result ->
            try {
                val obj = JSONObject(result)

                when {
                    obj.has("type") -> {
                        when (obj.optString("type")) {
                            "transfer" -> transferMoney(obj)
                            "creation" -> exchangeMoney(obj, true)
                            "destruction" -> exchangeMoney(obj, false)
                            "contact" -> addContact(obj)
                            else -> throw RuntimeException("Unrecognized type value ${obj.get("type")} in QR-code")
                        }
                    }
                    obj.has("presentation") -> {
                        when (obj.optString("presentation")) {
                            "attestation" -> verifyAttestation(obj)
                            else -> throw RuntimeException("Unrecognized presentation value ${obj.get("presentation")} in QR-code")
                        }
                    }
                    obj.has("public_key") -> {
                        try {
                            val publicKey = obj.optString("public_key")
                            defaultCryptoProvider.keyFromPublicBin(publicKey.hexToBytes())

                            PublicKeyScanOptionsDialog(obj).show(parentFragmentManager, tag)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            parentActivity.displaySnackbar(requireContext(), "Invalid public key in QR-code", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                        }
                    }
                    else -> throw RuntimeException("QR code not recognized")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                parentActivity.displaySnackbar(requireContext(), "Scanned QR code not in JSON format", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                initiateScan()
            }
        }
    }
}
