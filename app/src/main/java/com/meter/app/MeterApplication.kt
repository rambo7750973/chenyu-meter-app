package com.meter.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MeterApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any necessary components here
    }
}