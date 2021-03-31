package nl.tudelft.trustchain.currencyii.util.taproot

class CTransaction(private val nVersion: Int = 1, val vin: Array<CTxIn> = emptyArray<CTxIn>(), val vout: Array<CTxOut> = emptyArray<CTxOut>(), val nLockTime: Int = 1, val wit: CTxWitness = CTxWitness(), val sha256: UInt? = null, val hash: UInt? = null) {

}

class CTxIn() {

}

class CTxOut() {

}

class CTxWitness(){

}

//    public static byte[] TaprootSignatureHash(CTransaction txTo, spent_utxos, hash_type, input_index = 0, scriptpath = False, tapscript = CScript(), codeseparator_pos = -1, annex = None, tapscript_ver = DEFAULT_TAPSCRIPT_VER):
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

//fun create_spending_transaction(): def??:
//"""Construct a CTransaction object that spends the first ouput from txid."""
//# Construct transaction
//spending_tx = CTransaction()
//
//# Populate the transaction version
//spending_tx.nVersion = version
//
//# Populate the locktime
//spending_tx.nLockTime = 0
//
//# Populate the transaction inputs
//outpoint = COutPoint(int(txid, 16), 0)
//spending_tx_in = CTxIn(outpoint=outpoint, nSequence=nSequence)
//spending_tx.vin = [spending_tx_in]
//
//# Generate new Bitcoin Core wallet address
//dest_addr = self.nodes[0].getnewaddress(address_type="bech32")
//scriptpubkey = bytes.fromhex(self.nodes[0].getaddressinfo(dest_addr)['scriptPubKey'])
//
//# Complete output which returns 0.5 BTC to Bitcoin Core wallet
//amount_sat = int(0.5 * 100_000_000)
//dest_output = CTxOut(nValue=amount_sat, scriptPubKey=scriptpubkey)
//spending_tx.vout = [dest_output]
//
//return spending_tx
