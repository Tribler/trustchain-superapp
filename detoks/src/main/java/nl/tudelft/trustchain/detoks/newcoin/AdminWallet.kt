import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import nl.tudelft.trustchain.detoks.Token
import nl.tudelft.trustchain.detoks.db.DbHelper

data class AdminWallet(val tokens: ArrayList<Token>) {

    companion object {
        private var wallet :AdminWallet? = null
        private val dbHelper: DbHelper? = null;
        @RequiresApi(Build.VERSION_CODES.O)
        private fun create(context: Context): AdminWallet {
//            val generator = KeyPairGenerator.getInstance("RSA")
//            generator.initialize(2048)
//            val keyPair = generator.generateKeyPair()

            val dbHelper = DbHelper(context)
            val tokens = dbHelper.getAllAdminTokens()

            return AdminWallet(tokens)
        }
        @RequiresApi(Build.VERSION_CODES.O)
        fun getInstance(context: Context): AdminWallet {
            return this.wallet ?: create(context)
        }
    }

    val balance: Int get() {
        return tokens.size
    }

    fun addToken(token : Token) {
        tokens.add(token)
        dbHelper?.addToken(token)
    }

    fun removeToken(token: Token): Token{
        tokens.remove(token)
        dbHelper?.removeToken(token)
        return token
    }


    // Return the number of tokens needed to pay the value
    // For example: we need to pay 2 euros, but we have tokes of 0.50 cents, 1 euro token
    // This means we need to return either two 1 euro tokens or 4x0.50 cents tokens
    fun getPayment(value: Int): ArrayList<Token> {
        val tokensToPay = arrayListOf<Token>();
        var tempValue = 0;

        for(t in tokens) {
            if(tempValue + t.value <= value){
                tempValue = tempValue + t.value
                tokensToPay.add(removeToken(t))
            }
            if(tempValue == value) {
                break
            }
        }
        return tokensToPay
    }
}
