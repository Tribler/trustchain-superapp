package nl.tudelft.trustchain.datavault.community

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import id.walt.crypto.fromHexString
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
import nl.tudelft.trustchain.common.ebsi.EBSIVerifier
import nl.tudelft.trustchain.common.ebsi.EBSIWallet
import nl.tudelft.trustchain.common.ebsi.JWTHelper
import nl.tudelft.trustchain.common.ebsi.VerificationListener
import nl.tudelft.trustchain.common.util.TimingUtils
import nl.tudelft.trustchain.datavault.DataVaultMainActivity
import nl.tudelft.trustchain.datavault.accesscontrol.AccessControlList
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import nl.tudelft.trustchain.datavault.tools.isImage
import nl.tudelft.trustchain.datavault.ui.ImageViewHolder
import nl.tudelft.trustchain.datavault.ui.PeerVaultFileItem
import nl.tudelft.trustchain.datavault.ui.VaultBrowserFragment
import nl.tudelft.trustchain.datavault.ui.VaultBrowserFragment.Companion.VAULT_DIR
import nl.tudelft.trustchain.peerchat.community.AttachmentPayload
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileFilter
import java.lang.Exception

private val logger = KotlinLogging.logger {}

class DataVaultCommunity(private val context: Context) : EVACommunity() {
    override val serviceId = "3f0dd0ab2c774be3bd1b7bf2d8dddc2938dd18fa"

    private val logTag = "DataVaultCommunity"

    private lateinit var dataVaultMainActivity: DataVaultMainActivity
    lateinit var vaultBrowserFragment: VaultBrowserFragment
    private val attestationCommunity: AttestationCommunity by lazy {
        IPv8Android.getInstance().getOverlay()!!
    }

//    val sessionTokenCache = SessionTokenCache()
    val ebsiWallet: EBSIWallet get() {return EBSIWallet(context)}

    private val pendingImageViewHolders = mutableMapOf<String, Pair<PeerVaultFileItem, ImageViewHolder>>()
    private val pendingPeerVaultFolders = mutableMapOf<String, PeerVaultFileItem>()

    fun addPendingImageViewHolders(peerVaultFileItem: PeerVaultFileItem, imageViewHolder: ImageViewHolder) {
        pendingImageViewHolders[peerVaultFileItem.cacheKey] = Pair(peerVaultFileItem, imageViewHolder)
    }

    fun addPendingPeerVaultFolders(peerVaultFileItem: PeerVaultFileItem) {
        Log.e(logTag, "Adding ${peerVaultFileItem.cacheKey} to pending folders")
        pendingPeerVaultFolders[peerVaultFileItem.cacheKey] = peerVaultFileItem
    }

    val VAULT by lazy { File(context.filesDir, VAULT_DIR) }

    init {
        messageHandlers[MessageId.FILE] = ::onFilePacket
        messageHandlers[MessageId.TEST_FILE] = ::onTestFilePacket
        messageHandlers[MessageId.FILE_REQUEST] = ::onFileRequestPacket
        messageHandlers[MessageId.TEST_FILE_REQUEST] = ::onTestFileRequestPacket
        messageHandlers[MessageId.FILE_REQUEST_FAILED] = ::onFileRequestFailedPacket
        messageHandlers[MessageId.ACCESSIBLE_FILES_REQUEST] = ::onAccessibleFilesRequestPacket
        messageHandlers[MessageId.ACCESSIBLE_FILES] = ::onAccessibleFilesPacket

        evaProtocolEnabled = false //TODO true
    }

    override fun load() {
        super.load()

        // evaProtocol must be instantiated for DataVaultCommunity.
        // Make sure evaProtocolEnabled is set to true in init{}
        evaProtocol?.setOnEVASendCompleteCallback(::onEVASendCompleteCallback)
        evaProtocol?.setOnEVAReceiveProgressCallback(::onEVAReceiveProgressCallback)
        evaProtocol?.setOnEVAReceiveCompleteCallback(::onEVAReceiveCompleteCallback)
        evaProtocol?.setOnEVAErrorCallback(::onEVAErrorCallback)
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
        return listOf(EVAId.EVA_DATA_VAULT_FILE).contains(info)
    }

    ///////////////////////////////////

    fun onTestFilePacket(packet: Packet) {
        val (peer, payload) = packet.getDecryptedAuthPayload(
            AttachmentPayload.Deserializer, myPeer.key as PrivateKey
        )
        logger.debug { "<- $payload" }
        onTestFile(peer, payload)
    }

