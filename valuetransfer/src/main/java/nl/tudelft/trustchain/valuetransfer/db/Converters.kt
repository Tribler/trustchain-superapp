package nl.tudelft.trustchain.valuetransfer.db

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    @TypeConverter
    fun fromPairList(value: ArrayList<Pair<Float, Float>>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toPairList(value: String): ArrayList<Pair<Float, Float>> {
        return Json.decodeFromString(value)
    }
}
