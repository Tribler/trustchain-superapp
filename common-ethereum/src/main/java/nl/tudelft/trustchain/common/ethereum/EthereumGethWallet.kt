package nl.tudelft.trustchain.common.ethereum

import org.ethereum.geth.*

class EthereumGethWallet(val nodeConfig: NodeConfig, keyFile: String, nodeDirectory: String, keyStoreDirectory: String, val password: String) {
    val context: Context = Geth.newContext()
    val node: Node
    val keyStore: KeyStore
    val account: Account

    init {
        node = Geth.newNode(nodeDirectory, nodeConfig)
        node.start()

        // Initialize the keystore.
        keyStore = KeyStore(
            keyStoreDirectory,
            Geth.LightScryptN,
            Geth.LightScryptP
        )

        // Remove all existing accounts.
        for (i in keyStore.accounts.size() - 1 downTo 0) {
            val account = keyStore.accounts.get(i)
            keyStore.deleteAccount(account, password)
        }

        val key = keyFile.toByteArray(Charsets.ISO_8859_1)
        account = keyStore.importKey(key, password, password)
    }

    fun getAddress(): Address {
        return account.address
    }

    fun getBalance(): BigInt? {
        return node.ethereumClient.getBalanceAt(context, getAddress(), -1)
    }

    fun sendTo(address: Address, amount: BigInt) {
        val transaction = Transaction(
            node.ethereumClient.getPendingNonceAt(context, getAddress()), // Nonce.
            address, // Receive address.
            amount, // Amount.
            10000000, // Gas limit.
            BigInt(1), // Gas price.
            ByteArray(0)
        ) // Data.
        val signedTransaction = sign(transaction)
        send(signedTransaction)
    }

    fun send(transaction: Transaction) {
        node.ethereumClient.sendTransaction(context, transaction)
    }

    fun sign(transaction: Transaction): Transaction {
        return keyStore.signTxPassphrase(account, password, transaction, BigInt(4))
    }
}
