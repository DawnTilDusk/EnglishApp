package com.example.seedie

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SeedieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialization code for the app can go here
    }
}
