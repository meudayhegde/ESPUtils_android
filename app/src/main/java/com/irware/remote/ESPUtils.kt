package com.irware.remote

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

class ESPUtils: Application() {
    override fun onCreate() {
        super.onCreate()
        FILES_DIR = filesDir.absolutePath

        when(getSharedPreferences("theme_setting", Context.MODE_PRIVATE).getInt("application_theme", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { 0 }else{ 2 })) {
            1-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    companion object{
        lateinit var FILES_DIR: String

        const val PORT = 48321
        const val REMOTE_CONFIG_DIR = "remotes"
        const val DEVICE_CONFIG_DIR = "devices"
    }
}