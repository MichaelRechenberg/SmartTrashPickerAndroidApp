package mrechenberg.smarttrashpickerapp.database

import androidx.annotation.NonNull
import androidx.room.*
import java.util.*

/**
 * Represents one instance in time and place where
 *  a user picked up a piece of trash
 */
@Entity(tableName = "TrashPickup")
@TypeConverters(DateConverter::class)
data class TrashPickup(
    // The username of the user that picked up the trash
    @NonNull
    @ColumnInfo(name = "username")
    var username : String,

    // Latitude where the user picked up the trash
    @NonNull
    @ColumnInfo(name = "latitude")
    var latitude : Double,

    // Longitude where the user picked up the trash
    @NonNull
    @ColumnInfo(name = "longitude")
    var longitude : Double,

    // UTC timestamp when the user picked up the trash
    @NonNull
    @ColumnInfo(name = "datetime_collected")
    var collectedDate : Date,

    // ID of the TrashPickingSession in which this piece of trash was collected
    @ForeignKey(entity = TrashPickingSession::class, parentColumns = ["session_id"], childColumns = ["session_id"])
    @NonNull
    @ColumnInfo(name = "session_id")
    var sessionId : Long){

        // unique identifier for this trash pickup
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "pickup_id")
        var pickupId : Long = 0
}
