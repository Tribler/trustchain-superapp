package nl.tudelft.trustchain.musicdao.core.recommender.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.tudelft.trustchain.gossipML.models.collaborative_filtering.SongFeature
import nl.tudelft.trustchain.gossipML.models.collaborative_filtering.SongFeatureSerializer
import java.util.*

object UserRatingsSerializer : KSerializer<Map<String, Float>> {
    private val mapSerializer = MapSerializer(String.serializer(), Float.serializer())
    override val descriptor: SerialDescriptor = mapSerializer.descriptor
    override fun serialize(encoder: Encoder, value: Map<String, Float>) {
        mapSerializer.serialize(encoder, value)
    }
    override fun deserialize(decoder: Decoder): Map<String, Float> {
        return mapSerializer.deserialize(decoder).toMap()
    }
}
@Serializable(with = UserRatingsSerializer::class)
data class UserRatings(
    val ratings: Map<String, Float>
)
