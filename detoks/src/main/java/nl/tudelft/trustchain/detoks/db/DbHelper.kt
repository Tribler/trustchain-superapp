package nl.tudelft.trustchain.detoks.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import nl.tudelft.trustchain.detoks.Token

class DbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "database.db"
        const val DATABASE_VERSION = 1

        const val TABLE_NAME = "friends"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_ADDRESS = "address"

        const val TABLE_TOKEN ="tokens"
        const val COLUMN_TOKEN_ID = "token_id"
        const val COLUMN_VALUE = "token_value"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_NAME TEXT NOT NULL, " +
                "$COLUMN_ADDRESS TEXT NOT NULL)"
        )

        db?.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_TOKEN (" +
//                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_TOKEN_ID INTEGER PRIMARY KEY NOT NULL UNIQUE, " +
                "$COLUMN_VALUE INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Handle database schema upgrades here if needed
    }

    fun addFriend(name: String, address: String): Long {
        val values = ContentValues()
        values.put(COLUMN_NAME, name)
        values.put(COLUMN_ADDRESS, address)

        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $COLUMN_NAME=? OR $COLUMN_ADDRESS=?", arrayOf(name, address))
        if (cursor.count > 0) {
            cursor.close()
            db.close()
            return -1 // Return -1 to indicate that the friend already exists in the table
        }
        val newRowId = db.insert(TABLE_NAME, null, values)

        db.close()

        return newRowId
    }

    fun getAllFriends(): List<String> {
        val friendList = mutableListOf<String>()
        val selectQuery = "SELECT $COLUMN_NAME FROM $TABLE_NAME"

        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            val nameColumnIndex = cursor.getColumnIndex(COLUMN_NAME)
            if (nameColumnIndex >= 0) {
                do {
                    val name = cursor.getString(nameColumnIndex)
                    friendList.add(name)
                } while (cursor.moveToNext())
            }
        }

        cursor.close()
        db.close()

        return friendList
    }

    fun addToken(token: Token){
        val new_row = ContentValues()
        new_row.put(COLUMN_TOKEN_ID, token.id)
        new_row.put(COLUMN_VALUE, token.value)

        val db = this.writableDatabase
//        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME_TOKEN WHERE $COLUMN_TOKEN_ID=? OR $COLUMN_VALUE=?",
//            arrayOf(token_id, value.toString()))
        db.insert(TABLE_TOKEN, null, new_row) // null?
        db.close()
    }


    fun removeToken(token: Token) {
        val db = this.writableDatabase
        db.delete(
            TABLE_TOKEN,
            COLUMN_TOKEN_ID + " = ?",
            arrayOf<String>(java.lang.String.valueOf(token.id))
        )
        db.close()
    }

    //returns the id and the value of the token
    fun getAllTokens(): List<Pair<Int, Int>>{
        val tokenList = mutableListOf<Pair<Int, Int>>()
        val selectQueryId = "SELECT $COLUMN_TOKEN_ID FROM $TABLE_TOKEN"
        val selectQueryValue = "SELECT $COLUMN_TOKEN_ID FROM $TABLE_TOKEN"

        val db = this.readableDatabase

        val cursorId = db.rawQuery(selectQueryId, null)
        val cursorValue = db.rawQuery(selectQueryValue, null)

        if (cursorId.moveToFirst() && cursorValue.moveToFirst()) {
            val columnIdIndex = cursorId.getColumnIndex(COLUMN_TOKEN_ID)
            val columnValueIndex = cursorValue.getColumnIndex(COLUMN_VALUE)
            if (columnIdIndex >= 0 && columnValueIndex >= 0) {
                do {
                    val token_id = cursorId.getInt(columnIdIndex)
                    val token_value = cursorValue.getInt(columnValueIndex)
                    tokenList.add(Pair(token_id, token_value))
                } while (cursorId.moveToNext() && cursorValue.moveToNext())
            }
        }
        db.close()
        return tokenList

    }

    //update token if we keep the token when the owner changes?
}
