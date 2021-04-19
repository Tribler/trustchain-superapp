package nl.tudelft.trustchain.liquidity.data

import kotlinx.coroutines.delay
import nl.tudelft.trustchain.liquidity.data.ethereum.contracts.geth.MultiSigWallet
import org.ethereum.geth.*

class EthereumGethMultiSigWallet(gethWallet: EthereumGethWallet) {
    var singleGethWallet: EthereumGethWallet
    var gethNode: Node
    private var contractBound: Boolean = false
    lateinit var boundMultiSigWallet: MultiSigWallet

    init {
        singleGethWallet = gethWallet
        gethNode = gethWallet.node
        // val transactOpts = TransactOpts()
        // transactOpts.setContext(wallet.context)
        // transactOpts.from = wallet.account.address
        // transactOpts.gasLimit = 8000000
        // transactOpts.gasPrice = BigInt(2)
        // transactOpts.setSigner(MySigner(wallet.account, wallet.keyStore, wallet.password, BigInt(wallet.nodeConfig.ethereumNetworkID)))
        // transactOpts.nonce = wallet.node.ethereumClient.getPendingNonceAt(wallet.context, wallet.account.address)

        // val ownersAddresses = Addresses()
        // ownersAddresses.append(wallet.account.address)
        // instance = MultiSigWallet.deploy(transactOpts, wallet.node.ethereumClient, ownersAddresses, BigInt(1))
    }

    private suspend fun bindContract() {
        // if contract already bound, don't do anything
        if (contractBound) return
        // else, let's bind!
        val contractAddressHex = "0x2DfEecF7d2f5363149cC73cAb96C00575c732170"
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
