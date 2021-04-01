package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.sha256
import java.nio.ByteBuffer
import kotlin.experimental.and


class CTransaction(
    val nVersion: Int = 1,
    val vin: Array<CTxIn> = arrayOf(),
    val vout: Array<CTxOut> = arrayOf(),
    val wit: CTxWitness = CTxWitness(),
    val nLockTime: Int = 0,
    val sha256: UInt? = null,
    val hash: UInt? = null
)

class CTxIn(
    val prevout: COutPoint = COutPoint(),
    val scriptSig: ByteArray = byteArrayOf(),
    val nSequence: Int = 0
)

class CTxOut(
    val nValue: Int = 0,
    val scriptPubKey: ByteArray = byteArrayOf()
) {
    fun serialize(): ByteArray {

    }
}

class CTxWitness(val vtxinwit: Array<CTxIn> = arrayOf())

class COutPoint(
    var hash: Byte = 0,
    var n: Int = 0
) {

//    fun deserialize(f: ByteArray) {
//        hash = deser_uint256(f)
//        val f_buf = ByteBuffer.wrap(f)
//        n = f_buf.getInt(f)
//    }

    fun serialize(): ByteArray {
        var r: ByteArray = byteArrayOf()
        r += ser_uint256(hash.toInt())
        val r_buf: ByteBuffer = ByteBuffer.wrap(r)
        r_buf.putInt(n)
        r += r_buf.array()
        return r
    }
}

class CScript(
    val bytes: ByteArray
)

class CScriptOp(val n: Int)

val DEFAULT_TAPSCRIPT_VER = 0xc0.toByte()
val TAPROOT_VER = 0
val SIGHASH_ALL_TAPROOT: Byte = 0
val SIGHASH_ALL: Byte = 1
val SIGHASH_NONE: Byte = 2
val SIGHASH_SINGLE: Byte = 3
val SIGHASH_ANYONECANPAY: Byte = 0x80.toByte()

val OP_HASH160 = CScriptOp(0xa9)
val OP_EQUAL = CScriptOp(0x87)
val OP_1 = CScriptOp(0x51)
val ANNEX_TAG = 0x50.toByte()
fun ser_uint256(u_in: Int): ByteArray {
    var u = u_in
    var rs: ByteArray = byteArrayOf()
    for (i in 0..8) {
        rs += ByteBuffer.allocate(1).putInt((u and 0xFFFFFFFF.toInt()).toInt()).array()
        u = u shr 32
    }
    return rs
}

fun isPayToScriptHash(script: ByteArray): Boolean {
    return script.size == 23 && script[0] == OP_HASH160 && script[1].toInt() == 20 && script[22] == OP_EQUAL
}

