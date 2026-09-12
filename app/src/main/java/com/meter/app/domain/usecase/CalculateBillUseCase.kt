package com.meter.app.domain.usecase

import com.meter.app.domain.model.*
import java.util.*
import javax.inject.Inject

class CalculateBillUseCase @Inject constructor() {
    
    operator fun invoke(
        energyUsed: Float,
        pricing: ElectricityPrice,
        startTime: Long,
        endTime: Long
    ): BillingRecord {
        val totalCost = when (pricing.type) {
            BillingType.TIERED -> calculateTieredCost(energyUsed, pricing.tiers ?: emptyList())
            BillingType.TIME_OF_USE -> calculateTimeOfUseCost(energyUsed, pricing.timeSlots ?: emptyList())
            BillingType.FIXED -> energyUsed * (pricing.fixedPrice ?: 0f)
        }
        
        return BillingRecord(
            id = UUID.randomUUID().toString(),
            meterId = "", // Will be set by the caller
            startTime = startTime,
            endTime = endTime,
            energyUsed = energyUsed,
            unitPrice = totalCost / energyUsed,
            totalCost = totalCost,
            billingType = pricing.type,
            isPaid = false
        )
    }
    
    private fun calculateTieredCost(energyUsed: Float, tiers: List<PriceTier>): Float {
        var remaining = energyUsed
        var totalCost = 0f
        
        for (tier in tiers.sortedBy { it.minKwh }) {
            if (remaining <= 0) break
            
            val tierRange = tier.maxKwh - tier.minKwh
            val energyInTier = minOf(remaining, tierRange)
            
            totalCost += energyInTier * tier.price
            remaining -= energyInTier
        }
        
        return totalCost
    }
    
    private fun calculateTimeOfUseCost(energyUsed: Float, timeSlots: List<TimeSlotPrice>): Float {
        // Simplified calculation - in reality, you'd need to know the exact time of usage
        // For now, we'll just use the average price
        val averagePrice = timeSlots.map { it.price }.average().toFloat()
        return energyUsed * averagePrice
    }
    
    fun calculateMonthlyBill(
        readings: List<MeterReading>,
        pricing: ElectricityPrice
    ): Float {
        val totalEnergy = readings.sumOf { it.dailyEnergy.toDouble() }.toFloat()
        val calendar = Calendar.getInstance()
        
        // Get start and end of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        calendar.add(Calendar.MONTH, 1)
        val endTime = calendar.timeInMillis
        
        return invoke(totalEnergy, pricing, startTime, endTime).totalCost
    }
    
    fun calculateDailyBill(
        readings: List<MeterReading>,
        pricing: ElectricityPrice
    ): Float {
        val totalEnergy = readings.sumOf { it.dailyEnergy.toDouble() }.toFloat()
        val calendar = Calendar.getInstance()
        
        // Get start and end of day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endTime = calendar.timeInMillis
        
        return invoke(totalEnergy, pricing, startTime, endTime).totalCost
    }
}