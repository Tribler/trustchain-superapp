package nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.Edge

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.tudelft.trustchain.musicdao.core.recommender.model.Recommendation

object RecAsStringSerializer : KSerializer<Recommendation> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SongRec", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Recommendation) {
        encoder.encodeString(value.getUniqueIdentifier())
    }

    override fun deserialize(decoder: Decoder): Recommendation {
        return Recommendation(decoder.decodeString())
    }
}

