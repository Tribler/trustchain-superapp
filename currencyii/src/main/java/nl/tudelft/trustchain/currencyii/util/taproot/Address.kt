package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.hexToBytes
import org.bitcoinj.core.NetworkParameters

class Address {
    companion object {

        fun program_to_witness(version: Int, program: ByteArray): String {
            assert(0 <= version && version <= 16)
            assert(2 <= program.size && program.size <= 40)
            assert(version > 0 || program.size in arrayOf(20, 32))
//            val array = program.map { it.toInt() }.toTypedArray()

//            assert(program.contentToString().equals(array.toString()))

            return SegwitAddress(NetworkParameters.fromPmtProtocolID("regtest"), 0x01, program).toString()
        }
    }
}

fun main() {
    val version = 0x01
    val program = "003dd5fc3c1766d0a73466a5997da83efcc529107c9ecd0c56e2a28519f0eb3104".hexToBytes()
    val address = Address.program_to_witness(version, program)
    println(address)
}
