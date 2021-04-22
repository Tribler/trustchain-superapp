package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.bitcoinj.core.NetworkParameters
import kotlin.experimental.and

class SegwitAddressUtil {
    companion object {
        /**
         * Construct a {@link org.bitcoinj.core.SegwitAddress} from a byte array.
         *
         * @param program program
         * @return
         */
        fun key_to_witness(pubKeyData: ByteArray): String {
            val program = byteArrayOf(pubKeyData[0] and 1.toByte()).plus(pubKeyData.drop(1)).toHex().hexToBytes();

            assert(2 <= program.size && program.size <= 40)
//            assert(program.size in arrayOf(20, 32))

            return SegwitAddress(NetworkParameters.fromPmtProtocolID("regtest"), 0x01, program).toString()
        }
    }
}
