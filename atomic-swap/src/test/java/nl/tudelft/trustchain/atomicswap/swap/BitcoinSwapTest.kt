package nl.tudelft.trustchain.atomicswap.swap

import junit.framework.TestCase
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.BitcoinSwap
import org.bitcoinj.core.*
import org.bitcoinj.params.UnitTestParams
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.store.MemoryBlockStore
import org.bitcoinj.wallet.KeyChain
import org.bitcoinj.wallet.Wallet
import org.junit.Test

class BitcoinSwapTest {


    fun createBitcoinSwap(): BitcoinSwap {
        return BitcoinSwap(relativeLock = 1,networkParams = UnitTestParams())
    }

    fun createWallet(): Wallet {
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

        fundWallet(initiateWallet,Coin.parseCoin("10"))

        // the wallet that can claim
        val claimWallet = createWallet()
        val claimPublicKey = claimWallet.freshReceiveKey().pubKey
        val claimBitcoinSwap = createBitcoinSwap()

        val (tx, swapData) = initiateBitcoinSwap.startSwapTx(
            offerId = 0,
            wallet = initiateWallet,
            claimPubKey = claimPublicKey,
            "1"
        )

        claimWallet.commitTx(tx)

        claimBitcoinSwap.addInitialRecipientSwapdata(0,claimPublicKey,"1")

        claimBitcoinSwap.updateRecipientSwapData(0,swapData.secretHash,swapData.keyUsed,swapData.initiateTxId!!)

        val claimTx = claimBitcoinSwap.createClaimTx(tx.txId.bytes, swapData.secretUsed, 0, claimWallet)

        val result = runCatching {
            claimTx.inputs.first().verify(tx.outputs.find { it.scriptPubKey.scriptType == Script.ScriptType.P2SH })
        }


        assertTrue("Swap Tx should be claimable",result.isSuccess)

    }

}
