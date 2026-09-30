package com.andraste.tablet

import android.app.Application
import android.content.Context
import com.andraste.tablet.hal.CapabilityDetector
import com.andraste.tablet.hal.DeviceCapabilities

class AndrasterApp : Application() {

    lateinit var capabilities: DeviceCapabilities
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        capabilities = CapabilityDetector.detect(this)
    }

    companion object {
        private lateinit var instance: AndrasterApp
        fun get(): AndrasterApp = instance
        fun ctx(): Context = instance.applicationContext
    }
}
