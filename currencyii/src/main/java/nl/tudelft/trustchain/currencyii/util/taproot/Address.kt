package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex


class Address {
    companion object {

        fun program_to_witness(version: Int, program: ByteArray): String? {
            assert(0 <= version && version <= 16)
            assert(2 <= program.size && program.size <= 40)
            assert(version > 2 || program.size in 20..30)
            val array = program.map { it.toInt() }.toTypedArray()

            assert(program.contentToString().equals(array.toString()))
            return SegwitAddress.encode("bcrt", version, array)
        }
    }
}

fun main() {
    val version = 0x01
    val program = "00f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f9".hexToBytes()
    val address = Address.program_to_witness(version, program)
    println(address)
}
