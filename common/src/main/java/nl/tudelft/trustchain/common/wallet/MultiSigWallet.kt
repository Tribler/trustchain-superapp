package nl.tudelft.trustchain.common.wallet

interface MultiSigWallet {
    /**
     * The name of the coin that is held by this liquidity wallet.
     */
    val coinName: String

    /**
     * Starts a single transaction of the given amount of coins to the given address. This transaction
     * does not need to immediately be finished after this method finishes. In fact, usually this is
     * not the case with multi-signature wallets.
     */
    fun startTransaction(amount: Double, address: String)
}
