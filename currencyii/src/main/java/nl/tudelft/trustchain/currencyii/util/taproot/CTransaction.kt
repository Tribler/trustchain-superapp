package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.sha256
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.util.taproot.Messages.Companion.ser_compact_size
import nl.tudelft.trustchain.currencyii.util.taproot.Messages.Companion.ser_string
import kotlin.experimental.and
import kotlin.reflect.KProperty1

class CTransaction(
    val nVersion: Int = 1,
    val vin: Array<CTxIn> = arrayOf(),
    val vout: Array<CTxOut> = arrayOf(),
    val wit: CTxWitness = CTxWitness(),
    val nLockTime: Int = 0,
    val sha256: UInt? = null,
    val hash: UInt? = null
) {
    fun serialize(): ByteArray {
        return serialize_with_witness()
    }

    private fun serialize_with_witness(): ByteArray {
        var flags = 0
        if (!wit.is_null()) {
            flags = flags or 1
        }
        var r = byteArrayOf()
        r += nVersion.toInt().toByte()
        if (flags != 0) {
            val dummy: Array<CTxIn> = arrayOf()
            r += ser_vector(dummy)
            r += flags.toChar().toByte()
        }
        r += ser_vector(vin)
        r += ser_vector(vout)
        if ((flags and 1) != 0) {
            if (wit.vtxinwit.size != vin.size) {
                for (i in wit.vtxinwit.size..vin.size) {
                    wit.vtxinwit += CTxInWitness()
                }
            }
            r += wit.serialize()
        }
        r += nLockTime.toUInt().toByte()
        return r
    }
}

class CTxInWitness(
    val witness_stack: Array<ByteArray>? = null,
    val scriptWitness: CScriptWitness = CScriptWitness()
) {
    init {
        if (witness_stack != null) {
            scriptWitness.stack = witness_stack
        }
    }

    fun is_null(): Boolean {
        return scriptWitness.is_null()
    }

    fun serialize(): ByteArray {
        return Messages.ser_string_vector(scriptWitness.stack)
    }
}

class CTxIn(
    val prevout: COutPoint = COutPoint(),
    val scriptSig: ByteArray = byteArrayOf(),
    val nSequence: Int = 0
) {
    fun serialize(): ByteArray {
        var r: ByteArray = byteArrayOf()
        r += prevout.serialize()
        r += Messages.ser_string(scriptSig)
        r += nSequence.toUInt().toByte()
        return r
    }
}

class CTxOut(
    val nValue: Double = 0.0,
    val scriptPubKey: ByteArray = byteArrayOf()
) {
    fun serialize(): ByteArray {
        var r: ByteArray = byteArrayOf()
        r += nValue.toInt().toByte()
        r += Messages.ser_string(scriptPubKey)
        return r
    }
}

