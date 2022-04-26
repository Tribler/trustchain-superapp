package nl.tudelft.trustchain.atomicswap.swap

import junit.framework.TestCase.assertTrue
import nl.tudelft.trustchain.atomicswap.BitcoinSwap
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Context
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.params.UnitTestParams
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.Wallet
import org.junit.Test

class BitcoinSwapTest {


    private fun createBitcoinSwap(): BitcoinSwap {
        return BitcoinSwap(relativeLock = 1,networkParams = UnitTestParams())
    }

    private fun createWallet(): Wallet {
        return Wallet.createDeterministic(Context(UnitTestParams()), Script.ScriptType.P2PKH)
    }

    /**
     * Creates a fake transaction that pays [amount] coins to the wallet and sets the wallet to
     * allow spending unconfirmed outputs.
     *
     * This is usefully for tests that need the wallets to have funds.
     */
    fun fundWallet(wallet: Wallet,amount:Coin){
        val fakeTx = Transaction(UnitTestParams())
        fakeTx.addInput(Sha256Hash.ZERO_HASH,0,ScriptBuilder.createEmpty())
        fakeTx.addOutput(amount,wallet.currentReceiveAddress())

        wallet.commitTx(fakeTx)
        wallet.allowSpendingUnconfirmedTransactions() // allow us to use unconfirmed txs
    }

    @Test
    fun `A swap transaction should be able to be claimed`() {
        // the wallet that can reclaim
        val initiateWallet = createWallet()
        val initiateBitcoinSwap = createBitcoinSwap()
        val initiatePublicKey = initiateWallet.freshReceiveKey().pubKey
        fundWallet(initiateWallet,Coin.parseCoin("10"))

        // the wallet that can claim
        val claimWallet = createWallet()
        val claimPublicKey = claimWallet.freshReceiveKey().pubKey
        val claimBitcoinSwap = createBitcoinSwap()


        val initiateTrade = Trade(0,TradeOfferStatus.IN_PROGRESS,Currency.BTC,"1",Currency.BTC,"1")

        initiateTrade.setOnAccept(claimPublicKey,"", initiatePublicKey,"")

        val (tx, _) = initiateBitcoinSwap.createSwapTransaction(
            initiateTrade,
            initiateWallet
        )

        claimWallet.commitTx(tx)


        val claimTrade = Trade(0,TradeOfferStatus.IN_PROGRESS,Currency.BTC,"1",Currency.BTC,"1")
        claimTrade.setOnTrade(claimPublicKey,"")
        claimTrade.setOnInitiate(initiatePublicKey,initiateTrade.secretHash!!,tx.bitcoinSerialize(),"")
        claimTrade.setOnSecretObserved(initiateTrade.secret!!)
        val claimTx = claimBitcoinSwap.createClaimTransaction(claimTrade,claimWallet)

        val result = runCatching {
            claimTx.inputs.first().verify(tx.outputs.find { it.scriptPubKey.scriptType == Script.ScriptType.P2SH })
        }


        assertTrue("Swap Tx should be claimable",result.isSuccess)

    }

}
