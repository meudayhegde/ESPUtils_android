package com.github.meudayhegde.esputils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.github.meudayhegde.ThreadHandler
import com.github.meudayhegde.esputils.Strings.espInterstitialAdID
import com.github.meudayhegde.esputils.holders.DeviceProperties
import com.github.meudayhegde.esputils.holders.GPIOConfig
import com.github.meudayhegde.esputils.holders.GPIOObject
import com.github.meudayhegde.esputils.holders.RemoteProperties
import com.github.meudayhegde.esputils.net.ESPTable
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.io.File
import java.lang.ref.WeakReference

class ESPUtilsApp: Application() {
    override fun onCreate() {
        super.onCreate()
        contextRef = WeakReference(this)
        ESPTable.getInstance(this)

        when(getSharedPreferences(
            Strings.sharedPrefNameSettings, Context.MODE_PRIVATE).getInt(
            Strings.sharedPrefItemApplicationTheme, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            { 0 }else{ 2 })
        ){
            1-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        MobileAds.initialize(this) { status ->
            Log.d(status.javaClass.simpleName, status.toString())
            loadInterstitialAd(this)
        }

        remotePropList.clear()
        devicePropList.clear()
        gpioObjectList.clear()

        val arr = resources.obtainTypedArray(R.array.icons)
        iconDrawableList = IntArray(arr.length())
        for(i in 0 until arr.length())
            iconDrawableList[i] = arr.getResourceId(i, 0)
        arr.recycle()

        val deviceConfigDir = getPrivateFile(Strings.nameDirDeviceConfig)
        deviceConfigDir.exists() or deviceConfigDir.mkdirs()
        for(file: File in deviceConfigDir.listFiles { _, name ->
            name?.endsWith(Strings.extensionJson)?: false }!!) {
            devicePropList.add(DeviceProperties(file))
        }

        val gpioConfigFile = getPrivateFile(Strings.nameFileGPIOConfig)
        if (!gpioConfigFile.exists()) gpioConfigFile.createNewFile()
        gpioConfig = GPIOConfig(gpioConfigFile)
        val gpioObjectArray = gpioConfig!!.gpioObjectArray
        if(gpioObjectArray.length() >  0)for(i: Int in 0 until gpioObjectArray.length()){
            gpioObjectList.add(GPIOObject(gpioObjectArray.getJSONObject(i), gpioConfig!!))
        }

        ThreadHandler.runOnFreeThread {
            val files = getPrivateFile(Strings.nameDirRemoteConfig).listFiles { pathname ->
                pathname!!.isFile and (pathname.name.endsWith(Strings.extensionJson, true)) and pathname.canWrite()
            }
            files?.forEach { file ->
                remotePropList.add(RemoteProperties(file, null))
            }
        }
    }

    companion object{
        private var contextRef: WeakReference<Context>? = null

        val remotePropList = ArrayList<RemoteProperties>()
        val devicePropList = ArrayList<DeviceProperties>()
        val gpioObjectList = ArrayList<GPIOObject>()
        var iconDrawableList: IntArray = intArrayOf()

        var gpioConfig: GPIOConfig? = null

        const val ESP_COM_PORT = 48321
        const val UDP_PORT_ESP = 48327
        const val UDP_PORT_APP = 48326

        /**
         * Static function to get string resource without access to context from any class
         */
        fun getString(@StringRes stringRes: Int, vararg formatArgs: Any = emptyArray(),  context: Context? = contextRef?.get()): String{
            return context?.getString(stringRes, *formatArgs)?: ""
        }

        private fun getAbsolutePath(root: File?, vararg dirFileNames: Any = emptyArray(), context: Context? = contextRef?.get()): String{
            val rootDir = (root?: Environment.getExternalStorageDirectory()).absolutePath
            val dirTree = arrayListOf<String>(rootDir)
            dirFileNames.forEach {
                when(it){
                    is String -> if("?" !in it && "/" !in it) dirTree.add(it)
                    is Int -> {
                        val st = getString(it, context = context)
                        if("?" !in st && "/" !in st) dirTree.add(st)
                    }
                    else -> throw IllegalArgumentException()
                }
            }
            return TextUtils.join(File.separator, dirTree)
        }

        /**
         * @param dirFileNames directory names (String or StringRes) in the dirTree of the required file
         * @return File object. By default the mentioned file/dir will be stored in app private storage.
         * Note: Files stored in this directory will not be available to any other application (without root access).
         */
        fun getPrivateFile(vararg dirFileNames: Any = emptyArray(), context: Context? = contextRef?.get()): File{
            return File(
                getAbsolutePath(context?.filesDir?: Environment.getExternalStorageDirectory(),
                *dirFileNames)
            )
        }

        /**
         * @param dirFileNames directory names (String or StringRes) in the dirTree of the required file
         * @return File object. By default the mentioned file/dir will be stored in external cache directory.
         * Note: Files stored in this directory will be available to any application with storage permissions.
         */
        fun getExternalCache(vararg dirFileNames: Any = emptyArray(), context: Context? = contextRef?.get()): File{
            return File(
                getAbsolutePath(context?.externalCacheDir?:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.getStorageDirectory()
                else Environment.getExternalStorageDirectory(), *dirFileNames)
            )
        }

        /**
         * @param dirFileNames directory names (String or StringRes) in the dirTree of the required file
         * @return File object. By default the mentioned file/dir will be stored in private cache directory.
         * Note: Files stored in this directory will be available to any application with storage permissions.
         */
        fun getCache(vararg dirFileNames: Any = emptyArray(), context: Context? = contextRef?.get()): File{
            return File(
                getAbsolutePath(context?.cacheDir, *dirFileNames)
            )
        }

        fun updateStaticContext(context: Context){
            contextRef = WeakReference(context)
        }

        private var mInterstitialAd: InterstitialAd? = null
        private fun loadInterstitialAd(context: Context){
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(context, espInterstitialAdID, adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(InterstitialAd::class.simpleName, adError.toString())
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(InterstitialAd::class.simpleName, "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                }
            })

            mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdClicked() {
                    Log.d(InterstitialAd::class.simpleName, "Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(InterstitialAd::class.simpleName, "Ad dismissed fullscreen content.")
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    Log.e(InterstitialAd::class.simpleName, "Ad failed to show fullscreen content.")
                }

                override fun onAdImpression() {
                    Log.d(InterstitialAd::class.simpleName, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(InterstitialAd::class.simpleName, "Ad showed fullscreen content.")
                }
            }
        }

        /**
         * Show full screen InterstitialAd
         */
        fun showAd(activity: Activity){
            if (mInterstitialAd != null) {
                mInterstitialAd?.show(activity)
            } else {
                Log.d(InterstitialAd::class.simpleName, "The interstitial ad wasn't ready yet.")
            }
            loadInterstitialAd(activity)
        }
    }
}