class CScriptWitness(var stack: Array<ByteArray> = arrayOf()) {
    fun is_null(): Boolean {
        return stack.size != 0
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

    fun is_null(): Boolean {
        for (x in vtxinwit) {
            if (!x.is_null()) {
                return false
            }
        }
        return true
    }
}

class COutPoint(
    var hash: String = "",
    var n: Int = 0
) {
    fun serialize(): ByteArray {
        var r: ByteArray = byteArrayOf()
        r += ser_uint256(hash.toUInt())
        r += n.toUInt().toByte()
        return r
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

fun ser_uint256(u_in: UInt): ByteArray {
    var u: UInt = u_in
    var rs: ByteArray = byteArrayOf()
    for (i in 0..8) {
        rs += (u and 0xFFFFFFFFu).toUInt().toByte()
        u = u shr 32
    }
    return rs
}

fun ser_vector(l: Array<CTxOut>, ser_function_name: String? = null): ByteArray {
    var r = ser_compact_size(l.size)
    for (i in l) {
        if (ser_function_name != null) {
            r += readInstanceProperty<ByteArray>(i, ser_function_name)
        } else {
            r += i.serialize()
        }
    }
    return r
}

fun ser_vector(l: Array<CTxIn>, ser_function_name: String? = null): ByteArray {
    var r = ser_compact_size(l.size)
    for (i in l) {
        if (ser_function_name != null) {
            r += readInstanceProperty<ByteArray>(i, ser_function_name)
        } else {
            r += i.serialize()
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

fun tagged_hash(tag: String, data: ByteArray): ByteArray {
    var ss = sha256(tag.toByteArray(Charsets.UTF_8))
    ss += ss
    ss += data
    return sha256(ss)
}

fun TaprootSignatureHash(
    txTo: CTransaction,
    spent_utxos: Array<CTxOut>,
    hash_type: Byte,
    input_index: Short = 0,
    scriptpath: Boolean = false,
    tapscript: CScript = CScript(),
    codeseparator_pos: Int = -1,
    annex: ByteArray? = null,
    tapscript_ver: Byte = DEFAULT_TAPSCRIPT_VER
): ByteArray {
    assert(txTo.vin.size == spent_utxos.size)
    assert((hash_type in 0..3) || (hash_type in 0x81..0x83))
    assert(input_index < txTo.vin.size)

    val spk = spent_utxos[input_index.toInt()].scriptPubKey

    var ss_buf: ByteArray = byteArrayOf(0, hash_type)
    ss_buf += txTo.nVersion.toByte()
    ss_buf += txTo.nLockTime.toByte()

    if (hash_type and SIGHASH_ANYONECANPAY != 0.toByte()) {
        ss_buf += sha256(txTo.vin.map { it.prevout.serialize() })
        var temp: ByteArray = byteArrayOf()
        for (u in spent_utxos) {
            temp += u.nValue.toByte()
        }
        ss_buf += sha256(temp)
        temp = byteArrayOf()
        for (i in txTo.vin) {
            temp += i.nSequence.toUInt().toByte()
        }
        ss_buf += sha256(temp)
    }
    if ((hash_type and 3) != SIGHASH_SINGLE && (hash_type and 3) != SIGHASH_NONE) {
        ss_buf += sha256(txTo.vout.map { it.serialize() })
    }
    var spend_type = 0
    if (isPayToScriptHash(spk)) {
        spend_type = 1
    } else {
        assert(isPayToTaproot(spk))
    }
    if (annex != null) {
        assert(annex[0] == ANNEX_TAG)
        spend_type = spend_type or 2
    }
    if (scriptpath) {
        assert(tapscript.size() > 0)
        assert(codeseparator_pos >= -1)
        spend_type = spend_type or 4
    }
    ss_buf += byteArrayOf(spend_type.toByte())
    ss_buf += Messages.ser_string(spk)
    if (hash_type and SIGHASH_ANYONECANPAY != 0.toByte()) {
        ss_buf += txTo.vin[input_index.toInt()].prevout.serialize()
        ss_buf += byteArrayOf(spent_utxos[input_index.toInt()].nValue.toByte())
        ss_buf += byteArrayOf(txTo.vin[input_index.toInt()].nSequence.toByte())
    } else {
        ss_buf += byteArrayOf(input_index.toUShort().toByte())
    }
    if ((spend_type and 2) != 0) {
        ss_buf += sha256(Messages.ser_string(annex!!))
    }
    if ((hash_type and 3) == SIGHASH_SINGLE) {
        assert(input_index < txTo.vout.size)
        ss_buf += sha256(txTo.vout[input_index.toInt()].serialize())
    }
    if (scriptpath) {
        ss_buf += tagged_hash(
            "TapLeaf",
            byteArrayOf(tapscript_ver) + Messages.ser_string(tapscript.bytes)
        )
        ss_buf += byteArrayOf(0x02)
        ss_buf += byteArrayOf(codeseparator_pos.toShort().toByte())
    }
    assert(
        ss_buf.size == 177 - (hash_type and SIGHASH_ANYONECANPAY) * 50 - (hash_type and 3 == SIGHASH_NONE).compareTo(
            true
        ) * 32 - (isPayToScriptHash(spk)).compareTo(true) * 12 + (annex != null).compareTo(true) * 32 + scriptpath.compareTo(
            true
        ) * 35
    )
    return tagged_hash("TapSighash", ss_buf)
}
