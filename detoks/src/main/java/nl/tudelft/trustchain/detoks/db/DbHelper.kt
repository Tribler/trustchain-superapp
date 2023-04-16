package nl.tudelft.trustchain.detoks.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.common.primitives.Ints
import nl.tudelft.trustchain.detoks.Token
import nl.tudelft.trustchain.detoks.Token.Companion.serialize
import nl.tudelft.trustchain.detoks.newcoin.OfflineFriend

class DbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "database.db"
        const val DATABASE_VERSION = 1

        const val TABLE_NAME = "friends"
        const val COLUMN_NAME = "name"
        const val COLUMN_ADDRESS = "address"

        const val TABLE_ADMIN_TOKEN = "admin_tokens"

        const val TABLE_TOKEN = "tokens"
        const val COLUMN_TOKEN_ID = "token_id"
        const val COLUMN_VALUE = "token_value"
        const val COLUMN_VERIFIER = "verifier"
        const val COLUMN_GEN_HASH = "genesis_block"
        const val COLUMN_SER_TOKEN = "serialized_token"

        const val TABLE_RECIPIENTS = "recipients"
        const val COLUMN_RECIPIENT = "rec_publicKey"
        const val COLUMN_PROOF = "proof"

    }

    override fun onCreate(db: SQLiteDatabase?) {

        db?.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$COLUMN_NAME TEXT PRIMARY KEY, " +
                "$COLUMN_ADDRESS BLOB NOT NULL UNIQUE)"
        )
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_TOKEN")
        db?.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_TOKEN (" +
                "$COLUMN_TOKEN_ID BLOB PRIMARY KEY, " +
                "$COLUMN_VALUE BLOB NOT NULL, " +
                "$COLUMN_VERIFIER BLOB NOT NULL, " +
                "$COLUMN_GEN_HASH BLOB NOT NULL, " +
                "$COLUMN_SER_TOKEN BLOB NOT NULL UNIQUE); ",
            )

        db?.execSQL("DROP TABLE IF EXISTS $TABLE_ADMIN_TOKEN")
        db?.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_ADMIN_TOKEN (" +
                "$COLUMN_TOKEN_ID BLOB PRIMARY KEY, " +
                "$COLUMN_VALUE BLOB NOT NULL, " +
                "$COLUMN_VERIFIER BLOB NOT NULL, " +
                "$COLUMN_GEN_HASH BLOB NOT NULL, " +
                "$COLUMN_SER_TOKEN BLOB NOT NULL UNIQUE); ",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Handle database schema upgrades here if needed
    }

    fun addToken(token: Token, admin: Boolean = false) : Long {
        val new_row = ContentValues()
        new_row.put(COLUMN_TOKEN_ID, token.id)
        new_row.put(COLUMN_VALUE, token.value)
        new_row.put(COLUMN_VERIFIER, token.verifier)
        new_row.put(COLUMN_GEN_HASH, token.genesisHash)
        val collection = mutableListOf<Token>(token)
        new_row.put(COLUMN_SER_TOKEN, serialize(collection))

        var table = TABLE_TOKEN
        if (admin) {
            table = TABLE_ADMIN_TOKEN
        }

        val db = this.writableDatabase
//        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME_TOKEN WHERE $COLUMN_TOKEN_ID=? OR $COLUMN_VALUE=?",
//            arrayOf(token_id, value.toString()))
        val newRowId = db.insert(table, null, new_row) // null?
        db.close()
        return newRowId
    }

    /**
     * Removes the specified token from the DB
     */
    fun removeToken(token: Token) {
        var table = TABLE_TOKEN

        val db = this.writableDatabase
        db.delete(
            table,
            COLUMN_TOKEN_ID + " = ?",
            arrayOf<String>(java.lang.String.valueOf(token.id)),
        )
        db.close()
    }

    //returns the id and the value of the token
    @RequiresApi(Build.VERSION_CODES.O)
    fun getAllTokens(admin: Boolean = false): ArrayList<Token>{
        var table = TABLE_TOKEN
        if (admin) {
            table = TABLE_ADMIN_TOKEN
        }

        val tokenList = arrayListOf<Token>()
        val selectQueryId = "SELECT $COLUMN_SER_TOKEN FROM $table"

        val db = this.readableDatabase
        val cursorId = db.rawQuery(selectQueryId, null)

        if (cursorId.moveToFirst()) {
            val columnIdIndex = cursorId.getColumnIndex(COLUMN_SER_TOKEN)

            if (columnIdIndex >= 0) {
                do {
                    val token = cursorId.getBlob(columnIdIndex)
                    val tokens = Token.deserialize(token)
                    if(tokens.size == 1)
                        tokenList.add(tokens.elementAt(0))
//                    else error ?
                } while (cursorId.moveToNext())
            }
        }
        cursorId.close()
        db.close()
        return tokenList
    }

    fun addFriend(name: String, address: ByteArray): Long {
        val values = ContentValues()
        values.put(COLUMN_NAME, name)
        values.put(COLUMN_ADDRESS, address)

        val db = this.writableDatabase
        val newRowId = db.insert(TABLE_NAME, null, values)

        db.close()

        return newRowId
    }

    fun getAllFriends(): MutableList<OfflineFriend> {
        val friendList = mutableListOf<OfflineFriend>()
        val selectQuery = "SELECT $COLUMN_NAME,$COLUMN_ADDRESS FROM $TABLE_NAME"

        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            val nameColumnIndex = cursor.getColumnIndex(COLUMN_NAME)
            val addressColumnIndex = cursor.getColumnIndex(COLUMN_ADDRESS)
            if (nameColumnIndex >= 0 && addressColumnIndex >= 0) {
                do {
                    val name = cursor.getString(nameColumnIndex)
                    val address = cursor.getBlob(addressColumnIndex)
                    friendList.add(OfflineFriend(name, address))
                } while (cursor.moveToNext())
            }
        }

        cursor.close()
        db.close()

        return friendList
    }

    fun getFriendsPublicKey(friendName: String): ByteArray {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            arrayOf("address"),
            "name = ?",
            arrayOf(friendName),
            null,
            null,
            null
        )

        var address: ByteArray = byteArrayOf(0x48, 0x65, 0x6c, 0x6c, 0x6f)
        if (cursor.moveToFirst()) {
            val nameColumnIndex = cursor.getColumnIndex(COLUMN_ADDRESS)
            if (nameColumnIndex >= 0) {
                address = cursor.getBlob(nameColumnIndex)
            }
        }
        cursor.close()
        db.close()
        return address
    }
}
