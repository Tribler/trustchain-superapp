package nl.tudelft.trustchain.offlinedigitaleuro.db

import androidx.room.*

@Entity(tableName = "userdata_table")
data class UserData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "public_key") val public_key: String,
    @ColumnInfo(name = "private_key") val private_key: String,
)

@Dao
interface UserDao {

    @Query("SELECT * FROM userdata_table")
    fun getUserData(): List<UserData>

    @Insert
    suspend fun insertUser(userData: UserData)

    @Update
    suspend fun updateUser(userData: UserData)

    @Delete
    suspend fun deleteUserData(userData: UserData)
}
