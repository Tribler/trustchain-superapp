package com.example.musicdao.ui.screens.dao.persistence

import com.example.musicdao.ui.screens.dao.DaoCommunity
import kotlinx.coroutines.flow.Flow
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.currencyii.TrustChainHelper
import nl.tudelft.trustchain.currencyii.sharedWallet.SWUtil
import kotlin.reflect.KClass

typealias BlockType = String
typealias ResourceID = String

val availableBlocks = mapOf<String, PersistentTrustChainBlock>()

sealed class PersistentTrustChainBlock() {
    abstract var blockType: BlockType
}

sealed class PersistentTrustChainBlockResource : PersistentTrustChainBlock() {
    abstract var resourceID: ResourceID
}

data class RequestBlock(
    override var blockType: BlockType = "REQUEST_BLOCK",
    val name: String,
    val age: Int
) : PersistentTrustChainBlock()

data class AnswerBlock(
    override var blockType: BlockType = "ANSWER_BLOCK",
    val test: Boolean
) : PersistentTrustChainBlock()

interface PersistenceLayerInterface {
    fun post(data: PersistentTrustChainBlock)
    fun post(data: PersistentTrustChainBlockResource)
    suspend fun get(type: BlockType, id: ResourceID): PersistentTrustChainBlockResource
    suspend fun get(type: BlockType): List<PersistentTrustChainBlock>
    fun subscribe(type: BlockType): Flow<PersistentTrustChainBlock>
}

class SampleUsage() {
    fun main() {
        val trustChainCommunity = getTrustChainCommunity()
        val daoCommunity = getDaoCommunity()
        val trustChainHelper = TrustChainHelper(trustChainCommunity)
        val trustChainPersistence =
            PersistenceLayer(trustChainCommunity, trustChainHelper, daoCommunity)

        val testObject = RequestBlock(
            name = "sada",
            age = 21312,
            blockType = "sad"
        )
    }

    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    private fun getDaoCommunity(): DaoCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("DaoCommuniuty is not configured")
    }
}

class PersistenceLayer(
    private val trustChainCommunity: TrustChainCommunity,
    private val trustChainHelper: TrustChainHelper,
    private val daoCommunity: DaoCommunity
) : PersistenceLayerInterface {

    override fun post(data: PersistentTrustChainBlock) {
        val json = SWUtil.objectToJsonObject(data)
        val jsonString = json.toString()
        val me = trustChainCommunity.myPeer

        trustChainHelper.createProposalBlock(
            message = jsonString,
            publicKey = me.publicKey.keyToBin(),
            blockType = data.blockType
        )
    }

    override fun post(data: PersistentTrustChainBlockResource) {
        val json = SWUtil.objectToJsonObject(data)
        val jsonString = json.toString()
        val me = trustChainCommunity.myPeer

        trustChainHelper.createProposalBlock(
            message = jsonString,
            publicKey = me.publicKey.keyToBin(),
            blockType = data.blockType
        )
    }

    override suspend fun get(type: BlockType): List<PersistentTrustChainBlock> {
        crawl()

        val peers = daoCommunity.getPeers()

        val blocks = peers.map { peer ->
            val chain = trustChainHelper.getChainByUser(peer.publicKey.keyToBin())
            val filtered = chain.filter { it.type == type }
            filtered
        }.flatten()
//            .map { it ->
//                {
//
//                    // find type
//                    // find class instance
//                    // instantiate class instance
//
// //                    val subclasses = PersistentTrustChainBlock::class.sealedSubclasses
// //                    val types = subclasses.associate {
// //                        val classType = it as PersistentTrustChainBlock
// //                        classType::blockType.get() to it
// //                    }
// //
// //                    val instantiation = types.get(it.type)!!
// //                    val jsonString = it.transaction["message"] as String
// //                    val result = Gson().fromJson(jsonString, instantiation::class.java)
//
//                    val subclasses = PersistentTrustChainBlock::class.sealedSubclasses
//                    val types = subclasses.associate {
//                        val classType = it as PersistentTrustChainBlock
//                        classType::blockType.get() to it
//                    }
//
//                    val instantiation = types.get(it.type)!!
//                    val jsonString = it.transaction["message"] as String
//
// //                    val rta = RuntimeTypeAdapterFactory.of(PersistentTrustChainBlock::class.java)
// //                    types.forEach() { (key, value) ->
// //                        rta.registerSubtype(value::class.java, key)
// //                    }
// //                    val gson = GsonBuilder()
// //                        .registerTypeAdapterFactory(rta)
// //                        .create()
//
//                    val result = Json.fromJsonWithClass(jsonString, instantiation::class.java)
//
//                    result
//                }
//            }

        return listOf()
    }

    fun jsonToPersistentBlock() {
    }

    override suspend fun get(type: BlockType, id: ResourceID): PersistentTrustChainBlockResource {
        TODO("Not yet implemented")
    }

    fun test(): Int {
        return 12211
    }

    override fun subscribe(type: BlockType): Flow<PersistentTrustChainBlock> {
        TODO("Not yet implemented")
    }

    suspend fun crawl() {
        val peers = daoCommunity.getPeers()

        peers.forEach { peer ->
            trustChainCommunity.crawlChain(peer)
        }
    }

    fun <T : Any> KClass<out T>.createInstance(vararg args: Any): T =
        java.constructors.first().newInstance(*args) as T

    companion object {
        fun test() {
        }
    }
}
