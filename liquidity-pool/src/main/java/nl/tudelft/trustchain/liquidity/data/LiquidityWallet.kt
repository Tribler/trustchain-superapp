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
    fun initializePool()
}
