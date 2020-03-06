package nl.tudelft.ipv8.android.demo.coin

import com.google.common.base.Joiner
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.Wallet
import org.junit.Test
import java.io.File

class WalletManagerTest {

    @Test
    fun createMultiSignatureWallet() {
        println("Start test.")
        val walletManager = WalletManager(
            WalletManagerConfiguration(),
            File(".")
        )

        Thread.sleep(5000)

        println("Post-init.")

        val wallet = walletManager.kit.wallet()

        walletManager.toSeed()

        println("Current receive address")
        println(wallet.currentReceiveAddress())
        println("Protocol address:")
        println(wallet.issuedReceiveAddresses[0])
        println("Balances:")
        println(wallet.getBalance(Wallet.BalanceType.ESTIMATED_SPENDABLE))
        println(wallet.getBalance(Wallet.BalanceType.ESTIMATED))
        println(wallet.getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE))
        println(wallet.getBalance(Wallet.BalanceType.AVAILABLE))
        println(wallet.toString())

    }
}
