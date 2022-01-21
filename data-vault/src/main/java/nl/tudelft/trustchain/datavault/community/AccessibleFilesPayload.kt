package nl.tudelft.trustchain.datavault.community

import android.util.Log
import nl.tudelft.ipv8.attestation.wallet.AttestationBlob
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen
import nl.tudelft.ipv8.util.toHex
import org.json.JSONArray
import java.nio.charset.Charset

class AccessibleFilesPayload(
    val accessToken: String,
    val files: List<String>
): Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(accessToken.toByteArray()) + serializeVarLen(JSONArray(files).toString().toByteArray())
    }

    companion object Deserializer : Deserializable<AccessibleFilesPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<AccessibleFilesPayload, Int> {
            var localOffset = offset
            val (token, tokenSize) = deserializeVarLen(buffer, localOffset)
            localOffset += tokenSize
            val (filesBin, _) = deserializeVarLen(buffer, localOffset)

            var files = mutableListOf<String>()
            val jsonFiles = JSONArray(filesBin.toString(Charset.defaultCharset()))
            var i = 0
            while ( i < jsonFiles.length()) {
                files.add(jsonFiles.getString(i))
                i++
            }
            return Pair(AccessibleFilesPayload(token.toString(Charset.defaultCharset()), files), 0)
        }
    }
}
