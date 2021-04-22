package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.bitcoinj.core.ECKey
import org.junit.Test

import org.junit.Assert.*
import java.math.BigInteger
import kotlin.experimental.and

class MuSigTest {

    @Test
    fun aggregate_musig_signatures() {
        val nonceKey1 = ECKey.fromPrivate(BigInteger("514451593258031455215956018794650590333274290798379324717376700610851698631"))
        val nonceKey2 = ECKey.fromPrivate(BigInteger("11956400277919736063286645919884525832160975522703242034429953595835464801432"))
        val R_agg = MuSig.aggregate_schnorr_nonces(listOf(nonceKey1, nonceKey2)).first

        val s1 = BigInteger("87702316580188192134768828685038019069684190982614451361055363232288478978335")
        val s2 = BigInteger("70205003187379344988121702708730152167489459605852932449770410784261163634250")

        val expected = "7bdd007a2ada0fbf18fe8ea7858398e2775195db1a2cef127ef38eef861027bf5d1c6031344b0127f0ebc54b27eb1b7a1cd34229e86be03a2e521ccd92066e28"
        val actual = MuSig.aggregate_musig_signatures(listOf(s1, s2), R_agg).toHex()

        assertEquals(expected, actual)
    }

    @Test
    fun sign_musig() {
        val key1 = ECKey.fromPrivate(BigInteger("88218786999700320424912157840922001183470238663577897435520060565802125439712"))
        val key2 = ECKey.fromPrivate(BigInteger("11756621930195768229168784074199362003209438395325908648574429387730312779458"))
        val agg_pubkey = MuSig.generate_musig_key(listOf(key1, key2)).second

        val key1_c = ECKey.fromPrivate(BigInteger("1831054192583883058098689099279726084283766354549572339919052854814308263405"))
        val nonceKey1 = ECKey.fromPrivate(BigInteger("514451593258031455215956018794650590333274290798379324717376700610851698631"))
        val nonceKey2 = ECKey.fromPrivate(BigInteger("11956400277919736063286645919884525832160975522703242034429953595835464801432"))
        val R_agg = MuSig.aggregate_schnorr_nonces(listOf(nonceKey1, nonceKey2)).first

        val sighash_musig = "594dc4e841a628509c9467fdcb7361de7b7bba490bedd601d18d4ba7d752888b".hexToBytes()

        val expected = BigInteger("87702316580188192134768828685038019069684190982614451361055363232288478978335")
        val actual = MuSig.sign_musig(key1_c, nonceKey1, R_agg, agg_pubkey, sighash_musig)

        assertEquals(expected, actual)
    }

    @Test
    fun generate_musig_key() {
        val key1 = ECKey.fromPrivate(BigInteger("88218786999700320424912157840922001183470238663577897435520060565802125439712"))
        val key2 = ECKey.fromPrivate(BigInteger("11756621930195768229168784074199362003209438395325908648574429387730312779458"))

        val expected = "023dd5fc3c1766d0a73466a5997da83efcc529107c9ecd0c56e2a28519f0eb3104"
        val (cMap, actual) = MuSig.generate_musig_key(listOf(key1, key2))

        assertEquals(expected, actual.getEncoded(true).toHex())

        val expectedPrivChallenge1 = BigInteger("1831054192583883058098689099279726084283766354549572339919052854814308263405")
        val expectedPrivChallenge2 = BigInteger("102505784576051217526024106275692022993285503780642991935683834028794254508839")
        val expectedPubChallenge1 = "027018cf68d40bae2a6c2b3dcee830b2f02c11586cfbd21955ba3e6a3f1d0da05b"
        val expectedPubChallenge2 = "0288fd645aca21c03b53abbe67f5cfb62fe65da0fd9277f9b4a00b0dee64e9eb1f"

        val actualPrivChallenge1 = key1.privKey.multiply(BigInteger(1, cMap[key1])).mod(Schnorr.n)
        val actualPrivChallenge2 = key2.privKey.multiply(BigInteger(1, cMap[key2])).mod(Schnorr.n)
        val actualPubChallenge1 = key1.pubKeyPoint.multiply(BigInteger(1, cMap[key1])).getEncoded(true).toHex()
        val actualPubChallenge2 = key2.pubKeyPoint.multiply(BigInteger(1, cMap[key2])).getEncoded(true).toHex()

        assertEquals(expectedPrivChallenge1, actualPrivChallenge1)
        assertEquals(expectedPrivChallenge2, actualPrivChallenge2)
        assertEquals(expectedPubChallenge1, actualPubChallenge1)
        assertEquals(expectedPubChallenge2, actualPubChallenge2)

        val pubkeyDataMuSig = actual.getEncoded(true)

        val expectedAddress = "bcrt1pqq7atlpuzandpfe5v6jejldg8m7v22gs0j0v6rzku23g2x0savcsgwp82mv"
        val actualAddress = SegwitAddressUtil.key_to_witness(pubkeyDataMuSig)

        assertEquals(expectedAddress, actualAddress)
    }

    @Test
    fun aggregate_schnorr_nonces() {
        var key1 = ECKey.fromPrivate(BigInteger("2c68916c316d82ea8f3ebb27037354741ce080464590268780ffca750c5727f6", 16))
        var key2 = ECKey.fromPrivate(BigInteger("08d538794fcc7766ecc1e0fd9c1642e5cd5147c67807cce4d46144db5fcc8534", 16))

        var expected = "03326be2da46e2af92df7df4affacc9afdd8b0fb80e7958da65da62b6fc67883f4"
        var actual = MuSig.aggregate_schnorr_nonces(listOf(key1, key2))

        assertEquals(expected, actual.first.getEncoded(true).toHex())
        assertTrue(actual.second)

        key1 = ECKey.fromPrivate(BigInteger("01232b5623b219913420d5570fd5052538c8ee3014adfc9c8d9a73585110f7c7", 16))
        key2 = ECKey.fromPrivate(BigInteger("1a6f152e82687bd327d65dafb44c84dad1bcc605c9a128231ce0725305ffdc98", 16))

        expected = "037bdd007a2ada0fbf18fe8ea7858398e2775195db1a2cef127ef38eef861027bf"
        actual = MuSig.aggregate_schnorr_nonces(listOf(key1, key2))

        assertEquals(expected, actual.first.getEncoded(true).toHex())
        assertFalse(actual.second)
    }
}
