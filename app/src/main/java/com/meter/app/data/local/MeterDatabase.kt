package com.meter.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.meter.app.domain.model.BillingRecord
import com.meter.app.domain.model.Meter
import com.meter.app.domain.model.MeterReading

@Database(
    entities = [
        Meter::class,
        MeterReading::class,
        BillingRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MeterDatabase : RoomDatabase() {
    abstract fun meterDao(): MeterDao
}