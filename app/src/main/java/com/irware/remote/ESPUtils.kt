package com.irware.remote

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.irware.ThreadHandler
import com.irware.remote.holders.DeviceProperties
import com.irware.remote.holders.GPIOConfig
import com.irware.remote.holders.GPIOObject
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.net.ARPTable
import java.io.File

class ESPUtils: Application() {
    override fun onCreate() {
        super.onCreate()
        FILES_DIR = filesDir.absolutePath
        arpTable = ARPTable(-1)

        when(getSharedPreferences("settings", Context.MODE_PRIVATE).getInt("application_theme", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { 0 }else{ 2 })) {
            1-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        remotePropList.clear()
        devicePropList.clear()
        gpioObjectList.clear()

        val arr = resources.obtainTypedArray(R.array.icons)
        iconDrawableList = IntArray(arr.length())
        for(i in 0 until arr.length())
            iconDrawableList[i] = arr.getResourceId(i, 0)
        arr.recycle()

        remoteConfigPath = FILES_DIR + File.separator + REMOTE_CONFIG_DIR
        deviceConfigPath = FILES_DIR + File.separator + DEVICE_CONFIG_DIR

        val deviceConfigDir = File(deviceConfigPath)
        deviceConfigDir.exists() or deviceConfigDir.mkdirs()
        for(file: File in deviceConfigDir.listFiles { _, name -> name?.endsWith(".json")?: false }!!){
            devicePropList.add(DeviceProperties(file))
        }

        val gpioConfigFile = File(FILES_DIR + File.separator + "GPIOConfig.json")
        if (!gpioConfigFile.exists()) gpioConfigFile.createNewFile()
        gpioConfig = GPIOConfig(gpioConfigFile)
        val gpioObjectArray = gpioConfig!!.gpioObjectArray
        if(gpioObjectArray.length() >  0)for(i: Int in 0 until gpioObjectArray.length()){
            gpioObjectList.add(GPIOObject(gpioObjectArray.getJSONObject(i), gpioConfig!!))
        }

        ThreadHandler.runOnFreeThread {
            val files = File(remoteConfigPath).listFiles { pathname ->
                pathname!!.isFile and (pathname.name.endsWith(".json", true)) and pathname.canWrite()
            }
            files?.forEach { file ->
                remotePropList.add(RemoteProperties(file, null))
            }
        }
    }

    companion object{
        lateinit var FILES_DIR: String
        lateinit var arpTable: ARPTable

        val remotePropList = ArrayList<RemoteProperties>()
        val devicePropList = ArrayList<DeviceProperties>()
        val gpioObjectList = ArrayList<GPIOObject>()
        var iconDrawableList:IntArray = intArrayOf()

        var gpioConfig: GPIOConfig? = null
        var remoteConfigPath = ""
        var deviceConfigPath = ""

        const val ESP_COM_PORT = 48321
        const val REMOTE_CONFIG_DIR = "remotes"
        const val DEVICE_CONFIG_DIR = "devices"
    }
}