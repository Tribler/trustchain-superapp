package nl.tudelft.trustchain.valuetransfer.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import nl.tudelft.ipv8.keyvault.PublicKey

@Entity
data class TrustScore(
    @PrimaryKey val publicKey: PublicKey,
    @ColumnInfo(name = "score") val trustScore: Float
)
