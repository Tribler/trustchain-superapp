package nl.tudelft.trustchain.offlinedigitaleuro.db

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.offlinedigitaleuro.src.Token
import nl.tudelft.trustchain.offlinedigitaleuro.db.Token as DBToken

class DBUtility {

    suspend fun receive(token: Token, ctx: Context) {
        val db by lazy { OfflineDigitalEuroRoomDatabase.getDatabase(ctx) }
        db.tokensDao().insertToken(DBToken(token.id.toHex(), token.value.toDouble(), Token.serialize(mutableSetOf(token))))
        for (token_data in Token.deserialize(Token.serialize(mutableSetOf(token)))) {
            Log.i("db_token", "Token_ID: ${token.id.toHex()} \t Token value: ${token.value} \t Token_serialize function: ${token_data.id.toHex()}")
            break
        }
        // Log.i("db_token", "Token_ID: ${token.id} \t Token value: ${token.value} \t Token_serialize function: ${Token.deserialize(Token.serialize(mutableSetOf(token)))}")
    }

    suspend fun delete(tokensToSend: MutableSet<Token>, ctx: Context) {
        val db by lazy { OfflineDigitalEuroRoomDatabase.getDatabase(ctx) }
        for (token in tokensToSend) {
            db.tokensDao().deleteToken(
                token.id.toHex()
            );
            Log.d("TOKEN", "delete token ${token.id.toHex()}")
        }

        val allTokens = db.tokensDao().getAllTokens()
        for (token in allTokens) {
            Log.i("db_token", "Token_ID: ${token.token_id} \t Token value: ${token.token_value}")
        }
    }
}
