package nl.tudelft.trustchain.offlinemoney.db

import androidx.annotation.WorkerThread
import androidx.room.*
import kotlinx.coroutines.flow.Flow

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

//    @Query("DELETE FROM userdata_table")
//    suspend fun deleteUserData()

}
