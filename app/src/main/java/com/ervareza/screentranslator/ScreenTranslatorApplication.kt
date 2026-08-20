package com.ervareza.screentranslator

import android.app.Application

class ScreenTranslatorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(this))
    }
}
