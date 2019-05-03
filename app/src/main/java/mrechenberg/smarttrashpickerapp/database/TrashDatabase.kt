package mrechenberg.smarttrashpickerapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Database for trash entities
 */
@Database(entities = [TrashPickup::class, TrashPickingSession::class],
    version = 4)
abstract class TrashDatabase : RoomDatabase() {


    abstract fun getTrashPickingSessionDAO() : TrashPickingSessionDAO
    abstract fun getTrashPickupDAO() : TrashPickupDAO


    companion object {

        // Implement the Singleton pattern as per https://codelabs.developers.google.com/codelabs/android-room-with-a-view/#6
        private var INSTANCE : TrashDatabase? = null

        /**
         * Acquire singleton for the TrashDatabase
         */
        fun getDatabase(context : Context) : TrashDatabase {
            if (INSTANCE == null)
                synchronized(TrashDatabase::class.java){
                    if(INSTANCE == null){
                        INSTANCE = Room.databaseBuilder(
                                context.applicationContext,
                                TrashDatabase::class.java,
                            "trash_database")
                            .build()
                }
            }

            return INSTANCE as TrashDatabase
        }
    }


}