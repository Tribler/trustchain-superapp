package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.sha256
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.util.taproot.Messages.Companion.deserializeString
import nl.tudelft.trustchain.currencyii.util.taproot.Messages.Companion.deserializeStringVector
import nl.tudelft.trustchain.currencyii.util.taproot.Messages.Companion.deserializeVectorCTxIn
import nl.tudelft.trustchain.currencyii.util.taproot.Messages.Companion.deserializeVectorCTxOut
import nl.tudelft.trustchain.currencyii.util.taproot.Messages.Companion.read
import nl.tudelft.trustchain.currencyii.util.taproot.Messages.Companion.serCompactSize
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.reflect.KProperty1

class CTransaction(
    var nVersion: Int = 1,
    var vin: Array<CTxIn> = arrayOf(),
    var vout: Array<CTxOut> = arrayOf(),
    var wit: CTxWitness = CTxWitness(),
    var nLockTime: Int = 0,
    var sha256: UInt? = null,
    var hash: UInt? = null
) {
    fun serialize(): ByteArray {
        return serializeWithWitness()
    }

    private fun serializeWithWitness(): ByteArray {
        var flags = 0
        if (!wit.isNull()) {
            flags = flags or 1
        }
        var r = byteArrayOf()
        r += littleEndian(nVersion)

        if (flags == 1) {
            val dummy: Array<CTxIn> = arrayOf()
            r += serVector(dummy)
            r += littleEndian(flags.toChar())
        }

        r += serVector(vin)
        r += serVector(vout)

        if ((flags and 1) == 1) {
            if (wit.vtxinwit.size != vin.size) {
                // not tested, not needed for the transactions we make now.
                for (i in wit.vtxinwit.size until vin.size) {
                    wit.vtxinwit += CTxInWitness()
                }
            }

            r += wit.serialize()
        }

        r += littleEndian(nLockTime.toUInt())

        return r
    }

    fun deserialize(b: ByteArray): CTransaction {
        val bytes = b.iterator()
        nVersion = ByteBuffer.wrap(read(bytes, 4)).order(ByteOrder.LITTLE_ENDIAN).int
        vin = deserializeVectorCTxIn(bytes)
        var flags: Char = 0.toChar()
        if (vin.isEmpty()) {
            @Suppress("DEPRECATION")
            flags = ByteBuffer.wrap(read(bytes, 1)).order(ByteOrder.LITTLE_ENDIAN).get().toChar()
            if (flags != 0.toChar()) {
                vin = deserializeVectorCTxIn(bytes)
                vout = deserializeVectorCTxOut(bytes)
            }
        } else {
            vout = deserializeVectorCTxOut(bytes)
        }
        if (flags != 0.toChar()) {
            wit.vtxinwit = Array(vin.size) { CTxInWitness() }
            wit.deserialize(bytes)
        } else {
            wit = CTxWitness()
        }
        nLockTime = ByteBuffer.wrap(read(bytes, 4)).order(ByteOrder.LITTLE_ENDIAN).int
        sha256 = null
        hash = null
        return this
    }

    companion object {

        fun TaprootSignatureHash(
            txTo: CTransaction,
            spentUtxos: Array<CTxOut>,
            hash_type: Byte,
            input_index: Short = 0,
            scriptpath: Boolean = false,
            tapscript: CScript = CScript(),
            codeseparator_pos: Int = -1,
            annex: ByteArray? = null,
            tapscript_ver: Byte = DEFAULT_TAPSCRIPT_VER
        ): ByteArray {
            assert(txTo.vin.size == spentUtxos.size)
            assert((hash_type in 0..3) || (hash_type in 0x81..0x83))
            assert(input_index < txTo.vin.size)

            val spk = spentUtxos[input_index.toInt()].scriptPubKey

            var ssBuf: ByteArray = byteArrayOf(0, hash_type)
            ssBuf += littleEndian(txTo.nVersion)
            ssBuf += littleEndian(txTo.nLockTime)

            if ((hash_type and SIGHASH_ANYONECANPAY) != 1.toByte()) {
                var temp: ByteArray = byteArrayOf()

                for (i in txTo.vin) {
                    temp += i.prevout.serialize()
                }

                ssBuf += sha256(temp)

                temp = byteArrayOf()
                for (u in spentUtxos) {
                    temp += littleEndian(u.nValue)
                }
                ssBuf += sha256(temp)

                temp = byteArrayOf()
                for (i in txTo.vin) {
                    temp += littleEndian(i.nSequence)
                }
                ssBuf += sha256(temp)
            }

            if ((hash_type and 3) != SIGHASH_SINGLE && (hash_type and 3) != SIGHASH_NONE) {
                var toHash = byteArrayOf()
                for (v in txTo.vout) {
                    toHash += v.serialize()
                }
                ssBuf += sha256(toHash)
            }

            var spendType = 0
            if (isPayToScriptHash(spk)) {
                spendType = 1
            } else {
                assert(isPayToTaproot(spk))
            }

            if (annex != null) {
                assert(annex[0] == ANNEX_TAG)
                spendType = spendType or 2
            }

            if (scriptpath) {
                assert(tapscript.size() > 0)
                assert(codeseparator_pos >= -1)
                spendType = spendType or 4
            }

            ssBuf += byteArrayOf(spendType.toByte())
            ssBuf += Messages.serString(spk)

            if (hash_type and SIGHASH_ANYONECANPAY == 1.toByte()) {
                // not tested, not needed for the transactions we make now.
                // this is probably wrong though, need to use little endian instead of big endian when adding to ssBuf
                ssBuf += txTo.vin[input_index.toInt()].prevout.serialize()
                ssBuf += byteArrayOf(spentUtxos[input_index.toInt()].nValue.toByte())
                ssBuf += byteArrayOf(txTo.vin[input_index.toInt()].nSequence.toByte())
            } else {
                ssBuf += littleEndian(input_index.toUShort())
            }

            if ((spendType and 2) == 1) {
                // not tested, not needed for the transactions we make now.
                ssBuf += sha256(Messages.serString(annex!!))
            }

            if ((hash_type and 3) == SIGHASH_SINGLE) {
                // not tested, not needed for the transactions we make now.
                assert(input_index < txTo.vout.size)
                ssBuf += sha256(txTo.vout[input_index.toInt()].serialize())
            }

            if (scriptpath) {
                // not tested, not needed for the transactions we make now.
                // this is probably wrong though, need to use little endian instead of big endian when adding to ssBuf
                ssBuf += taggedHash(
                    "TapLeaf",
                    byteArrayOf(tapscript_ver) + Messages.serString(tapscript.bytes)
                )
                ssBuf += byteArrayOf(0x02)
                ssBuf += byteArrayOf(codeseparator_pos.toShort().toByte())
            }

            assert(
                ssBuf.size == 177 - (hash_type and SIGHASH_ANYONECANPAY) * 50 -
                    (if (hash_type and 3 == SIGHASH_NONE) 1 else 0) * 32 - (
                    if (isPayToScriptHash(
                            spk
                        )
                    )
                        1 else 0
                    ) * 12 + (if (annex != null) 1 else 0) * 32 + (if (scriptpath) 1 else 0) * 35
            )

            return taggedHash("TapSighash", ssBuf)
        }
    }
}

