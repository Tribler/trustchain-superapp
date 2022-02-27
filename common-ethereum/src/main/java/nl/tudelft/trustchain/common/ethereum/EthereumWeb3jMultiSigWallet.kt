package nl.tudelft.trustchain.common.ethereum

import nl.tudelft.trustchain.common.ethereum.contracts.web3j.MultiSigWallet
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.tx.gas.StaticGasProvider
import java.math.BigInteger

class EthereumWeb3jMultiSigWallet(val web3j: Web3j, val address: String, wallet: EthereumWeb3jWallet) {
    var credentials = wallet.credentials
    var contractGasProvider = StaticGasProvider(BigInteger.valueOf(4), BigInteger.valueOf(8000000))
    private var contractBound: Boolean = false
    lateinit var boundMultiSigWallet: MultiSigWallet

    fun address(): String {
        return boundMultiSigWallet.contractAddress
    }

    suspend fun balance(): BigInteger {
        ensureContractBound()
        return web3j.ethGetBalance(boundMultiSigWallet.contractAddress, DefaultBlockParameter.valueOf("latest")).sendAsync().get().balance
    }

    suspend fun withdraw(destination: String, value: BigInteger): String {
        if (value > balance()) return ""
        val receipt = boundMultiSigWallet.submitTransaction(destination, value, byteArrayOf()).sendAsync().get()
        return receipt.transactionHash
    }

    fun bindContract() {
        // If contract already bound, don't do anything
        if (contractBound) return
        boundMultiSigWallet = MultiSigWallet(address, web3j, credentials, contractGasProvider)
        contractBound = true
    }

    private suspend fun ensureContractBound() {
        if (!contractBound) bindContract()
    }
}
