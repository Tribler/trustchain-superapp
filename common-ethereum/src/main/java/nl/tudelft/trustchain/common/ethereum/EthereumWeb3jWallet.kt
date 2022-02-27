package nl.tudelft.trustchain.common.ethereum

import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Numeric
import java.io.File
import java.math.BigInteger

class EthereumWeb3jWallet(
    val web3j: Web3j,
    var walletDirectory: File,
    keyPair: ECKeyPair,
    password: String
) {

    val credentials: Credentials

    init {
        val fileName = WalletUtils.generateWalletFile(password, keyPair, walletDirectory, false)
        walletDirectory = File(walletDirectory.absolutePath + "/" + fileName)
        credentials = WalletUtils.loadCredentials(password, walletDirectory)
    }

    fun address(): String {
        return credentials.address
    }

    fun balance(): BigInteger {
        return web3j.ethGetBalance(credentials.address, DefaultBlockParameter.valueOf("latest")).sendAsync().get().balance
    }

    fun nonce(): BigInteger {
        val ethGetTransactionCount = web3j.ethGetTransactionCount(
            address(), DefaultBlockParameterName.LATEST
        ).sendAsync().get()
        return ethGetTransactionCount.transactionCount
    }

    fun send(receiveAddress: String, value: BigInteger) {
        val rawTransaction = RawTransaction.createEtherTransaction(
            nonce(),
            BigInteger.valueOf(4),
            BigInteger.valueOf(8000000),
            receiveAddress,
            value
        )
        val signedMessage = Numeric.toHexString(
            TransactionEncoder.signMessage(
                rawTransaction,
                credentials
            )
        )
        web3j.ethSendRawTransaction(signedMessage).sendAsync().get()
    }
}
