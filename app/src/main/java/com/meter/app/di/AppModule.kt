package com.meter.app.di

import android.content.Context
import androidx.room.Room
import com.meter.app.data.local.MeterDao
import com.meter.app.data.local.MeterDatabase
import com.meter.app.ble.BleManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    @Provides
    @Singleton
    fun provideMeterDatabase(@ApplicationContext context: Context): MeterDatabase {
        return Room.databaseBuilder(
            context,
            MeterDatabase::class.java,
            "meter_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    @Singleton
    fun provideMeterDao(database: MeterDatabase): MeterDao {
        return database.meterDao()
    }
    
    @Provides
    @Singleton
    fun provideBleManager(@ApplicationContext context: Context): BleManager {
        return BleManager(context)
    }
}