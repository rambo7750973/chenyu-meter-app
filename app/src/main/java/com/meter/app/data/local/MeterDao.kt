package com.meter.app.data.local

import androidx.room.*
import com.meter.app.domain.model.BillingRecord
import com.meter.app.domain.model.Meter
import com.meter.app.domain.model.MeterReading
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterDao {

    @Query("SELECT * FROM meters ORDER BY lastSeen DESC")
    fun getAllMeters(): Flow<List<Meter>>

    @Query("SELECT * FROM meters WHERE id = :meterId")
    fun getMeterById(meterId: String): Flow<Meter?>

    @Query("SELECT * FROM meters WHERE macAddress = :macAddress LIMIT 1")
    suspend fun getMeterByMac(macAddress: String): Meter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeter(meter: Meter)

    @Update
    suspend fun updateMeter(meter: Meter)

    @Delete
    suspend fun deleteMeter(meter: Meter)

    @Query("UPDATE meters SET isOnline = :isOnline, lastSeen = :timestamp WHERE id = :meterId")
    suspend fun updateMeterStatus(meterId: String, isOnline: Boolean, timestamp: Long)

    @Query("SELECT * FROM meter_readings WHERE meterId = :meterId ORDER BY timestamp DESC LIMIT :limit")
    fun getMeterReadings(meterId: String, limit: Int = 100): Flow<List<MeterReading>>

    @Query("SELECT * FROM meter_readings WHERE meterId = :meterId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getMeterReadingsByTimeRange(meterId: String, startTime: Long, endTime: Long): Flow<List<MeterReading>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeterReading(reading: MeterReading)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeterReadings(readings: List<MeterReading>)

    @Query("SELECT * FROM meter_readings WHERE meterId = :meterId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReading(meterId: String): Flow<MeterReading?>

    @Query("SELECT SUM(dailyEnergy) FROM meter_readings WHERE meterId = :meterId AND timestamp >= :startOfDay")
    suspend fun getTodayEnergy(meterId: String, startOfDay: Long): Float?

    @Query("SELECT SUM(dailyEnergy) FROM meter_readings WHERE meterId = :meterId AND timestamp >= :startOfMonth")
    suspend fun getMonthEnergy(meterId: String, startOfMonth: Long): Float?

    @Query("SELECT SUM(dailyEnergy) FROM meter_readings WHERE meterId = :meterId AND timestamp >= :startOfYear")
    suspend fun getYearEnergy(meterId: String, startOfYear: Long): Float?

    @Query("SELECT * FROM billing_records WHERE meterId = :meterId ORDER BY startTime DESC")
    fun getBillingRecords(meterId: String): Flow<List<BillingRecord>>

    @Query("SELECT * FROM billing_records WHERE meterId = :meterId AND startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    fun getBillingRecordsByTimeRange(meterId: String, startTime: Long, endTime: Long): Flow<List<BillingRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillingRecord(record: BillingRecord)

    @Update
    suspend fun updateBillingRecord(record: BillingRecord)

    @Query("SELECT SUM(totalCost) FROM billing_records WHERE meterId = :meterId AND isPaid = 0")
    suspend fun getUnpaidAmount(meterId: String): Float?

    @Query("SELECT SUM(totalCost) FROM billing_records WHERE meterId = :meterId AND isPaid = 1")
    suspend fun getPaidAmount(meterId: String): Float?

    @Query("UPDATE billing_records SET isPaid = 1, paidAt = :paidAt WHERE id = :recordId")
    suspend fun markRecordAsPaid(recordId: String, paidAt: Long)

    @Query("SELECT * FROM billing_records WHERE id = :recordId")
    suspend fun getBillingRecordById(recordId: String): BillingRecord?
}