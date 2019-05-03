package mrechenberg.smarttrashpickerapp.database

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.*

/**
 * Entity for one trash picking session
 *
 * A session is an interval of time where
 *  someone was picking up trash (essentially
 *  a record of the uptime of the BLE connection
 *  to the Pi)
 *
 * Sessions for the same user should not have overlapping
 *  times (i.e. start_time < end_time, and for two sessions p and q
 *  for user u, p.start_time > q.end_time
 *  OR q.start_time > p.end_time)
 *
 */
@Entity(tableName = "TrashPickingSession")
@TypeConverters(DateConverter::class)
data class TrashPickingSession(

    // Username of the user whose session this is
    @NonNull
    @ColumnInfo(name = "username")
    var username : String,

    // The UTC timestamp for when this trash picking session started
    @NonNull
    @ColumnInfo(name = "start_timestamp")
    var startTime : Date,

    // The UTC timestamp for when this trash picking session ended
    // If null, then we don't know if the session is still going on or
    //   if we couldn't record the end of the session
    @ColumnInfo(name = "end_timestamp")
    var endTime : Date?){


    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "session_id")
    var sessionId : Long = 0


}





