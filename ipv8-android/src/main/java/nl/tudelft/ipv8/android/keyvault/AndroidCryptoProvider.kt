package nl.tudelft.ipv8.android.keyvault

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import nl.tudelft.ipv8.keyvault.*

private val lazySodium = LazySodiumAndroid(SodiumAndroid())

object AndroidCryptoProvider : CryptoProvider {
    override fun generateKey(): PrivateKey {
        return LibNaClSK.generate(lazySodium)
    }

    override fun keyFromPublicBin(bin: ByteArray): PublicKey {
        return LibNaClPK.fromBin(bin, lazySodium)
    }

    override fun keyFromPrivateBin(bin: ByteArray): PrivateKey {
        return LibNaClSK.fromBin(bin, lazySodium)
    }
}
