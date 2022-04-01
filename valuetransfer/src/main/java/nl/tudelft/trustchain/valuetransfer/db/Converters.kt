package nl.tudelft.trustchain.valuetransfer.db

import androidx.room.TypeConverter
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex

class Converters {
    @TypeConverter
    fun fromKey(publicKey: PublicKey): String {
        return publicKey.keyToBin().toHex()
    }

    @TypeConverter
    fun hexToKey(publicKey: String): PublicKey {
        return defaultCryptoProvider.keyFromPublicBin(publicKey.hexToBytes())
    }
}
