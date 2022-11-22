package com.irware.remote

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.irware.ThreadHandler
import com.irware.remote.holders.DeviceProperties
import com.irware.remote.holders.GPIOConfig
import com.irware.remote.holders.GPIOObject
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.net.ARPTable
import java.io.File
import java.lang.ref.WeakReference

class ESPUtilsApp: Application() {
    override fun onCreate() {
        super.onCreate()
        contextRef = WeakReference(this)
        arpTable = ARPTable(-1)

        when(getSharedPreferences(
            getString(R.string.shared_pref_name_settings), Context.MODE_PRIVATE).getInt(
            getString(R.string.shared_pref_item_application_theme), if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            { 0 }else{ 2 })
        ){
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

        val deviceConfigDir = getAbsoluteFile(R.string.name_dir_device_config)
        deviceConfigDir.exists() or deviceConfigDir.mkdirs()
        for(file: File in deviceConfigDir.listFiles { _, name ->
            name?.endsWith(getString(R.string.extension_json))?: false }!!) {
            devicePropList.add(DeviceProperties(file))
        }

        val gpioConfigFile = getAbsoluteFile(R.string.name_file_gpio_config)
        if (!gpioConfigFile.exists()) gpioConfigFile.createNewFile()
        gpioConfig = GPIOConfig(gpioConfigFile)
        val gpioObjectArray = gpioConfig!!.gpioObjectArray
        if(gpioObjectArray.length() >  0)for(i: Int in 0 until gpioObjectArray.length()){
            gpioObjectList.add(GPIOObject(gpioObjectArray.getJSONObject(i), gpioConfig!!))
        }

        ThreadHandler.runOnFreeThread {
            val files = getAbsoluteFile(R.string.name_dir_remote_config).listFiles { pathname ->
                pathname!!.isFile and (pathname.name.endsWith(getString(R.string.extension_json), true)) and pathname.canWrite()
            }
            files?.forEach { file ->
                remotePropList.add(RemoteProperties(file, null))
            }
        }
    }

    companion object{
        lateinit var arpTable: ARPTable

        private var contextRef: WeakReference<Context>? = null

        val remotePropList = ArrayList<RemoteProperties>()
        val devicePropList = ArrayList<DeviceProperties>()
        val gpioObjectList = ArrayList<GPIOObject>()
        var iconDrawableList: IntArray = intArrayOf()

        var gpioConfig: GPIOConfig? = null

        const val ESP_COM_PORT = 48321

        /**
         * Static function to get string resource without access to context from any class
         */
        fun getString(@StringRes stringRes: Int, vararg formatArgs: Any = emptyArray()): String{
            return contextRef?.get()?.getString(stringRes, *formatArgs)?: ""
        }

        /**
         * @param dirFileNames directory names (String or StringRes) in the dirTree of the required file
         * @return complete file path of the required file from filesystem root. By default the mentioned file/dir will be stored in private storage
         */
        fun getAbsolutePath(vararg dirFileNames: Any = emptyArray()): String{
            val filesDir = contextRef?.get()?.filesDir?.absolutePath?:
            Environment.getExternalStorageDirectory().absolutePath
            val dirTree = arrayListOf<String>(filesDir)
            dirFileNames.forEach {
                when(it){
                    is String -> if("?" !in it && "/" !in it) dirTree.add(it)
                    is Int -> {
                        val st = getString(it)
                        if("?" !in st && "/" !in st) dirTree.add(st)
                    }
                    else -> throw IllegalArgumentException()
                }
            }
            return TextUtils.join(File.separator, dirTree)
        }

        /**
         * @param dirFileNames directory names (String or StringRes) in the dirTree of the required file
         * @return File object. By default the mentioned file/dir will be stored in private storage.
         */
        fun getAbsoluteFile(vararg dirFileNames: Any = emptyArray()): File{
            return File(getAbsolutePath(*dirFileNames))
        }
    }
}