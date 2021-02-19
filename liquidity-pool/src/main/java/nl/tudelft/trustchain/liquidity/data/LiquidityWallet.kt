package nl.tudelft.trustchain.liquidity.data

interface LiquidityWallet {
    /**
     * The name of the coin that is held by this liquidity wallet.
     */
    val coinName: String

    /**
     * Initializes the wallet using the parent liquidity pool. This method is meant to set up event
     * listeners for underlying wallets and the actions to perform upon receiving coins.
     */
    fun initializePool(pool: LiquidityPool)

    /**
     * Starts a single transaction of the given amount of coins to the given address. This transaction
     * does not need to immediately be finished after this method finishes. In fact, usually this is
     * not the case with multi-signature wallets.
     */
    fun startTransaction(amount: Double, address: String)
}