    fun onFilePacket(packet: Packet) {
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

    val durations = JSONObject().apply {
        put(Policy.AccessTokenType.SESSION_TOKEN.name, JSONArray())
        put(Policy.AccessTokenType.TCID.name, JSONArray())
        put(Policy.AccessTokenType.JWT.name, JSONArray())
        put(Policy.AccessTokenType.JSONLD.name, JSONArray())
    }
    private fun onTestFile(peer: Peer, payload: AttachmentPayload) {
        Log.e(logTag, "Test File ${payload.id} received  from $peer")
//        onFile(peer, payload.id, payload.data)
        try {
            val testTimestamp = payload.id.split("=")
            val att = testTimestamp[0]
            val sendTime = testTimestamp[1].toLong()
            val receiveTime = TimingUtils.getTimestamp()
            val duration = receiveTime - sendTime
//            Log.e(logTag, "onTestFile: $att duration $duration")
            durations.getJSONArray(att).put(duration)
            Log.e(logTag, "durations: $durations")

        } catch (exception: Exception) {
            Log.e(logTag, "onTestFile: id not a timestamp", exception)
        }
    }

    private fun onFile(peer: Peer, payload: AttachmentPayload) {
        Log.e(logTag, "File received ${payload.id}")
        onFile(peer, payload.id, payload.data)
    }

    private fun onFile(peer: Peer, id: String, data: ByteArray) {
        val peerFileId = PeerVaultFileItem.getCacheKey(peer, id)
        val pair = pendingImageViewHolders[peerFileId]

        if (pair != null) {
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
        Log.e("ACCESSIBLE FILES", "Token: ${payload.sessionToken}, files: ${payload.files}")

        if (!::vaultBrowserFragment.isInitialized) {
            return
        }

        val peerFileId = PeerVaultFileItem.getCacheKey(peer, payload.id!!)
        val pendingPeerVaultFolder = pendingPeerVaultFolders[peerFileId]

        if (pendingPeerVaultFolder != null) {
            Log.e("ACCESSIBLE FILES", "Pending peer vault found ($peerFileId)")
            pendingPeerVaultFolders.remove(peerFileId)
            vaultBrowserFragment.updateAccessibleFiles(pendingPeerVaultFolder, payload.sessionToken, payload.files)
        } else {
            Log.e("ACCESSIBLE FILES", "No pending peer vault found ($peerFileId)")
        }
    }

    private fun onTestFileRequestPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(VaultFileRequestPayload.Deserializer)
        logger.debug { "<- $payload" }
        onTestFileRequest(peer, payload)
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

    var TEST_SESSION_TOKEN: String? = null
    private fun onFileRequestFailed(payload: VaultFileRequestFailedPayload) {
        Log.e(logTag, payload.message)
        if (payload.message.contains("Invalid session token")) {
            Log.e(logTag, "Error contains invalid session token: ${payload.message.split("=")}")
            Log.e(logTag, "Session token: ${payload.message.split("=")[1].trim()}")
            TEST_SESSION_TOKEN = payload.message.split("=")[1].trim()
        }
        // notify("Failed", payload.message)
    }

    private fun vaultFile(file: File): Pair<File, AccessControlList> {
        return Pair(file, AccessControlList(file, this, attestationCommunity))
    }

    private fun vaultFile(filename: String): Pair<File, AccessControlList> {
        val file = File(VAULT, filename)
        return vaultFile(file)
    }

    private fun vaultPath(absolutePath: String): String {
        return absolutePath.split(VAULT.absolutePath + "/").last()
    }

    private fun onTestFileRequest(peer: Peer, payload: VaultFileRequestPayload) {
        Log.e(logTag, "Received test file request from $peer. Access token: ${payload.accessTokenType}")
        onFileRequest(peer, payload )
    }

    private fun onFileRequest(peer: Peer, payload: VaultFileRequestPayload) {
        Log.e(logTag, "Received file request. Access token: ${payload.accessTokenType}")

        val vaultFiles = mutableListOf<Triple<String, File, AccessControlList>>().apply {
            payload.ids.forEach { id ->
                val (file, accessPolicy) = vaultFile(id)
                add(Triple(id, file, accessPolicy))
            }
        }

//        val (file, accessPolicy) = vaultFile(payload.id!!)

        when(payload.accessTokenType) {
            Policy.AccessTokenType.SESSION_TOKEN -> {
                val tempSessionToken = payload.accessTokens[0]
                JWTHelper.verifyJWT(tempSessionToken, VerificationListener { jwtPayload ->
                    if (jwtPayload == null) {
//                    Invalid session token (not verified)
                        sendFileRequestFailed(peer, payload.ids,"Invalid session token. New token=${generateToken(peer)}")
                    } else {
//                    Valid session token
                        if (jwtPayload["exp"]?.toString()?.toLongOrNull() ?: Long.MAX_VALUE > TimingUtils.getTimestamp()) {
//                        Expired
                            sendFileRequestFailed(peer, payload.ids,"Session token expired")
                        } else {
                            verifyAndSendFiles(vaultFiles, peer, payload)
                        }
                    }
                }, ebsiWallet.publicKey)
            }
            Policy.AccessTokenType.TCID -> {
                val attestations = AccessControlList.filterAttestations(
                    peer,
                    payload.accessTokens.map {
                        AttestationBlob.deserialize(it.fromHexString()).first
                    }
                )
                verifyAndSendFiles(vaultFiles, peer, payload, attestations)
            }
            Policy.AccessTokenType.JWT -> {
                payload.accessTokens.forEach { jwt ->
                    EBSIVerifier.verifyJWT(jwt, VerificationListener { jwtPayload ->
                        if (jwtPayload == null) {
                            sendFileRequestFailed(peer, payload.ids,"Invalid jwt")
                        } else {
                            verifyAndSendFiles(vaultFiles, peer, payload)
                        }
                    })
                }
            }
            else -> verifyAndSendFiles(vaultFiles, peer, payload)
        }
    }

    private fun verifyAndSendFiles(vaultFiles: List<Triple<String, File, AccessControlList>>, peer: Peer, payload: VaultFileRequestPayload, attestations: List<AttestationBlob>? = null) {
        vaultFiles.forEach { vaultFile ->
            val id = if (vaultBrowserFragment.PERFORMANCE_TEST) payload.accessMode else vaultFile.first
            val file = vaultFile.second
            val accessPolicy = vaultFile.third

            if (!file.exists()) {
                Log.e(logTag, "The requested file does not exist")
                sendFileRequestFailed(peer, listOf(id), "The requested file does not exist")
            } else if (file.isDirectory) {
                Log.e(logTag, "The requested file is a directory")
                sendFileRequestFailed(peer, listOf(id), "The requested file is a directory")
            } else if (!accessPolicy.verifyAccess(payload.accessMode, payload.accessTokenType, attestations ?: payload.accessTokens)) {
                Log.e(logTag, "Access Policy not met")
                sendFileRequestFailed(peer, listOf(id), "Access Policy not met")
            } else {
                try {
                    when {
                        file.isImage() -> {
                            Log.e(logTag, "File being sent is image")
                            sendImage(peer, id, file)
                        }
                        else -> {
                            sendFile(peer, id, file)
                        }
                    }
                } catch (e: SQLiteConstraintException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /*
    else {
                accessPolicy.verifyAccess(peer, payload.accessMode, payload.accessTokenType, payload.accessTokens, nl.tudelft.trustchain.datavault.accesscontrol.VerificationListener { verified ->
                    if (verified) {
                        try {
                            when {
                                file.isImage() -> {
                                    Log.e(logTag, "File being sent is image")
                                    sendImage(peer, id, file)
                                }
                                else -> {
                                    sendFile(peer, id, file)
                                }
                            }
                        } catch (e: SQLiteConstraintException) {
                            e.printStackTrace()
                        }
                    } else {
                        Log.e(logTag, "Access Policy not met")
                        sendFileRequestFailed(peer, listOf(id), "Access Policy not met")
                    }
                })
            }
*/
    private fun onAccessibleFilesRequest(peer: Peer, payload: VaultFileRequestPayload) {
        val folderId = payload.ids.firstOrNull()
        Log.e(logTag, "accessible files request id: $folderId, access mode: ${payload.accessMode}, token: ${payload.accessTokenType}")

        when(payload.accessTokenType) {
            Policy.AccessTokenType.SESSION_TOKEN -> {
                val tempSessionToken = payload.accessTokens[0]
                JWTHelper.verifyJWT(tempSessionToken, VerificationListener { jwtPayload ->
                    if (jwtPayload == null) {
//                    Invalid session token (not verified)
                        sendAccessibleFilesRequestFailed(peer, folderId,"Invalid session token")
                    } else {
//                    Valid session token
                        if (jwtPayload["exp"]?.toString()?.toLongOrNull() ?: Long.MAX_VALUE > TimingUtils.getTimestamp()) {
//                        Expired
                            sendAccessibleFilesRequestFailed(peer, folderId,"Session token expired")
                        } else {
                            filterAndSendAccessibleFiles(peer, payload, tempSessionToken)
                        }
                    }
                }, ebsiWallet.publicKey)

                //            val tempSessionToken = sessionTokenCache.get(payload.accessTokens[0])
//            if (tempSessionToken == null) {
//                sendAccessibleFilesRequestFailed(peer, payload.id,"Invalid session token")
//                return
//            } else if (tempSessionToken.isExpired()) {
//                sendAccessibleFilesRequestFailed(peer, payload.id,"Session token expired")
//                return
//            }
//            tempSessionToken.extend()
//            sessionTokenCache.set(tempSessionToken.value, tempSessionToken)
//            tempSessionToken
            }
            Policy.AccessTokenType.TCID -> {
                val attestations = AccessControlList.filterAttestations(
                    peer,
                    payload.accessTokens.map {
                        AttestationBlob.deserialize(it.fromHexString()).first
                    }
                )
                filterAndSendAccessibleFiles(peer, payload, null, attestations)
            }
            Policy.AccessTokenType.JWT -> {
                payload.accessTokens.forEach { jwt ->
                    EBSIVerifier.verifyJWT(jwt, VerificationListener { jwtPayload ->
                        if (jwtPayload == null) {
                            sendFileRequestFailed(peer, payload.ids,"Invalid jwt")
                        } else {
                            filterAndSendAccessibleFiles(peer, payload, null)
                        }
                    })
                }
            }
            else -> filterAndSendAccessibleFiles(peer, payload, null)
        }
    }

    private fun filterAndSendAccessibleFiles(peer: Peer, payload: VaultFileRequestPayload, sessionToken: String?, attestations: List<AttestationBlob>? = null) {
        val fileFilter = FileFilter { file ->
            val fileName = file.name
            val (_, accessPolicy) = vaultFile(file)
            !fileName.startsWith(".") && !fileName.endsWith(".acl") && accessPolicy.verifyAccess(payload.accessMode, payload.accessTokenType, attestations ?: payload.accessTokens)
        }

        val folderId = payload.ids.firstOrNull()
        val filteredFiles = if (folderId == null || folderId == VAULT_DIR) {
            VAULT.listFiles(fileFilter)
        } else {
            File(VAULT, folderId).listFiles(fileFilter)
        }?.map { file ->
            Log.e("PATHS", "VAULT PATH: ${vaultPath(file.absolutePath)}")
            if (file.isDirectory) "${vaultPath(file.absolutePath)}/" else vaultPath(file.absolutePath)
        }

        sendAccessibleFiles(peer, folderId, sessionToken ?: generateToken(peer), filteredFiles)
    }

    private fun generateToken(peer: Peer): String {
//        TODO pass filtered files to generate DirectoryTree
        return JWTHelper.createSessionToken(ebsiWallet, peer.mid)
//        val sessionToken = SessionToken(peer)
//        sessionTokenCache.set(sessionToken.value, sessionToken)
//        return sessionToken
    }

    private fun sendBytes(peer: Peer, id: String, evaId: String, data: ByteArray) {
        val payload = AttachmentPayload(id, data)
        val messageId = if (vaultBrowserFragment.PERFORMANCE_TEST) MessageId.TEST_FILE else MessageId.FILE
        val packet = serializePacket(messageId, payload, encrypt = true, recipient = peer)
        logger.debug { "-> $payload" }

        if (evaProtocolEnabled) {
            evaSendBinary(
                peer,
                evaId,
                id,
                packet
            )
        } else {
            send(peer, packet)
        }
    }

    private fun sendFile(peer: Peer, id: String, file: File) {
        sendBytes(peer, id, EVAId.EVA_DATA_VAULT_FILE, file.readBytes())
    }

    private fun sendImage(peer: Peer, id: String, file: File) {
        val options = BitmapFactory.Options().also {
            it.outWidth = ImageViewHolder.IMAGE_WIDTH
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
        val os = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)

        if (vaultBrowserFragment.PERFORMANCE_TEST) {
            sendBytes(peer, id, EVAId.TEST_EVA_DATA_VAULT_FILE, os.toByteArray())
        } else {
            sendBytes(peer, id, EVAId.EVA_DATA_VAULT_FILE, os.toByteArray())
        }
    }

    private fun sendAccessibleFiles(peer: Peer, id: String?, sessionToken: String, files: List<String>?) {
        Log.e(logTag, "Sending ${files?.size ?: 0} file(s) to ${peer.publicKey.keyToBin().toHex()}")
        val payload = AccessibleFilesPayload(id, sessionToken, files ?: listOf())
        val packet = serializePacket(MessageId.ACCESSIBLE_FILES, payload, encrypt = true)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

//    fun sendAccessibleFilesRequest(peerVaultFileItem: PeerVaultFileItem, id: String?, accessMode: String, accessTokenType: Policy.AccessTokenType, accessTokens: List<String>) {
    fun sendAccessibleFilesRequest(peerVaultFileItem: PeerVaultFileItem, id: String?, accessMode: String, accessTokenType: Policy.AccessTokenType, accessTokens: List<String>) {
        Log.e(logTag, "Sending accessible files request for $id")
        Log.e(logTag, "including ${accessTokens.size} access token(s) ($accessTokenType)")
        val payload = VaultFileRequestPayload(if (id == null) listOf() else listOf(id), accessMode, accessTokenType, accessTokens)
        val packet = serializePacket(MessageId.ACCESSIBLE_FILES_REQUEST, payload)
        logger.debug { "-> $payload" }
        addPendingPeerVaultFolders(peerVaultFileItem)
        send(peerVaultFileItem.peer, packet)
    }

    fun sendTestFileRequest(peer: Peer,
                            accessMode: String,
                            ids: List<String>,
                            accessTokenType: Policy.AccessTokenType,
                            accessTokens: List<String>) {
        Log.e(logTag, "Sending test file request ($accessTokenType)")

        val payload = VaultFileRequestPayload(ids, accessMode, accessTokenType, accessTokens)
        val packet = serializePacket(MessageId.TEST_FILE_REQUEST, payload)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

//    fun sendFileRequest(peer: Peer, accessMode: String, id: String, sessionToken: String) {
    fun sendFileRequest(peer: Peer, accessMode: String, ids: List<String>, sessionToken: String) {
        Log.e(logTag, "Sending file request")
        // Log.e(logTag, "accessToken: $accessToken, includFing ${attestations?.size ?: 0} attestation(s)")

        val payload = VaultFileRequestPayload(ids, accessMode, Policy.AccessTokenType.SESSION_TOKEN, listOf(sessionToken))
        val packet = serializePacket(MessageId.FILE_REQUEST, payload)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

//    private fun sendFileRequestFailed(peer: Peer, id: String, message: String) {
    private fun sendFileRequestFailed(peer: Peer, ids: List<String>, message: String) {
        Log.e(logTag, "File request $ids failed ($message)")
        val payload = VaultFileRequestFailedPayload(ids, "Vault file request $ids: $message")
        val packet = serializePacket(MessageId.FILE_REQUEST_FAILED, payload)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

    private fun sendAccessibleFilesRequestFailed(peer: Peer, id: String?, message: String) {
        Log.e(logTag, "Accessible files request failed ($message)")
        val payload = VaultFileRequestFailedPayload(if (id == null) listOf() else listOf(id), "Accessible files request $id: $message")
        // TODO own MessageId?
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

    /*fun setVaultBrowserFragment(fragment: VaultBrowserFragment) {
        vaultBrowserFragment = fragment
    }*/

    object MessageId {
        const val FILE = 11
        const val FILE_REQUEST = 12
        const val FILE_REQUEST_FAILED = 13
        const val ACCESSIBLE_FILES_REQUEST = 14
        const val ACCESSIBLE_FILES = 15
        const val TEST_FILE = 21
        const val TEST_FILE_REQUEST = 22
    }

    /**
     * Every community initializes a different version of the EVA protocol (if enabled).
     * To distinguish the incoming packets/requests an ID must be used to hold/let through the
     * EVA related packets.
     */
    object EVAId {
        const val EVA_DATA_VAULT_FILE = "eva_data_vault_file"
        const val TEST_EVA_DATA_VAULT_FILE = "test_eva_data_vault_file"
    }

    class Factory(
        private val context: Context,
    ) : Overlay.Factory<DataVaultCommunity>(DataVaultCommunity::class.java) {
        override fun create(): DataVaultCommunity {
            return DataVaultCommunity(context)
        }
    }
}
