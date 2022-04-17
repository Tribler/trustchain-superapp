package nl.tudelft.trustchain.valuetransfer.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex

/**
 * Database entity representing a trust score.
 * Each score contains:
 * - a public key (to identify to who to score belongs)
 * - a score value indicating the current trust level
 * - a list of received values containing a given trust score to the public key, together with the score of the giving party.
 * Can be serialized using the kotlin serialize functionality.
 */
@kotlinx.serialization.Serializable
@Entity
data class TrustScore(
    @kotlinx.serialization.Serializable(with = Serializer::class)
    @PrimaryKey val publicKey: PublicKey,
    @ColumnInfo(name = "score") val trustScore: Float,
    @Transient
    @ColumnInfo(name = "values") val values: ArrayList<Pair<Float, Float>>? = null
) {
    companion object {
        // Serializer for the PublicKey
        object Serializer : KSerializer<PublicKey> {
            override fun deserialize(decoder: Decoder): PublicKey {
                return defaultCryptoProvider.keyFromPublicBin(decoder.decodeString().hexToBytes())
            }

            override val descriptor: SerialDescriptor
                get() = PrimitiveSerialDescriptor("PublicKey", PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, value: PublicKey) {
                return encoder.encodeString(value.keyToBin().toHex())
            }

        }
    }
}
