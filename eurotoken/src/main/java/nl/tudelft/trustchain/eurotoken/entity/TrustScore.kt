package nl.tudelft.trustchain.eurotoken.entity
import nl.tudelft.ipv8.keyvault.PublicKey
import java.util.*

data class TrustScore (
    val pubKey : PublicKey,
    val trust : Int
)
