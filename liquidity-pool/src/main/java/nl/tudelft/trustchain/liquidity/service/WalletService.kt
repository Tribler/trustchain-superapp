package nl.tudelft.trustchain.liquidity.service

import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.PeerAddress
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.math.BigDecimal
import java.net.InetAddress
import java.net.URL

object WalletService {
    private const val bitcoinFaucetEndpoint = "http://134.122.59.107:3000"
    val params = RegTestParams.get()

    fun createPersonalWallet(dir: File): WalletAppKit =
        createWallet(dir, "personal")

    fun createMultiSigWallet(dir: File): WalletAppKit =
        createWallet(dir, "multi-sig")

    fun createWallet(dir: File, name: String): WalletAppKit {
        val app = object : WalletAppKit(params, dir, name) {
            override fun onSetupCompleted() {
                if (wallet().keyChainGroupSize < 1) {
                    wallet().importKey(ECKey())
                }

                if (wallet().balance.isZero) {
                    val address = wallet().issuedReceiveAddresses.first().toString()
                    URL("$bitcoinFaucetEndpoint?id=$address").readBytes()
                }
            }
        }
        val localHost = InetAddress.getByName("134.122.59.107")
        app.setPeerNodes(PeerAddress(params, localHost, params.port))
        app.setAutoSave(true)
        app.setBlockingStartup(false)
        app.startAsync()
        app.awaitRunning()
        return app
    }
}
