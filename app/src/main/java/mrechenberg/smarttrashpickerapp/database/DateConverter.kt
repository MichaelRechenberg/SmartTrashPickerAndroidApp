package mrechenberg.smarttrashpickerapp.database

import androidx.room.TypeConverter
import java.util.*

/**
 * Convert between Java date objects to Long timestamp equivalents
 *  (number of milliseconds since Linux epoch) for Room
 */
class DateConverter {

    companion object {
        @TypeConverter
        @JvmStatic
        fun fromTimestamp(value : Long?) : Date? {
            return if (value == null) null else Date(value)
        }

        @TypeConverter
        @JvmStatic
        fun toDate(date : Date?) : Long? {
            return date?.time
        }
    }

}