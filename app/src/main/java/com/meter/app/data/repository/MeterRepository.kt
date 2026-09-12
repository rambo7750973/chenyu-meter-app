package com.meter.app.data.repository

import com.meter.app.data.local.MeterDao
import com.meter.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeterRepository @Inject constructor(
    private val meterDao: MeterDao
) {
    
    // Meter operations
    fun getAllMeters(): Flow<List<Meter>> = meterDao.getAllMeters()
    
    fun getMeterById(meterId: String): Flow<Meter?> = meterDao.getMeterById(meterId)
    
    suspend fun createMeter(
        name: String,
        address: String,
        macAddress: String,
        model: String = "",
        firmwareVersion: String = ""
    ): Meter {
        val meter = Meter(
            id = UUID.randomUUID().toString(),
            name = name,
            address = address,
            macAddress = macAddress,
            model = model,
            firmwareVersion = firmwareVersion,
            isOnline = false,
            lastSeen = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        meterDao.insertMeter(meter)
        return meter
    }
    
    suspend fun updateMeter(meter: Meter) = meterDao.updateMeter(meter)
    
    suspend fun deleteMeter(meter: Meter) = meterDao.deleteMeter(meter)
    
    suspend fun updateMeterStatus(meterId: String, isOnline: Boolean) {
        meterDao.updateMeterStatus(meterId, isOnline, System.currentTimeMillis())
    }
    
    // Reading operations
    fun getMeterReadings(meterId: String, limit: Int = 100): Flow<List<MeterReading>> {
        return meterDao.getMeterReadings(meterId, limit)
    }
    
    fun getMeterReadingsByTimeRange(meterId: String, startTime: Long, endTime: Long): Flow<List<MeterReading>> {
        return meterDao.getMeterReadingsByTimeRange(meterId, startTime, endTime)
    }
    
    suspend fun recordMeterReading(
        meterId: String,
        currentPower: Float,
        voltage: Float,
        current: Float,
        totalEnergy: Float,
        dailyEnergy: Float,
        monthlyEnergy: Float
    ): MeterReading {
        val reading = MeterReading(
            id = UUID.randomUUID().toString(),
            meterId = meterId,
            timestamp = System.currentTimeMillis(),
            currentPower = currentPower,
            voltage = voltage,
            current = current,
            totalEnergy = totalEnergy,
            dailyEnergy = dailyEnergy,
            monthlyEnergy = monthlyEnergy
        )
        meterDao.insertMeterReading(reading)
        return reading
    }
    
    fun getLatestReading(meterId: String): Flow<MeterReading?> {
        return meterDao.getLatestReading(meterId)
    }
    
    suspend fun getTodayEnergy(meterId: String): Float {
        val startOfDay = getStartOfDay()
        return meterDao.getTodayEnergy(meterId, startOfDay) ?: 0f
    }
    
    suspend fun getMonthEnergy(meterId: String): Float {
        val startOfMonth = getStartOfMonth()
        return meterDao.getMonthEnergy(meterId, startOfMonth) ?: 0f
    }
    
    suspend fun getYearEnergy(meterId: String): Float {
        val startOfYear = getStartOfYear()
        return meterDao.getYearEnergy(meterId, startOfYear) ?: 0f
    }
    
    // Billing operations
    fun getBillingRecords(meterId: String): Flow<List<BillingRecord>> {
        return meterDao.getBillingRecords(meterId)
    }
    
    suspend fun createBillingRecord(
        meterId: String,
        startTime: Long,
        endTime: Long,
        energyUsed: Float,
        unitPrice: Float,
        billingType: BillingType
    ): BillingRecord {
        val totalCost = energyUsed * unitPrice
        val record = BillingRecord(
            id = UUID.randomUUID().toString(),
            meterId = meterId,
            startTime = startTime,
            endTime = endTime,
            energyUsed = energyUsed,
            unitPrice = unitPrice,
            totalCost = totalCost,
            billingType = billingType,
            isPaid = false
        )
        meterDao.insertBillingRecord(record)
        return record
    }
    
    suspend fun updateBillingRecord(record: BillingRecord) = meterDao.updateBillingRecord(record)
    
    suspend fun markAsPaid(recordId: String) {
        val record = meterDao.getBillingRecords("").value.find { it.id == recordId }
        if (record != null) {
            meterDao.updateBillingRecord(record.copy(isPaid = true, paidAt = System.currentTimeMillis()))
        }
    }
    
    suspend fun getUnpaidAmount(meterId: String): Float {
        return meterDao.getUnpaidAmount(meterId) ?: 0f
    }
    
    suspend fun getPaidAmount(meterId: String): Float {
        return meterDao.getPaidAmount(meterId) ?: 0f
    }
    
    // Statistics
    suspend fun getMeterStats(meterId: String): MeterStats {
        val todayEnergy = getTodayEnergy(meterId)
        val monthEnergy = getMonthEnergy(meterId)
        val yearEnergy = getYearEnergy(meterId)
        
        // Calculate costs (simplified - in real app, use pricing model)
        val todayCost = todayEnergy * 0.5f // Example price
        val monthCost = monthEnergy * 0.5f
        val yearCost = yearEnergy * 0.5f
        
        val averageDaily = if (yearEnergy > 0) {
            val daysInYear = 365
            yearEnergy / daysInYear
        } else {
            0f
        }
        
        return MeterStats(
            meterId = meterId,
            todayEnergy = todayEnergy,
            monthEnergy = monthEnergy,
            yearEnergy = yearEnergy,
            todayCost = todayCost,
            monthCost = monthCost,
            yearCost = yearCost,
            averageDaily = averageDaily
        )
    }
    
    // Helper functions for time calculations
    private fun getStartOfDay(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getStartOfMonth(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getStartOfYear(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}