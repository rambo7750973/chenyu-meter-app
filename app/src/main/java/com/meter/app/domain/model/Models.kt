package com.meter.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meters")
data class Meter(
    @PrimaryKey
    val id: String,
    val name: String,
    val address: String,
    val macAddress: String,
    val model: String = "",
    val firmwareVersion: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "meter_readings")
data class MeterReading(
    @PrimaryKey
    val id: String,
    val meterId: String,
    val timestamp: Long,
    val currentPower: Float, // 当前功率 (W)
    val voltage: Float, // 电压 (V)
    val current: Float, // 电流 (A)
    val totalEnergy: Float, // 总用电量 (kWh)
    val dailyEnergy: Float, // 日用电量 (kWh)
    val monthlyEnergy: Float // 月用电量 (kWh)
)

@Entity(tableName = "billing_records")
data class BillingRecord(
    @PrimaryKey
    val id: String,
    val meterId: String,
    val startTime: Long,
    val endTime: Long,
    val energyUsed: Float, // 用电量 (kWh)
    val unitPrice: Float, // 单价 (元/kWh)
    val totalCost: Float, // 总费用 (元)
    val billingType: BillingType,
    val isPaid: Boolean = false,
    val paidAt: Long? = null
)

enum class BillingType {
    TIERED, // 阶梯电价
    TIME_OF_USE, // 分时电价
    FIXED // 固定电价
}

data class ElectricityPrice(
    val id: String,
    val name: String,
    val type: BillingType,
    val tiers: List<PriceTier>? = null,
    val timeSlots: List<TimeSlotPrice>? = null,
    val fixedPrice: Float? = null
)

data class PriceTier(
    val minKwh: Float,
    val maxKwh: Float,
    val price: Float
)

data class TimeSlotPrice(
    val startTime: String, // HH:mm
    val endTime: String, // HH:mm
    val price: Float,
    val isPeak: Boolean
)

data class MeterStats(
    val meterId: String,
    val todayEnergy: Float,
    val monthEnergy: Float,
    val yearEnergy: Float,
    val todayCost: Float,
    val monthCost: Float,
    val yearCost: Float,
    val averageDaily: Float
)