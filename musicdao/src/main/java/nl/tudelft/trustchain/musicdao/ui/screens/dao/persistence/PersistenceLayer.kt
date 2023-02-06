package nl.tudelft.trustchain.musicdao.ui.screens.dao.persistence

import android.annotation.SuppressLint
import nl.tudelft.trustchain.musicdao.core.dao.DaoCommunity
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.currencyii.TrustChainHelper
import nl.tudelft.trustchain.currencyii.sharedWallet.SWUtil
import kotlin.reflect.KClass

typealias BlockType = String
typealias ResourceID = String

sealed class PersistentTrustChainBlock() {

    data class RequestBlock(
        val name: String,
        val age: Int
    ) : PersistentTrustChainBlock() {
        companion object {
            const val blockType: BlockType = "test"
        }
    }

    data class AnswerBlock(
        val blockType: String = "tes2t"
    ) : PersistentTrustChainBlock() {
        companion object {
            const val blockType: BlockType = "test"
        }
    }

    companion object {
        val mapping = mapOf(
            RequestBlock.blockType to RequestBlock::class,
            AnswerBlock.blockType to AnswerBlock::class
        )
    }
}

sealed class PersistentTrustChainBlockResource : PersistentTrustChainBlock() {
    abstract var resourceID: ResourceID
}

interface PersistenceLayerInterface {
    fun post(data: PersistentTrustChainBlock)
    fun post(data: PersistentTrustChainBlockResource)
    suspend fun get(type: BlockType, id: ResourceID): PersistentTrustChainBlockResource
    suspend fun get(type: BlockType): List<PersistentTrustChainBlock>
    fun subscribe(type: BlockType): Flow<PersistentTrustChainBlock>
}

class SampleUsage() {
    suspend fun main() {
        val trustChainCommunity = getTrustChainCommunity()
        val daoCommunity = getDaoCommunity()
        val trustChainHelper = TrustChainHelper(trustChainCommunity)
        val trustChainPersistence =
            PersistenceLayer(trustChainCommunity, trustChainHelper, daoCommunity)

        val test = trustChainPersistence.get(PersistentTrustChainBlock.RequestBlock.blockType)
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
            publicKey = me.publicKey.keyToBin()
        )
    }

    override fun post(data: PersistentTrustChainBlockResource) {
        val json = SWUtil.objectToJsonObject(data)
        val jsonString = json.toString()
        val me = trustChainCommunity.myPeer

        trustChainHelper.createProposalBlock(
            message = jsonString,
            publicKey = me.publicKey.keyToBin()
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
            .map { it ->
                val block: KClass<out PersistentTrustChainBlock>? =
                    PersistentTrustChainBlock.mapping[it.type]?.let { classType ->
                        val jsonObject = Gson().fromJson(
                            it.transaction["message"].toString(),
                            JsonObject::class.java
                        )
                        val json: KClass<out PersistentTrustChainBlock> = Gson().fromJson(
                            jsonObject.toString(),
                            classType::class.java
                        )
                        json
                    }
            }

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

    @SuppressLint("NewApi")
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
