package nl.tudelft.trustchain.gossipML.ipv8

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.gossipML.models.OnlineModel
import nl.tudelft.trustchain.gossipML.models.collaborative_filtering.MatrixFactorization
import nl.tudelft.trustchain.gossipML.models.feature_based.Adaline
import nl.tudelft.trustchain.gossipML.models.feature_based.Pegasos
import org.junit.Assert
import org.junit.Test

@ExperimentalUnsignedTypes
class ModelExchangeMessageTest {
    private val lazySodium = LazySodiumJava(SodiumJava())
    private val key = LibNaClSK.fromBin(
        "4c69624e61434c534b3a054b2367b4854a8bf2d12fcd12158a6731fcad9cfbff7dd71f9985eb9f064c8118b1a89c931819d3482c73ebd9be9ee1750dc143981f7a481b10496c4e0ef982".hexToBytes(),
        lazySodium
    )
    private val originPublicKey = key.pub().keyToBin()
    private val ttl = 2u
    private val model1 = Pegasos(0.4, 10, 5)
    private val model2 = MatrixFactorization(Array(0) { "" }.zip(Array(0) { 0.0 }).toMap().toSortedMap())

    @ExperimentalUnsignedTypes
    @Test
    fun checkTTL() {
        val modelType = model1.name
        val payload = ModelExchangeMessage(originPublicKey, ttl, modelType, model1)
        Assert.assertTrue(payload.checkTTL())
        Assert.assertFalse(payload.checkTTL())
    }

    @ExperimentalUnsignedTypes
    @Test
    fun serializeAndDeserializeFeatureBasedModel() {
        val modelType = model1.name
        val payload = ModelExchangeMessage(originPublicKey, ttl, modelType, model1)
        val delta = 0.01
        val serialized = payload.serialize()
        val (deserialized, size) = ModelExchangeMessage.deserialize(serialized, 0)
        Assert.assertEquals(serialized.size, size)
        Assert.assertEquals(payload.modelType, deserialized.modelType)
        Assert.assertArrayEquals(
            (payload.model as OnlineModel).weights,
            (deserialized.model as OnlineModel).weights
        )
        if (payload.model is Adaline && deserialized.model is Adaline) {
            Assert.assertEquals(
                (payload.model as Adaline).bias,
                (deserialized.model as Adaline).bias,
                delta
            )
        }

        Assert.assertEquals(payload.ttl, deserialized.ttl)
        Assert.assertArrayEquals(payload.originPublicKey, deserialized.originPublicKey)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun serializeAndDeserializeMFBasedModel() {
        val modelType = model2.name
        val payload = ModelExchangeMessage(originPublicKey, ttl, modelType, model2)
        val serialized = payload.serialize()
        val (deserialized, size) = ModelExchangeMessage.deserialize(serialized, 0)
        Assert.assertEquals(serialized.size, size)
        Assert.assertEquals(payload.modelType, deserialized.modelType)

        Assert.assertEquals(payload.ttl, deserialized.ttl)
        Assert.assertArrayEquals(payload.originPublicKey, deserialized.originPublicKey)
    }
}