class CTxInWitness(
    witness_stack: Array<ByteArray>? = null,
    val scriptWitness: CScriptWitness = CScriptWitness()
) {
    init {
        if (witness_stack != null) {
            scriptWitness.stack = witness_stack
        }
    }

    fun isNull(): Boolean {
        return scriptWitness.isNull()
    }

    fun serialize(): ByteArray {
        return Messages.serStringVector(scriptWitness.stack)
    }

    fun deserialize(bytes: ByteIterator) {
        this.scriptWitness.stack = deserializeStringVector(bytes)
    }
}

class CTxIn(
    var prevout: COutPoint = COutPoint(),
    var scriptSig: ByteArray = byteArrayOf(),
    var nSequence: Int = 0
) {
    fun serialize(): ByteArray {
        var r: ByteArray = byteArrayOf()
        r += prevout.serialize()
        r += Messages.serString(scriptSig)
        r += littleEndian(nSequence)
        return r
    }

    fun deserialize(bytes: ByteIterator): CTxIn {
        this.prevout = COutPoint()
        this.prevout.deserialize(bytes)
        this.scriptSig = deserializeString(bytes)
        this.nSequence =
            ByteBuffer.wrap(read(bytes, 4)).order(ByteOrder.LITTLE_ENDIAN).int
        return this
    }
}

