package nl.tudelft.trustchain.offlinedigitaleuro.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.offlinedigitaleuro.db.OfflineDigitalEuroRoomDatabase
import nl.tudelft.trustchain.offlinedigitaleuro.db.Transactions
import nl.tudelft.trustchain.offlinedigitaleuro.payloads.TransferQR
import nl.tudelft.trustchain.offlinedigitaleuro.src.Token
import nl.tudelft.trustchain.offlinedigitaleuro.src.Wallet
import org.json.JSONObject
import java.lang.Integer.min
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class TransactionUtility {

    class SendRequest(
        val oneEuroCount: Int = 0,
        val twoEuroCount: Int = 0,
        val fiveEuroCount: Int = 0,
        val tenEuroCount: Int = 0,
    )

    class DuplicatedTokens(
        private val incomingToken: Token,
        private val storedToken: Token,
    ) {
        fun getDoubleSpender() : Pair<PublicKey?, String> {
            // general check that does not include the owner before we owned the token
            // incoming - 1 because of intermediary wallet and stored - 2 because we already own it and intermediary wallet
            val minSize: Int = min(incomingToken.numRecipients - 1, storedToken.numRecipients - 2)
            for (i in 0 until minSize step 2) {
                val incomingOwner = incomingToken.recipients[i].publicKey
                val storedOwner = storedToken.recipients[i].publicKey

                if (!(incomingOwner contentEquals storedOwner)) {
                   if (i == 0) {
                       val errMsg = "Error: token with same ID but different initial owners found"
                       return Pair(null, errMsg)
                   }

                    val doubleSpender = incomingToken.recipients[i - 2].publicKey
                    return Pair(defaultCryptoProvider.keyFromPublicBin(doubleSpender), "")
                }

            }

            // the owner right before us is the double spender if we reached this point
            val doubleSpender = incomingToken.recipients[incomingToken.numRecipients - 2].publicKey
            return Pair(defaultCryptoProvider.keyFromPublicBin(doubleSpender), "")

//            no longer needed/possible to do a mistake? TODO: decide
//            val errMsg = "Error: token already in DB but no double spender was found"
//            return Pair(null, errMsg)
        }
    }

companion object {

    // completes a transaction and inserts the tokens in the DB
    // returns true if the operation succeeded and on failure also returns the message
    fun receiveTransaction(tq: TransferQR, db: OfflineDigitalEuroRoomDatabase, my_pbk: PublicKey): Pair<Boolean, String> {
        val nowOwnedTokens: MutableSet<Token> = cedeTokens(my_pbk, tq.pvk, tq.tokens.toMutableList())

        val result = TokenDBUtility.insertToken(nowOwnedTokens.toList(), db)
        val code: TokenDBUtility.Codes = result.first
        if (code != TokenDBUtility.Codes.OK) {
            val errMsg = "Error: failed to insert received tokens into the DB, reason: $code"
            Log.e("ODE", errMsg)
            return Pair(false, errMsg)
        }

        runBlocking(Dispatchers.IO) {
            db.transactionsDao().insertTransaction(
                Transactions(
                    Random.nextInt(0, 10000),
                    SimpleDateFormat("dd MMM yyyy HH:mm:ss z", Locale.getDefault()).format(Date()),
                    tq.getPreviousOwner().first?.keyToBin()!!.toHex(),
                    tq.getValue(),
                    true
                )
            )
        }

        return Pair(true, "")
    }

    // Gets the tokens from the transfer qr and checks if they are already stored in the DB
    // if NO error, returns Pair<[*], ""> -> an empty list means no duplicates
    // else, returns Pair<null, "*">
    fun getDuplicateTokens(tq: TransferQR, db: OfflineDigitalEuroRoomDatabase) : Pair<MutableList<DuplicatedTokens>?, String> {
        val ret: MutableList<DuplicatedTokens> = mutableListOf()

        for (t in tq.tokens) {
            val (code, tokenFromDb) = TokenDBUtility.findToken(t, db)
            if (code == TokenDBUtility.Codes.OK) {
                ret.add(DuplicatedTokens(t, tokenFromDb!!))
                continue
            } else if (code != TokenDBUtility.Codes.ERR_NOT_FOUND) {
                val errMsg = "Error: failed to check if token is in DB, reason: $code"
                Log.e("ODE", errMsg)
                return Pair(null, errMsg)
            }
        }

        return Pair(ret, "")
    }

    // completes the transaction by deleting the transferred tokens from the DB and some other stuff
    // returns true if everything went fine
    // otherwise returns false and the error message
    fun completeSendTransaction(sendTransaction: JSONObject, db: OfflineDigitalEuroRoomDatabase) : Pair<Boolean, String> {
        val (maybeTq, errMsg) = TransferQR.fromJson(sendTransaction)

        if (maybeTq == null) {
            val newErrMsg = "Error: Transaction JSON wrongly formatted, reason: $errMsg"
            return Pair(false, newErrMsg)
        }
        val tq: TransferQR = maybeTq

        val result: Pair<TokenDBUtility.Codes, MutableList<Token>> = TokenDBUtility.deleteTokens(tq.tokens.toList(), db)
        val code: TokenDBUtility.Codes = result.first
        if (code != TokenDBUtility.Codes.OK) {
            Log.e("ODE", "Error: failed to delete tokens from DB, reason: $code")
            return Pair(false, "Error: failed to delete tokens from DB, reason: $code")
        }

        runBlocking(Dispatchers.IO) {
            db.transactionsDao().insertTransaction(
                Transactions(
                    Random.nextInt(0, 10000),
                    SimpleDateFormat("dd MMM yyyy HH:mm:ss z", Locale.getDefault()).format(Date()),
                    tq.getPreviousOwner().first?.keyToBin()!!.toHex(),
                    -(tq.getValue()),
                    true
                )
            )
        }

        return Pair(true, "")
    }

    // get a JSON object which represents a transaction with the required tokens signed and the
    // intermediary wallet or get null and an error message
    fun getSendTransaction(req: SendRequest, db: OfflineDigitalEuroRoomDatabase, my_pvk: PrivateKey): Pair<JSONObject?, String> {
        val tokenExtractTransaction = getTokensToSend(req, db)

        val tokens: MutableList<Token> = tokenExtractTransaction.first ?: return Pair(null, tokenExtractTransaction.second)

        val intermediaryWallet = Wallet()

        val cededTokens: MutableSet<Token> = cedeTokens(intermediaryWallet.publicKey, my_pvk, tokens)

        val ret: JSONObject = TransferQR.createJson(intermediaryWallet.privateKey, cededTokens)

        return Pair(ret, "")
    }

    // extracts the required number of tokens of certain values from the DB
    // returns null and an error message on failure
    private fun getTokensToSend(req: SendRequest, db: OfflineDigitalEuroRoomDatabase): Pair<MutableList<Token>?, String> {
        val coinAndCount: List<Pair<Double, Int>> = listOf(
            Pair(1.0, req.oneEuroCount),  Pair(2.0, req.twoEuroCount),
            Pair(5.0, req.fiveEuroCount), Pair(10.0, req.tenEuroCount)
        )

        val ret: MutableList<Token> = mutableListOf()

        for (r in coinAndCount) {
            val coinVal = r.first
            val coinCount = r.second

            val result = TokenDBUtility.getTokens(coinVal, coinCount, db)

            val code = result.first
            if (code != TokenDBUtility.Codes.OK) {
                Log.e("ODE", "Error: failed to prepare to send tokens, reason: $code")
                return Pair(null, "Error: failed to prepare to send tokens, reason: $code")
            }

            ret.addAll(result.second)
        }

        return Pair(ret, "")
    }

    // change the owner of the tokens to the public key given
    private fun cedeTokens(other_pbk: PublicKey, my_pvk: PrivateKey, tokens: MutableList<Token>) : MutableSet<Token> {
        val ret: MutableSet<Token> = mutableSetOf()

        for (t in tokens) {
            t.signByPeer(other_pbk.keyToBin(), my_pvk)
            ret.add(t)
        }

        return ret
    }

} // companion object
}
