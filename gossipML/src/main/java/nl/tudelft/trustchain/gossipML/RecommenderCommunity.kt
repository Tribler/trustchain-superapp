package nl.tudelft.trustchain.gossipML

import android.util.Log
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCrawler
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.trustchain.gossipML.db.RecommenderStore
import nl.tudelft.trustchain.gossipML.ipv8.FeaturesExchangeMessage
import nl.tudelft.trustchain.gossipML.ipv8.ModelExchangeMessage
import nl.tudelft.trustchain.gossipML.ipv8.RequestModelMessage
import nl.tudelft.trustchain.gossipML.models.Model
import nl.tudelft.trustchain.gossipML.models.OnlineModel
import nl.tudelft.trustchain.gossipML.models.collaborative_filtering.MatrixFactorization
import nl.tudelft.trustchain.gossipML.models.collaborative_filtering.PublicMatrixFactorization
import java.util.*
import kotlin.random.Random

/**
 * Recommender community for gossiping recommendation models and songs features among peers
 */
@ExperimentalUnsignedTypes
open class RecommenderCommunity(
    val recommendStore: RecommenderStore,
    settings: TrustChainSettings,
    database: TrustChainStore,
    crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler) {
    override val serviceId = "29384902d2938f34872398758cf7ca9238ccc333"

    class Factory(
        private val recommendStore: RecommenderStore,
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<RecommenderCommunity>(RecommenderCommunity::class.java) {
        override fun create(): RecommenderCommunity {
            return RecommenderCommunity(recommendStore, settings, database, crawler)
        }
    }

    init {
        messageHandlers[MessageId.MODEL_EXCHANGE_MESSAGE] = ::onModelExchange
        messageHandlers[MessageId.REQUEST_MODEL] = ::onModelRequest
        messageHandlers[MessageId.EXCHANGE_FEATURES] = ::onFeaturesExchange
    }

    /**
     * on initialization initiate walking models
     */
    override fun load() {
        super.load()

        if (Random.nextInt(0, 1) == 0) initiateWalkingModel()
    }

    /**
     * exchange extracted features for local songs with other peers
     */
    fun exchangeMyFeatures() {
        try {
            Log.i("Recommend", "Send my features")
            performLocalFeaturesExchange()
        } catch (e: Exception) {
            Log.i("Recommend", "Sending local features failed")
            e.printStackTrace()
        }
    }

    /**
     * Load / create walking models (feature based and collaborative filtering)
     */
    fun initiateWalkingModel() {
        try {
            Log.i("Recommend", "Initiate random walk")
            performRemoteModelExchange(recommendStore.getLocalModel("Pegasos"))
            performRemoteModelExchange(recommendStore.getLocalModel("MatrixFactorization"))
        } catch (e: Exception) {
            Log.i("Recommend", "Random walk failed")
            e.printStackTrace()
        }
    }

    /**
     * Exchange walking model with other peers
     *
     * @param model walking model
     * @param ttl ttl of a model packet
     * @param originPublicKey public key
     * @return amount of peers to whom model has been sent
     */
    @ExperimentalUnsignedTypes
    fun performRemoteModelExchange(
        model: Model,
        ttl: UInt = 2u,
        originPublicKey: ByteArray = myPeer.publicKey.keyToBin()
    ): Int {
        val maxPeersToAsk = 5
        var count = 0
        for ((index, peer) in getPeers().withIndex()) {
            if (index >= maxPeersToAsk) break
            val packet = serializePacket(
                MessageId.MODEL_EXCHANGE_MESSAGE,
                ModelExchangeMessage(originPublicKey, ttl, model.name, model)
            )
            Log.i("Recommend", "Sending models to ${peer.address}")
            send(peer, packet)
            count += 1
        }
        return count
    }

    /**
     * Exchange local song's features with other peers
     */
    private fun performLocalFeaturesExchange() {
        val myFeatures = recommendStore.getMyFeatures()
        for (feature in myFeatures) {
            feature.songFeatures?.let { performFeaturesExchange(feature.key, it) }
        }
    }

    /**
     * exchange packet with song features with other peers
     *
     * @param identifier song indentifier
     * @param features song features
     * @param ttl packet ttl
     * @param originPublicKey original public key
     * @return amount of peers to whom features has been sent
     */
    @ExperimentalUnsignedTypes
    fun performFeaturesExchange(
        identifier: String,
        features: String,
        ttl: UInt = 2u,
        originPublicKey: ByteArray = myPeer.publicKey.keyToBin()
    ): Int {
        val maxPeersToAsk = 5
        var count = 0
        for ((index, peer) in getPeers().withIndex()) {
            if (index >= maxPeersToAsk) break
            val packet = serializePacket(
                MessageId.EXCHANGE_FEATURES,
                FeaturesExchangeMessage(originPublicKey, ttl, identifier, features)
            )
            Log.i("Recommend", "Sending features to ${peer.address}")
            send(peer, packet)
            count += 1
        }
        Log.i("Recommend", "Features exchanged with $count peer(s)")
        return count
    }

    /**
     * Handling incoming walking model by updating it with local features
     * and merging with local model
     *
     * @param packet incoming packet with walking model
     */
    @ExperimentalUnsignedTypes
    fun onModelExchange(packet: Packet) {
        Log.i("Recommend", "Some packet with model received")
        val (_, payload) = packet.getAuthPayload(ModelExchangeMessage)

        // packet contains model type and weights from peer
        @Suppress("DEPRECATION")
        val modelType = payload.modelType.toLowerCase(Locale.ROOT)
        var peerModel = payload.model
        Log.i("Recommend", "Walking model is de-packaged")

        var localModel = recommendStore.getLocalModel(modelType)

        if (modelType == "Adaline" || modelType == "Pegasos") {
            val data = recommendStore.getLocalSongData()
            val songFeatures = data.first
            val playcounts = data.second
            val models = mergeFeatureModel(
                localModel as OnlineModel,
                peerModel as OnlineModel,
                songFeatures,
                playcounts
            )
            Log.i("Recommend", "Walking an random models are merged")
            recommendStore.storeModelLocally(models.first)
            if (payload.checkTTL()) performRemoteModelExchange(models.second)
        } else {
            peerModel = peerModel as PublicMatrixFactorization
            localModel = localModel as MatrixFactorization
            if (localModel.songFeatures.size == 0) {
                localModel = MatrixFactorization(peerModel.peerFeatures)
                localModel.updateRatings(
                    recommendStore.getSongIds().zip(recommendStore.getPlaycounts()).toMap()
                        .toSortedMap()
                )
                recommendStore.storeModelLocally(localModel)
                val maxPeersToAsk = 5
                var count = 0
                for ((index, peer) in getPeers().withIndex()) {
                    if (index >= maxPeersToAsk) break
                    send(
                        peer,
                        serializePacket(
                            MessageId.REQUEST_MODEL,
                            RequestModelMessage(
                                myPeer.publicKey.keyToBin(),
                                2u,
                                "MatrixFactorization"
                            )
                        )
                    )
                    count += 1
                }
                Log.i("Recommend", "Model request sent to $count peer(s)")
            } else {
                Log.i("Recommend", "Merging MatrixFactorization")
                localModel.updateRatings(
                    recommendStore.getSongIds().zip(recommendStore.getPlaycounts()).toMap()
                        .toSortedMap()
                )
                localModel.merge(peerModel.peerFeatures)
                recommendStore.storeModelLocally(localModel)
                Log.i("Recommend", "Stored new MatrixFactorization")
                if (payload.checkTTL()) performRemoteModelExchange(localModel)
            }
        }
    }

    /**
     * Handling incomingpacket with song features
     * by adding 'unseen' features to the local database
     *
     * @param packet incoming packet with song features
     */
    @ExperimentalUnsignedTypes
    fun onFeaturesExchange(packet: Packet) {
        Log.i("Recommend", "Some packet with features received")
        val (_, payload) = packet.getAuthPayload(FeaturesExchangeMessage)

        // packet contains model type and weights from peer
        @Suppress("DEPRECATION")
        val songIdentifier = payload.songIdentifier.toLowerCase(Locale.ROOT)
        val features = payload.features
        Log.i("Recommend", "Song features are de-packaged")

        recommendStore.addNewFeatures(songIdentifier, features)
        if (payload.checkTTL()) performFeaturesExchange(songIdentifier, features)
    }

    /**
     * Handle model request from other peers
     *
     * @param packet request model packet
     */
    private fun onModelRequest(packet: Packet) {
        Log.i("Recommend", "Model request received")
        val (_, payload) = packet.getAuthPayload(ModelExchangeMessage)
        @Suppress("DEPRECATION")
        val modelType = payload.modelType.toLowerCase(Locale.ROOT)
        val model = recommendStore.getLocalModel(modelType)
        send(
            packet.source,
            serializePacket(
                MessageId.MODEL_EXCHANGE_MESSAGE,
                ModelExchangeMessage(myPeer.publicKey.keyToBin(), 1u, model.name, model)
            )
        )
    }

    /**
     * Handle local and walking model merging and updating
     *
     * @param incomingModel walking model
     * @param localModel local model
     * @param features local features
     * @param labels local labels
     * @return pair of merged local and merged walking models
     */
    private fun mergeFeatureModel(
        incomingModel: OnlineModel,
        localModel: OnlineModel,
        features: Array<Array<Double>>,
        labels: IntArray
    ):
        Pair<OnlineModel, OnlineModel> {
        localModel.merge(incomingModel)
        incomingModel.update(features, labels.map { it.toDouble() }.toTypedArray())
        return Pair(localModel, incomingModel)
    }

    object MessageId {
        val MODEL_EXCHANGE_MESSAGE: Int
            get() {
                return 27
            }
        val REQUEST_MODEL: Int
            get() {
                return 40
            }
        val EXCHANGE_FEATURES: Int
            get() {
                return 99
            }
    }
}
