package nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.Edge

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node

object NodeAsStringSerializer : KSerializer<Node> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Node", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Node) {
        encoder.encodeString(value.getKey())
    }

    override fun deserialize(decoder: Decoder): Node {
        return Node(decoder.decodeString())
    }
}