fun ByteArray.getUIntAt(idx: Int) =
    ((this[idx].toUInt() and 0xFFu) shl 24) or
        ((this[idx + 1].toUInt() and 0xFFu) shl 16) or
        ((this[idx + 2].toUInt() and 0xFFu) shl 8) or
        (this[idx + 3].toUInt() and 0xFFu)

class CTxOut(
    var nValue: Long = 0,
    var scriptPubKey: ByteArray = byteArrayOf()
) {
    fun serialize(): ByteArray {
        var r: ByteArray = byteArrayOf()
        r += littleEndian(nValue)
        r += Messages.serString(scriptPubKey)
        return r
    }

    fun deserialize(bytes: ByteIterator): CTxOut {
        this.nValue =
            ByteBuffer.wrap(read(bytes, 8)).order(ByteOrder.LITTLE_ENDIAN).long
        this.scriptPubKey = deserializeString(bytes)
        return this
    }
}

class CScriptWitness(var stack: Array<ByteArray> = arrayOf()) {
    fun isNull(): Boolean {
        return stack.isEmpty()
    }
}

class CTxWitness(
    var vtxinwit: Array<CTxInWitness> = arrayOf()
) {
    /**
     * This is different than the usual vector serialization
     * we omit the length of the vector, which is required to be
     * the same length as the transaction's vin vector.
     */
    fun serialize(): ByteArray {
        var r: ByteArray = byteArrayOf()
        for (x in vtxinwit) {
            r += x.serialize()
        }
        return r
    }

    fun isNull(): Boolean {
        for (x in vtxinwit) {
            if (!x.isNull()) {
                return false
            }
        }
        return true
    }

    fun deserialize(bytes: ByteIterator): CTxWitness {
        for (element in vtxinwit) {
            element.deserialize(bytes)
        }
        return this
    }
}

fun littleEndian(uint: UInt): ByteArray {
    val bb: ByteBuffer = ByteBuffer.allocate(4)
    bb.order(ByteOrder.LITTLE_ENDIAN)
    bb.putInt(uint.toInt())
    return bb.array()
}

class COutPoint(
    var hash: String = "",
    var n: Int = 0
) {
    fun serialize(): ByteArray {
        var r: ByteArray = byteArrayOf()
        r += serUint256(BigInteger(1, hash.hexToBytes()))
        r += littleEndian(n.toUInt())
        return r
    }

    fun deserialize(bytes: ByteIterator): COutPoint {
        this.hash = Messages.deserializeUInt256(bytes)
        this.n = ByteBuffer.wrap(read(bytes, 4)).order(ByteOrder.LITTLE_ENDIAN).int
        return this
    }
}

class CScript(val bytes: ByteArray = byteArrayOf()) {
    fun size(): Int {
        return bytes.size
    }

    fun toHex(): String {
        return bytes.toHex()
    }
}

class CScriptOp(private val n: Int) {
    override fun equals(other: Any?): Boolean {
        return n.toByte() == other
    }

    override fun hashCode(): Int {
        return n
    }
}

