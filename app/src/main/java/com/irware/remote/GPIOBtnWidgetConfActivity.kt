package com.irware.remote

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.irware.remote.holders.GPIOConfig
import com.irware.remote.holders.GPIOObject
import com.irware.remote.holders.ListItemCommon
import com.irware.remote.ui.adapters.ListAdapterCommon
import kotlin.math.min

class GPIOBtnWidgetConfActivity : AppCompatActivity() {

    var widgetId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recycler_refresh_layout)

        widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)?: AppWidgetManager.INVALID_APPWIDGET_ID

        setLayoutParams()

        val gpioConfigFile = ESPUtilsApp.getPrivateFile(Strings.nameFileGPIOConfig)
        if (!gpioConfigFile.exists()) gpioConfigFile.createNewFile()
        ESPUtilsApp.gpioConfig = GPIOConfig(gpioConfigFile)
        val gpioObjectArray = ESPUtilsApp.gpioConfig!!.gpioObjectArray
        ESPUtilsApp.gpioObjectList.clear()
        if(gpioObjectArray.length() >  0) for(i: Int in 0 until gpioObjectArray.length()){
            ESPUtilsApp.gpioObjectList.add(GPIOObject(gpioObjectArray.getJSONObject(i), ESPUtilsApp.gpioConfig!!))
        }

        val itemList = ArrayList<ListItemCommon>()
        ESPUtilsApp.gpioObjectList.forEach {
            itemList.add(ListItemCommon(it.title, it.subTitle, R.drawable.icon_lamp, it))
        }

        val listAdapter = ListAdapterCommon(itemList)
            .setOnItemClickListener{ _, listItem ->
                val sharedPref = getSharedPreferences(widgetId.toString(), Context.MODE_PRIVATE)
                val editor = sharedPref.edit()

                editor.putString(Strings.sharedPrefItemGPIOTitle, listItem.title)
                editor.putString(Strings.sharedPrefItemGPIOSubtitle, listItem.subTitle)
                editor.putString(Strings.sharedPrefItemGPIODevice, (listItem.linkedObj as GPIOObject).deviceProperties.nickName)
                editor.putString(Strings.sharedPrefItemGPIODeviceMAC, (listItem.linkedObj as GPIOObject).macAddr)
                editor.putInt(Strings.sharedPrefItemGPIOPinNumber, (listItem.linkedObj as GPIOObject).gpioNumber)
                editor.putInt(Strings.sharedPrefItemGPIOPinValue, (listItem.linkedObj as GPIOObject).pinValue)
                editor.putString(Strings.sharedPrefItemGPIOUsername, (listItem.linkedObj as GPIOObject).deviceProperties.userName)
                editor.putString(Strings.sharedPrefItemGPIOPassword, (listItem.linkedObj as GPIOObject).deviceProperties.password)
                editor.apply()

                updateAppWidget()
            }

        val recyclerView = findViewById<RecyclerView>(R.id.refresh_layout_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
        }
    }

    private fun updateAppWidget(){
        val intent = Intent(this, GPIOButtonWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val  ids = AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, GPIOButtonWidget::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }

    private fun setLayoutParams(){
        val lWindowParams = WindowManager.LayoutParams()
        lWindowParams.copyFrom(window?.attributes)
        lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT
        lWindowParams.height = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes = lWindowParams

        MainActivity.layoutParams.width = resources.displayMetrics.widthPixels
        MainActivity.layoutParams.height = resources.displayMetrics.heightPixels

        val width = min(MainActivity.layoutParams.width, MainActivity.layoutParams.height)
        MainActivity.NUM_COLUMNS = when{ width > 920 -> 5; width < 720 -> 3; else -> 4}
        lWindowParams.width = MainActivity.layoutParams.width * 7 / 8
        lWindowParams.height = MainActivity.layoutParams.height * 6 / 8
        window?.attributes = lWindowParams
    }
}