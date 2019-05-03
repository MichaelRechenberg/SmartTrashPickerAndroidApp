package mrechenberg.smarttrashpickerapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TrashPickingSessionDAO {

    @Query("SELECT * FROM TrashPickingSession WHERE session_id = :id")
    fun getTrashPickingSession(id : Long) : TrashPickingSession

    @Query("SELECT * FROM TrashPickingSession WHERE username = :username ORDER BY start_timestamp DESC LIMIT 1")
    fun getMostRecentTrashPickingSessionOfUser(username : String) : TrashPickingSession

    // Returns the autogenerated session_id
    @Insert
    fun insert(session : TrashPickingSession) : Long
}