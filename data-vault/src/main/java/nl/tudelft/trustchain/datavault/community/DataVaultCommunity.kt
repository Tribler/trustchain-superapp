package nl.tudelft.trustchain.datavault.community

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import mu.KotlinLogging
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.wallet.AttestationBlob
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.trustchain.datavault.DataVaultMainActivity
import nl.tudelft.trustchain.datavault.accesscontrol.AccessPolicy
import nl.tudelft.trustchain.datavault.ui.VaultBrowserFragment
import nl.tudelft.trustchain.peerchat.community.AttachmentPayload
import java.io.File
import java.io.FileOutputStream

private val logger = KotlinLogging.logger {}

class DataVaultCommunity(private val context: Context) : Community() {
    override val serviceId = "3f0dd0ab2c774be3bd1b7bf2d8dddc2938dd18fa"

    private val logTag = "DataVaultCommunity"

    private lateinit var dataVaultMainActivity: DataVaultMainActivity
    private lateinit var vaultBrowserFragment: VaultBrowserFragment
    val attestationCommunity: AttestationCommunity by lazy {
        IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
    }

    init {
        messageHandlers[MessageId.FILE] = ::onFilePacket
        messageHandlers[MessageId.FILE_REQUEST] = ::onFileRequestPacket
        messageHandlers[MessageId.FILE_REQUEST_FAILED] = ::onFileRequestFailedPacket
    }

    private fun onFilePacket(packet: Packet) {
        val (_, payload) = packet.getDecryptedAuthPayload(
            AttachmentPayload.Deserializer, myPeer.key as PrivateKey
        )
        logger.debug { "<- $payload" }
        onFile(payload)
    }

    private fun onFile(payload: AttachmentPayload) {
        val message = String(payload.data)
        Log.e(logTag, "Received file: $message")
        notify(message)
        val (file, _) = vaultFile(payload.id)
        Log.e(logTag, "Received file: ${file.absolutePath}")
        // Save attachment
        val fos = FileOutputStream(file)
        fos.write(payload.data)
        fos.close()

    }

    private fun onFileRequestPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(VaultFileRequestPayload.Deserializer)
        logger.debug { "<- $payload" }
        onFileRequest(peer, payload)
    }

    private fun onFileRequestFailedPacket(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(VaultFileRequestFailedPayload.Deserializer)
        logger.debug { "<- $payload" }
        onFileRequestFailed(payload)
    }

    private fun onFileRequestFailed(payload: VaultFileRequestFailedPayload) {
        Log.e(logTag, payload.message)
        //Toast.makeText(context, payload.message, Toast.LENGTH_LONG).show()
        notify(payload.message)
    }

    private fun vaultFile(filename: String): Pair<File, AccessPolicy> {
        val VAULT = File(context.filesDir, VaultBrowserFragment.VAULT_DIR)
        return Pair(File(VAULT, filename), AccessPolicy(attestationCommunity))
    }

    private fun onFileRequest(peer: Peer, payload: VaultFileRequestPayload) {
        Log.e(logTag, "Received file request")
        try {
            val (file, accessPolicy) = vaultFile(payload.id)
            if (!file.exists()) {
                Log.e(logTag, "The requested file does not exist")
                sendFileRequestFailed(peer, payload.id, "The requested file does not exist")
            } else if (!accessPolicy.verifyAttestations(peer, payload.attestations)) {
                Log.e(logTag, "Access Policy not met")
                //sendFileRequestFailed(peer, payload.id, "Access Policy not met")
                sendFile(peer, payload.id, file)
            } else {
                sendFile(peer, payload.id, file)
                logger.debug { "$peer.mid" }
            }
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        }
    }

    fun sendFile(peer: Peer, id: String, file: File) {
        Log.e(logTag, "Sending file")
        val payload = AttachmentPayload(id, file.readBytes())
        val packet =
            serializePacket(MessageId.FILE, payload, encrypt = true, recipient = peer)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

    fun sendFileRequest(peer: Peer, id: String, attestations: List<AttestationBlob>) {
        Log.e(logTag, "Sending file request")
        Log.e(logTag, "including ${attestations.size} attestation(s)")
        val payload = VaultFileRequestPayload(id, listOf())
        val packet = serializePacket(MessageId.FILE_REQUEST, payload)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

    private fun sendFileRequestFailed(peer: Peer, id: String, message: String) {
        Log.e(logTag, "Sending file request failed")
        val payload = VaultFileRequestFailedPayload("Vault file request $id: $message")
        val packet = serializePacket(MessageId.FILE_REQUEST_FAILED, payload)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

    private fun notify(message: String) {
        if (!::vaultBrowserFragment.isInitialized) {
            return
        }
        vaultBrowserFragment.notify(message)
    }

    fun setDataVaultActivity(activity: DataVaultMainActivity) {
        dataVaultMainActivity = activity
    }

    fun setVaultBrowserFragment(fragment: VaultBrowserFragment) {
        vaultBrowserFragment = fragment
    }

    object MessageId {
        const val FILE = 1
        const val FILE_REQUEST = 2
        const val FILE_REQUEST_FAILED = 3
    }

    class Factory(
        private val context: Context,
    ) : Overlay.Factory<DataVaultCommunity>(DataVaultCommunity::class.java) {
        override fun create(): DataVaultCommunity {
            return DataVaultCommunity(context)
        }
    }
}
