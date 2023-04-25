package nl.tudelft.trustchain.detoks.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import androidx.annotation.RequiresApi
import nl.tudelft.trustchain.detoks.Token
import nl.tudelft.trustchain.detoks.Token.Companion.serialize
import nl.tudelft.trustchain.detoks.newcoin.OfflineFriend
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_SER_TOKEN = "serialized_token"


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
                "$COLUMN_TIMESTAMP TEXT NOT NULL, " +
                "$COLUMN_SER_TOKEN BLOB NOT NULL UNIQUE); ",
            )

        db?.execSQL("DROP TABLE IF EXISTS $TABLE_ADMIN_TOKEN")
        db?.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_ADMIN_TOKEN (" +
                "$COLUMN_TOKEN_ID BLOB NOT NULL, " +
                "$COLUMN_TIMESTAMP TEXT NOT NULL, " +
                "$COLUMN_SER_TOKEN BLOB NOT NULL UNIQUE," +
                "PRIMARY KEY ($COLUMN_TOKEN_ID, $COLUMN_TIMESTAMP)); ",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Handle database schema upgrades here if needed
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addToken(token: Token, admin: Boolean = false) : Long {

        val  formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTimestamp: String = token.timestamp.format(formatter)

        val new_row = ContentValues()
        new_row.put(COLUMN_TOKEN_ID, token.id)
        new_row.put(COLUMN_TIMESTAMP, formattedTimestamp)
        val collection = mutableListOf<Token>(token)
        new_row.put(COLUMN_SER_TOKEN, serialize(collection))

        var table = TABLE_TOKEN
        if (admin) {
            table = TABLE_ADMIN_TOKEN
        }

        val db = this.writableDatabase

        val newRowId = db.insert(table, null, new_row) // null?
        db.close()
        return newRowId
    }

    /**
     * Removes the specified token from the DB
     */
    fun removeToken(token: Token, admin: Boolean = false) {
        var table = TABLE_TOKEN
        if (admin) {
            table = TABLE_ADMIN_TOKEN
        }

        val db = this.writableDatabase
        db.execSQL("DELETE FROM " + table +" WHERE "+ COLUMN_TOKEN_ID +"=?",
            arrayOf<ByteArray>(token.id))
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
        val selectQueryId = "SELECT $COLUMN_TIMESTAMP, $COLUMN_SER_TOKEN FROM $table"


        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQueryId, null)

        if (cursor.moveToFirst()) {
            val columnIdIndex = cursor.getColumnIndexOrThrow(COLUMN_SER_TOKEN)
            val columnTimeIndex = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            if (columnIdIndex >= 0 && columnTimeIndex >= 0) {
                do {
                    val token = cursor.getBlob(columnIdIndex)
                    val timestamp = cursor.getString(columnTimeIndex)
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val dateTime: LocalDateTime = LocalDateTime.parse(timestamp, formatter)

                    val tokens = Token.deserialize(token, dateTime)
                    if(tokens.size == 1)
                        tokenList.add(tokens.elementAt(0))

//                    else error ?
                } while (cursor.moveToNext())
            }
        }
        cursor.close()
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
