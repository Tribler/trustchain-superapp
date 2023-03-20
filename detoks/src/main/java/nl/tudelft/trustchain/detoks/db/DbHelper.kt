package nl.tudelft.trustchain.detoks.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

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
        db?.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_NAME TEXT, " +
                "$COLUMN_ADDRESS TEXT)"
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
}
