package com.danlee.criminalintent.database

import androidx.room.TypeConverter
import java.util.*

class CrimeTypeConverters {

    // convert Date Object to record to database
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    // convert date from database to Date Object
    @TypeConverter
    fun toDate(millisSinceEpoch: Long?): Date? {
        return millisSinceEpoch?.let {
            Date(it)
        }
    }

    // convert data from database to UUID Object
    @TypeConverter
    fun toUUID(uuid: String?): UUID? {
        return UUID.fromString(uuid)
    }

    // convert UUID Object to record to database
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }
}