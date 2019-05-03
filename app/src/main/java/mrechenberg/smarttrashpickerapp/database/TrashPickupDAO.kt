package mrechenberg.smarttrashpickerapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TrashPickupDAO {

    @Insert
    fun insert(trashPickup: TrashPickup) : Long

    @Query("SELECT * FROM TrashPickup WHERE session_id = :sessionId")
    fun getAllPickupsOfSession(sessionId: Long) : List<TrashPickup>

    @Query("SELECT COUNT(*) FROM TrashPickup WHERE session_id = :sessionId")
    fun getPickupCountOfSession(sessionId: Long) : Int
}