const val DEFAULT_TAPSCRIPT_VER = 0xc0.toByte()
const val TAPROOT_VER = 0
const val SIGHASH_ALL_TAPROOT: Byte = 0
const val SIGHASH_ALL: Byte = 1
const val SIGHASH_NONE: Byte = 2
const val SIGHASH_SINGLE: Byte = 3
const val SIGHASH_ANYONECANPAY: Byte = 0x80.toByte()

val OP_HASH160 = CScriptOp(0xa9)
val OP_EQUAL = CScriptOp(0x87)
val OP_1 = CScriptOp(0x51)
const val ANNEX_TAG = 0x50.toByte()

fun littleEndian(i: BigInteger): ByteArray {
    val bb: ByteBuffer = ByteBuffer.allocate(i.toByteArray().size)
    bb.order(ByteOrder.BIG_ENDIAN)
    bb.put(i.toByteArray())

    val littleEndian = bb.array().reversedArray()
    return if (littleEndian.size > 3) {
        littleEndian.copyOfRange(0, 4)
    } else {
        val output = ByteArray(4)
        littleEndian.forEachIndexed { index, byte -> output[index] = byte }
        output
    }
}

fun serUint256(u_in: BigInteger): ByteArray {
    var u: BigInteger = u_in
    var rs: ByteArray = byteArrayOf()
    for (i in 0..7) {
        rs += littleEndian(u.and(0xFFFFFFFF.toBigInteger()))
        u = u.shiftRight(32)
    }
    return rs
}

fun serVector(l: Array<CTxOut>, ser_function_name: String? = null): ByteArray {
    var r = serCompactSize(l.size)
    for (i in l) {
        r += if (ser_function_name != null) {
            readInstanceProperty(i, ser_function_name)
        } else {
            i.serialize()
        }
    }
    return r
}

fun serVector(l: Array<CTxIn>, ser_function_name: String? = null): ByteArray {
    var r = serCompactSize(l.size)
    for (i in l) {
        r += if (ser_function_name != null) {
            readInstanceProperty(i, ser_function_name)
        } else {
            i.serialize()
        }
    }
    return r
}

@Suppress("UNCHECKED_CAST")
fun <R> readInstanceProperty(instance: Any, propertyName: String): R {
    val property = instance::class.members
        // don't cast here to <Any, R>, it would succeed silently
        .first { it.name == propertyName } as KProperty1<Any, *>
    // force a invalid cast exception if incorrect type here
    return property.get(instance) as R
}

fun isPayToScriptHash(script: ByteArray): Boolean {
    return script.size == 23 && OP_HASH160.equals(script[0]) && script[1].toInt() == 20 && OP_EQUAL.equals(
        script[22]
    )
}

fun isPayToTaproot(script: ByteArray): Boolean {
    return script.size == 35 && OP_1.equals(script[0]) && script[1].toInt() == 33 && script[2] >= 0 && script[2] <= 1
}

fun taggedHash(tag: String, data: ByteArray): ByteArray {
    var ss = sha256(tag.toByteArray(Charsets.UTF_8))
    ss += ss
    ss += data
    return sha256(ss)
}

fun littleEndian(int: Int): ByteArray {
    val bb: ByteBuffer = ByteBuffer.allocate(4)
    bb.order(ByteOrder.LITTLE_ENDIAN)
    bb.putInt(int)
    return bb.array()
}

fun littleEndian(long: Long): ByteArray {
    val bb: ByteBuffer = ByteBuffer.allocate(8)
    bb.order(ByteOrder.LITTLE_ENDIAN)
    bb.putLong(long)
    return bb.array()
}

fun littleEndian(char: Char): ByteArray {
    val bb: ByteBuffer = ByteBuffer.allocate(1)
    bb.order(ByteOrder.LITTLE_ENDIAN)
    @Suppress("DEPRECATION")
    bb.put(char.toByte())
    return bb.array()
}

fun littleEndian(uShort: UShort): ByteArray {
    val bb: ByteBuffer = ByteBuffer.allocate(2)
    bb.order(ByteOrder.LITTLE_ENDIAN)
    bb.put(uShort.toByte())
    return bb.array()
}
