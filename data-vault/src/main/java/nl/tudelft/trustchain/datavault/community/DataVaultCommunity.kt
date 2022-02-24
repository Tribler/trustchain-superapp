package nl.tudelft.trustchain.datavault.community

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.wallet.AttestationBlob
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.eva.EVACommunity
import nl.tudelft.ipv8.messaging.eva.TransferException
import nl.tudelft.ipv8.messaging.eva.TransferProgress
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.datavault.DataVaultMainActivity
import nl.tudelft.trustchain.datavault.accesscontrol.AccessControlList
import nl.tudelft.trustchain.datavault.ui.ImageViewHolder
import nl.tudelft.trustchain.datavault.ui.PeerVaultFileItem
import nl.tudelft.trustchain.datavault.ui.VaultBrowserFragment
import nl.tudelft.trustchain.peerchat.community.AttachmentPayload
import java.io.File

private val logger = KotlinLogging.logger {}

class DataVaultCommunity(private val context: Context) : EVACommunity() {
    override val serviceId = "3f0dd0ab2c774be3bd1b7bf2d8dddc2938dd18fa"

    private val logTag = "DataVaultCommunity"

    private lateinit var dataVaultMainActivity: DataVaultMainActivity
    private lateinit var vaultBrowserFragment: VaultBrowserFragment
    val attestationCommunity: AttestationCommunity by lazy {
        IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
    }

    val pendingImageViewHolders = mutableMapOf<String, Pair<PeerVaultFileItem, ImageViewHolder>>()

    fun addPendingImageViewHolders(peerVaultFileItem: PeerVaultFileItem, imageViewHolder: ImageViewHolder) {
        pendingImageViewHolders[peerVaultFileItem.cacheKey] = Pair(peerVaultFileItem, imageViewHolder)
    }

    val VAULT by lazy { File(context.filesDir, VaultBrowserFragment.VAULT_DIR) }

    init {
        messageHandlers[MessageId.FILE] = ::onFilePacket
        messageHandlers[MessageId.FILE_REQUEST] = ::onFileRequestPacket
        messageHandlers[MessageId.FILE_REQUEST_FAILED] = ::onFileRequestFailedPacket
        messageHandlers[MessageId.ACCESSIBLE_FILES_REQUEST] = ::onAccessibleFilesRequestPacket
        messageHandlers[MessageId.ACCESSIBLE_FILES] = ::onAccessibleFilesPacket

        evaProtocolEnabled = true
    }

    override fun load() {
        super.load()

        // evaProtocol must be instantiated for DataVaultCommunity.
        // Make sure evaProtocolEnabled is set to true in init{}
        evaProtocol!!.setOnEVASendCompleteCallback(::onEVASendCompleteCallback)
        evaProtocol!!.setOnEVAReceiveProgressCallback(::onEVAReceiveProgressCallback)
        evaProtocol!!.setOnEVAReceiveCompleteCallback(::onEVAReceiveCompleteCallback)
        evaProtocol!!.setOnEVAErrorCallback(::onEVAErrorCallback)
    }

    /**
     * EVA Callbacks
     */
    private fun onEVASendCompleteCallback(peer: Peer, info: String, nonce: ULong) {
        Log.e(simpleName, "ON EVA send complete callback for '$info'")

        if (!isDataVaultEVA(info)) return

        if (isEvaSendCompleteCallbackInitialized) {
            this.evaSendCompleteCallback(peer, info, nonce)
        }
    }

    private fun onEVAReceiveProgressCallback(peer: Peer, info: String, progress: TransferProgress) {
        //Log.e(simpleName, "ON EVA receive progress callback for '$info'. Progress: ${progress.progress}")

        if (!isDataVaultEVA(info)) return

        if (isEvaReceiveProgressCallbackInitialized) {
            this.evaReceiveProgressCallback(peer, info, progress)
        }
    }

    private fun onEVAReceiveCompleteCallback(peer: Peer, info: String, id: String, data: ByteArray?) {
        //Log.e(simpleName, "ON EVA receive complete callback for '$info'")

        if (!isDataVaultEVA(info)) return

        /*data?.let {
            val packet = Packet(peer.address, it)
            //onAttachmentPacket(packet)
            Log.e(simpleName, packet.source.toString())
        }*/

        if (isEvaReceiveCompleteCallbackInitialized) {
            this.evaReceiveCompleteCallback(peer, info, id, data)
        }
    }

