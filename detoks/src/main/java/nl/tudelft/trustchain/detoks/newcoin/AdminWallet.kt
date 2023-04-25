import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import nl.tudelft.trustchain.detoks.Token
import nl.tudelft.trustchain.detoks.db.DbHelper

data class AdminWallet(
    val publicKey: String
) {

    companion object {
        private var wallet :AdminWallet? = null
        private var tokens : MutableList<Token>? = null
        private var dbHelper: DbHelper? = null
        @RequiresApi(Build.VERSION_CODES.O)
        private fun create(context: Context): AdminWallet {
//            val generator = KeyPairGenerator.getInstance("RSA")
//            generator.initialize(2048)
//            val keyPair = generator.generateKeyPair()

            dbHelper = DbHelper(context)
            tokens = dbHelper!!.getAllTokens(admin = true)
            wallet = AdminWallet("s")

            return wallet!!
        }
        @RequiresApi(Build.VERSION_CODES.O)
        fun getInstance(context: Context): AdminWallet {
            return this.wallet ?: create(context)
        }
    }

    fun getTokens(): MutableList<Token> {
        return tokens!!
    }

    val balance: Int get() {
        return tokens!!.size
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addToken(token : Token) {
        val result = dbHelper?.addToken(token, admin = true)
        if (result != -1L) {
            tokens!!.add(token)
        }
    }
//    return result
}
