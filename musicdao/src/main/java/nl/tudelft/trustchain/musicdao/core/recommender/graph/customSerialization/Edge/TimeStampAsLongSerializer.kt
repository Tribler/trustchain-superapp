package nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.Edge

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jgrapht.graph.DefaultWeightedEdge
import java.sql.Timestamp

object TimeStampAsLongSerializer : KSerializer<Timestamp> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Timestamp) {
        encoder.encodeLong(value.time)
    }

    override fun deserialize(decoder: Decoder): Timestamp {
        return Timestamp(decoder.decodeLong())
    }
}
