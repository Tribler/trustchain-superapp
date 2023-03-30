package nl.tudelft.trustchain.detoks.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import nl.tudelft.trustchain.detoks.newcoin.OfflineFriend

class DbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "database.db"
        const val DATABASE_VERSION = 1

        const val TABLE_NAME = "friends"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_ADDRESS = "address"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        db?.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$COLUMN_NAME TEXT PRIMARY KEY, " +
                "$COLUMN_ADDRESS BLOB NOT NULL UNIQUE)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Handle database schema upgrades here if needed
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
}