    private fun onEVAErrorCallback(peer: Peer, exception: TransferException) {
        Log.e(simpleName, "ON EVA error callback for '${exception.info}'")

        if (!isDataVaultEVA(exception.info)) return

        if (isEvaErrorCallbackInitialized) {
            this.evaErrorCallback(peer, exception)
        }
    }

    private fun isDataVaultEVA(info: String): Boolean {
        return listOf(EVAId.EVA_DATA_VAULT_FILE, ).contains(info)
    }

    ///////////////////////////////////

    private fun onFilePacket(packet: Packet) {
        val (peer, payload) = packet.getDecryptedAuthPayload(
            AttachmentPayload.Deserializer, myPeer.key as PrivateKey
        )
        logger.debug { "<- $payload" }
        onFile(peer, payload)
    }

    private fun onAccessibleFilesPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(AccessibleFilesPayload.Deserializer)
        logger.debug { "<- $payload" }
        onAccessibleFiles(peer, payload)
    }

    private fun onFile(peer: Peer, payload: AttachmentPayload) {
        Log.e(logTag, "File received ${payload.id}")
        onFile(peer, payload.id, payload.data)
    }

    fun onFile(peer: Peer, id: String, data: ByteArray) {
        val peerFileId = "${peer.mid}_$id"
        val pair = pendingImageViewHolders[peerFileId]

        if (pair != null) {
            Log.e(logTag, "pending view holder and vault item found ($peerFileId)")
            val peerVaultFileItem = pair.first
            pendingImageViewHolders.remove(peerFileId)
            val cacheFile = peerVaultFileItem.writeDataCacheFile(context, data)
            CoroutineScope(Dispatchers.Main).launch {
                val imageViewHolder = pair.second
                imageViewHolder.setRemoteFileImage(peerVaultFileItem, cacheFile)
            }
        }
    }

    private fun onAccessibleFiles(peer: Peer, payload: AccessibleFilesPayload) {
        Log.e("ACCESSIBLE FILES", "Token: ${payload.accessToken}, files: ${payload.files}")

        if (!::vaultBrowserFragment.isInitialized) {
            return
        }
        vaultBrowserFragment.browseRequestableFiles(peer, payload.accessToken, payload.files)
    }

    private fun onFileRequestPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(VaultFileRequestPayload.Deserializer)
        logger.debug { "<- $payload" }
        onFileRequest(peer, payload)
    }

    private fun onAccessibleFilesRequestPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(VaultFileRequestPayload.Deserializer)
        logger.debug { "<- $payload" }
        onAccessibleFilesRequest(peer, payload)
    }

    private fun onFileRequestFailedPacket(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(VaultFileRequestFailedPayload.Deserializer)
        logger.debug { "<- $payload" }
        onFileRequestFailed(payload)
    }

    private fun onFileRequestFailed(payload: VaultFileRequestFailedPayload) {
        Log.e(logTag, payload.message)
        // notify("Failed", payload.message)
    }

    private fun vaultFile(filename: String): Pair<File, AccessControlList> {
        val VAULT = File(context.filesDir, VaultBrowserFragment.VAULT_DIR)
        val file = File(VAULT, filename)
        return Pair(file, AccessControlList(file, attestationCommunity))
    }

    private fun onFileRequest(peer: Peer, payload: VaultFileRequestPayload) {
        Log.e(logTag, "Received file request. Access token: ${payload.accessToken}")
        try {
            val (file, accessPolicy) = vaultFile(payload.id!!)
            if (!file.exists()) {
                Log.e(logTag, "The requested file does not exist")
                sendFileRequestFailed(peer, payload.id, "The requested file does not exist")
            } else if (file.isDirectory) {
                Log.e(logTag, "The requested file is a directory")
                sendFileRequestFailed(peer, payload.id, "The requested file is a directory")
            } else if (!accessPolicy.verifyAccess(peer, payload.accessMode, payload.accessToken, payload.attestations)) {
                Log.e(logTag, "Access Policy not met")
                sendFileRequestFailed(peer, payload.id, "Access Policy not met")
                //sendFile(peer, payload.id, file)
            } else {
                sendFile(peer, payload.id, file)
                logger.debug { "$peer.mid" }
            }
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        }
    }

    private fun onAccessibleFilesRequest(peer: Peer, payload: VaultFileRequestPayload) {
        Log.e(logTag, "accessible files request id: ${payload.id}, accessmode: ${payload.accessMode}, token: ${payload.accessToken}")
        val allFiles = if (payload.id == null) {
            VAULT.list()?.asList()
        } else {
            File(VAULT, payload.id).list()?.asList()
        }

        if (payload.attestations != null && !payload.attestations.isEmpty()) {
            Log.e(logTag, "Attestations for accessible files (${payload.attestations.size} att(s))")
            val accessToken: String? = generateToken(payload.attestations)
            sendAccessibleFiles(peer, accessToken, filterFiles(peer, payload.accessMode, null, payload.attestations, allFiles))
        } else if (payload.accessToken != null) {
            Log.e(logTag, "Access token for accessible files (${payload.accessToken})")
            // Currently accessibleFilesRequest don't contain an access token, only attestations
            sendAccessibleFiles(peer, payload.accessToken, filterFiles(peer, payload.accessMode, payload.accessToken, null, allFiles))
        } else {
            Log.e(logTag, "No credentials. Check public files")
            // no credentials
            sendAccessibleFiles(peer, null, filterFiles(peer, payload.accessMode, null, null, allFiles))
        }
    }

    private fun filterFiles(peer: Peer, accessMode: String, accessToken: String?, attestations: List<AttestationBlob>?, files: List<String>?): List<String>{
        if (files != null) {
            return files.filter { fileName -> !fileName.startsWith(".") && !fileName.endsWith(".acl") }.
                filter { fileName ->
                    val (_, accessPolicy) = vaultFile(fileName)
                    accessPolicy.verifyAccess(peer, accessMode, accessToken, attestations)
                }
        }

        return listOf()
    }

    private fun generateToken(attestations: List<AttestationBlob>): String? {
        if (attestations.isEmpty()) {
            return null
        }

        return "TEMP_TOKEN"
    }

    fun sendFile(peer: Peer, id: String, file: File) {
        Log.e(logTag, "Sending file")
        val payload = AttachmentPayload(id, file.readBytes())
        val packet =
            serializePacket(MessageId.FILE, payload, encrypt = true, recipient = peer)
        logger.debug { "-> $payload" }

        if (evaProtocolEnabled) {
            evaSendBinary(
                peer,
                EVAId.EVA_DATA_VAULT_FILE,
                id,
                packet
            )
        } else {
            send(peer, packet)
        }
    }

    fun sendAccessibleFiles(peer: Peer, accessToken: String?, files: List<String>?) {
        Log.e(logTag, "Sending ${files?.size ?: 0} file(s) to ${peer.publicKey.keyToBin().toHex()}")
        val payload = AccessibleFilesPayload(accessToken, files ?: listOf())
        val packet = serializePacket(MessageId.ACCESSIBLE_FILES, payload)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

    fun sendAccessibleFilesRequest(peer: Peer, accessMode: String, attestations: List<AttestationBlob>, folder: String?) {
        Log.e(logTag, "Sending accessible files request")
        Log.e(logTag, "including ${attestations.size} attestation(s)")
        val payload = VaultFileRequestPayload(folder, accessMode, null, attestations)
        val packet = serializePacket(MessageId.ACCESSIBLE_FILES_REQUEST, payload)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

    fun sendFileRequest(peer: Peer, accessMode: String, id: String, accessToken: String? = null, attestations: List<AttestationBlob>? = null) {
        Log.e(logTag, "Sending file request")
        // Log.e(logTag, "accessToken: $accessToken, including ${attestations?.size ?: 0} attestation(s)")

        val payload = VaultFileRequestPayload(id, accessMode, accessToken, attestations)
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

    private fun notify(id: String, message: String) {
        if (!::vaultBrowserFragment.isInitialized) {
            return
        }
        vaultBrowserFragment.notify(id, message)
    }

    fun setDataVaultActivity(activity: DataVaultMainActivity) {
        dataVaultMainActivity = activity
    }

    fun setVaultBrowserFragment(fragment: VaultBrowserFragment) {
        vaultBrowserFragment = fragment
    }

    object MessageId {
        const val FILE = 11
        const val FILE_REQUEST = 12
        const val FILE_REQUEST_FAILED = 13
        const val ACCESSIBLE_FILES_REQUEST = 14
        const val ACCESSIBLE_FILES = 15
    }

    /**
     * Every community initializes a different version of the EVA protocol (if enabled).
     * To distinguish the incoming packets/requests an ID must be used to hold/let through the
     * EVA related packets.
     */
    object EVAId {
        const val EVA_DATA_VAULT_FILE = "eva_data_vault_file"
    }

    class Factory(
        private val context: Context,
    ) : Overlay.Factory<DataVaultCommunity>(DataVaultCommunity::class.java) {
        override fun create(): DataVaultCommunity {
            return DataVaultCommunity(context)
        }
    }
}
