package nl.tudelft.ipv8.keyvault

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava

private val lazySodium = LazySodiumJava(SodiumJava())

object JavaCryptoProvider : CryptoProvider {
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