fun isPayToTaproot(script: ByteArray): Boolean {
    return script.size == 35 && script[0] == OP_1 && script[1].toInt() == 33 && script[2] >= 0 && script[2] <= 1
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
    input_index: Int = 0,
    scriptpath: Boolean = false,
    tapscript: CScript = CScript(), //TODO: No idea what the input should be
    codeseparator_pos: Int = -1,
    annex: ByteArray? = null,
    tapscript_ver: Byte = DEFAULT_TAPSCRIPT_VER
): ByteArray {
    assert(txTo.vin.size == spent_utxos.size)
    assert((hash_type in 0..3) || (hash_type in 0x81..0x83))
    assert(input_index < txTo.vin.size)

    val spk = spent_utxos[input_index].scriptPubKey
    val ss_buf: ByteBuffer = ByteBuffer.wrap(byteArrayOf(0, hash_type)) // Epoch, hash_type
    ss_buf.putInt(txTo.nVersion)
    ss_buf.putInt(txTo.nLockTime)
    var ss: ByteArray = ss_buf.array()

    if (hash_type and SIGHASH_ANYONECANPAY != 0.toByte()) {
        ss += sha256(txTo.vin.map { it.prevout.serialize() })
        var temp: ByteBuffer = ByteBuffer.allocate(spent_utxos.size)
        for (u in spent_utxos) {
            temp.putInt(u.nValue)
        }
        ss += sha256(temp.array())
        temp = ByteBuffer.allocate(txTo.vin.size)
        for (i in txTo.vin) {
            temp.putInt(i.nSequence)
        }
        ss += sha256(temp.array())
    }
    if ((hash_type and 3) != SIGHASH_SINGLE && (hash_type and 3) != SIGHASH_NONE) {
        ss += sha256(txTo.vout.map { it.serialize() })
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
        assert(tapscript.size > 0)
        assert(codeseparator_pos >= -1)
        spend_type = spend_type or 4
    }
    ss += byteArrayOf(spend_type.toByte())
    ss += ser_string(spk)
    if (hash_type and SIGHASH_ANYONECANPAY != 0.toByte()) {
        ss += txTo.vin[input_index].prevout.serialize()
        ss += ByteBuffer.allocate(1).putInt(spent_utxos[input_index].nValue).array()
        ss += ByteBuffer.allocate(1).putInt(txTo.vin[input_index].nSequence).array()
    } else {
        ss += ByteBuffer.allocate(1).putInt(input_index).array()
    }
    if ((spend_type and 2) != 0) {
        ss += sha256(ser_string(annex))
    }
    if ((hash_type and 3) == SIGHASH_SINGLE) {
        assert(input_index < txTo.vout.size)
        ss += sha256(txTo.vout[input_index].serialize())
    }
    if (scriptpath) {
        ss += tagged_hash("TapLeaf", byteArrayOf(tapscript_ver) + ser_string(tapscript))
        ss += byteArrayOf(0x02)
        ss += ByteBuffer.allocate(1).putInt(codeseparator_pos).array()
    }
    assert(ss.size == 177 - (hash_type and SIGHASH_ANYONECANPAY) * 50 - (hash_type and 3 == SIGHASH_NONE).compareTo(true) * 32 - (isPayToScriptHash(spk)).compareTo(true) * 12 + (annex != null).compareTo(true) * 32 + scriptpath.compareTo(true) * 35)
    return tagged_hash("TapSighash", ss)
}
//        assert (len(txTo.vin) == len(spent_utxos))
//        assert((hash_type >= 0 and hash_type <= 3) or (hash_type >= 0x81 and hash_type <= 0x83))
//        assert (input_index < len(txTo.vin))
//    spk = spent_utxos[input_index].scriptPubKey
//    ss = bytes([0, hash_type]) # epoch, hash_type
//    ss += struct.pack("<i", txTo.nVersion)
//    ss += struct.pack("<I", txTo.nLockTime)
//        if not (hash_type & SIGHASH_ANYONECANPAY):
//    ss += sha256(b"".join(i.prevout.serialize() for i in txTo.vin))
//    ss += sha256(b"".join(struct.pack("<q", u.nValue) for u in spent_utxos))
//    ss += sha256(b"".join(struct.pack("<I", i.nSequence) for i in txTo.vin))
//        if (hash_type & 3) != SIGHASH_SINGLE and (hash_type & 3) != SIGHASH_NONE:
//    ss += sha256(b"".join(o.serialize() for o in txTo.vout))
//    spend_type = 0
//        if IsPayToScriptHash(spk):
//    spend_type = 1
//        else:
//        assert(IsPayToTaproot(spk))
//        if annex is not None:
//        assert (annex[0] == ANNEX_TAG)
//    spend_type |= 2
//        if (scriptpath):
//        assert (len(tapscript) > 0)
//        assert (codeseparator_pos >= -1)
//    spend_type |= 4
//    ss += bytes([spend_type])
//    ss += ser_string(spk)
//    if (hash_type & SIGHASH_ANYONECANPAY):
//    ss += txTo.vin[input_index].prevout.serialize()
//    ss += struct.pack("<q", spent_utxos[input_index].nValue)
//    ss += struct.pack("<I", txTo.vin[input_index].nSequence)
//        else:
//    ss += struct.pack("<H", input_index)
//        if (spend_type & 2):
//    ss += sha256(ser_string(annex))
//        if (hash_type & 3 == SIGHASH_SINGLE):
//        assert (input_index < len(txTo.vout))
//    ss += sha256(txTo.vout[input_index].serialize())
//        if (scriptpath):
//    ss += tagged_hash("TapLeaf", bytes([tapscript_ver]) + ser_string(tapscript))
//    ss += bytes([0x02])
//    ss += struct.pack("<h", codeseparator_pos)
//        assert (len(ss) == 177 - bool(hash_type & SIGHASH_ANYONECANPAY) * 50 - ((hash_type & 3) == SIGHASH_NONE) * 32 - (IsPayToScriptHash(spk)) * 12 + (annex is not None) * 32 + scriptpath * 35)
//        return tagged_hash("TapSighash", ss)

// fun create_spending_transaction(): def??:
// """Construct a CTransaction object that spends the first ouput from txid."""
// # Construct transaction
// spending_tx = CTransaction()
//
// # Populate the transaction version
// spending_tx.nVersion = version
//
// # Populate the locktime
// spending_tx.nLockTime = 0
//
// # Populate the transaction inputs
// outpoint = COutPoint(int(txid, 16), 0)
// spending_tx_in = CTxIn(outpoint=outpoint, nSequence=nSequence)
// spending_tx.vin = [spending_tx_in]
//
// # Generate new Bitcoin Core wallet address
// dest_addr = self.nodes[0].getnewaddress(address_type="bech32")
// scriptpubkey = bytes.fromhex(self.nodes[0].getaddressinfo(dest_addr)['scriptPubKey'])
//
// # Complete output which returns 0.5 BTC to Bitcoin Core wallet
// amount_sat = int(0.5 * 100_000_000)
// dest_output = CTxOut(nValue=amount_sat, scriptPubKey=scriptpubkey)
// spending_tx.vout = [dest_output]
//
// return spending_tx
