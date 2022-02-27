package nl.tudelft.trustchain.common.ethereum

import kotlinx.coroutines.delay
import nl.tudelft.trustchain.common.ethereum.BuildConfig
import nl.tudelft.trustchain.common.ethereum.contracts.geth.MultiSigWallet
import org.ethereum.geth.*

class EthereumGethMultiSigWallet(gethWallet: EthereumGethWallet) {
    var singleGethWallet: EthereumGethWallet
    var gethNode: Node
    private var contractBound: Boolean = false
    lateinit var boundMultiSigWallet: MultiSigWallet

    init {
        singleGethWallet = gethWallet
        gethNode = gethWallet.node
    }

    private suspend fun bindContract() {
        // if contract already bound, don't do anything
        if (contractBound) return
        // else, let's bind!
        val contractAddressHex = BuildConfig.ETH_TEST_MULTISIG_ADDR
        val contractAddress = Geth.newAddressFromHex(contractAddressHex)
        // wait until Geth node has at least 1 peer
        while (!nodeConnected()) {
            delay(200L)
        }
        boundMultiSigWallet =
            MultiSigWallet(
                contractAddress,
                singleGethWallet.node.ethereumClient
            )
        contractBound = true
    }

    private fun nodeConnected(): Boolean {
        return gethNode.peersInfo.size() > 0L
    }

    class MySigner(private val account: Account?, private val keyStore: KeyStore?, private val password: String?, private val chainId: BigInt?) : Signer {
        override fun sign(address: Address?, transaction: Transaction?): Transaction {
            return keyStore!!.signTxPassphrase(account, password, transaction, chainId)
        }
    }
}
