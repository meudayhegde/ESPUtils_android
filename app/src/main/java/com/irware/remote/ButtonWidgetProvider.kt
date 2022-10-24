package com.irware.remote

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.Toast
import com.irware.remote.net.ARPTable
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.net.SocketClient
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException


class ButtonWidgetProvider: AppWidgetProvider() {

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {

        val thisWidget = ComponentName(context!!, ButtonWidgetProvider::class.java)
        val allWidgetIds = appWidgetManager!!.getAppWidgetIds(thisWidget)

        val arr = context.resources.obtainTypedArray(R.array.icons)
        val iconDrawableList = IntArray(arr.length())
        for(i in 0 until arr.length())
            iconDrawableList[i] = arr.getResourceId(i,0)
        arr.recycle()

        for (widgetId in allWidgetIds) {

            val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout)
            val objList = getJSONObject(context,widgetId,remoteViews)
            if(objList.isEmpty()) return
            val buttonProp = objList[1] as ButtonProperties

            remoteViews.setTextViewText(R.id.widget_button,buttonProp.text)
            if(buttonProp.icon!=0) remoteViews.setTextViewCompoundDrawables(R.id.widget_button,iconDrawableList[buttonProp.icon],0,0,0)

            val intent = Intent(context, javaClass)
            intent.action = BUTTON_CLICK
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            intent.putExtra("WidgetID", widgetId)
            val pendingIntent = PendingIntent.getBroadcast(context, widgetId, intent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE
                    else 0
                )
            remoteViews.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if(BUTTON_CLICK == intent?.action){
            val manager = AppWidgetManager.getInstance(context)
            val remoteViews = RemoteViews(context?.packageName,R.layout.widget_layout)
            val watchWidget = ComponentName(context!!,ButtonWidgetProvider::class.java)

            val widgetID = intent.getIntExtra("WidgetID",0)
            val objList = getJSONObject(context, widgetID, remoteViews)
            if(objList.isEmpty()) return
            val remoteProp = objList[0] as RemoteProperties
            val buttonProp = objList[1] as ButtonProperties
            val handler = Handler(Looper.getMainLooper())

            (MainActivity.arpTable ?: ARPTable(1)).getIpFromMac(remoteProp.deviceProperties.macAddress) { address ->
                    val userName = remoteProp.deviceProperties.userName
                    val password = remoteProp.deviceProperties.password
                    if (address  == null){
                        handler.post {
                            Toast.makeText(context, "Err: Device not reachable", Toast.LENGTH_LONG).show()
                            manager.updateAppWidget(watchWidget, remoteViews)
                        }
                        return@getIpFromMac
                    }
                    SocketClient.sendIrCode(address, userName, password, buttonProp.jsonObj) { result ->
                            handler.post {
                                if(result.contains("success")) Toast.makeText(context, buttonProp.text + ": " + JSONObject(result).getString("response"), Toast.LENGTH_LONG).show()
                                else Toast.makeText(context, context.getString(R.string.device_not_connected), Toast.LENGTH_LONG).show()
                            }
                        }
                    manager.updateAppWidget(watchWidget, remoteViews)
                }
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds?.forEach {
            val pref = context?.getSharedPreferences("widget_associations", Context.MODE_PRIVATE)
            val editor = pref?.edit()
            editor?.remove(it.toString())
            editor?.apply()
        }
    }

    private fun getJSONObject(context:Context,widgetID:Int,views:RemoteViews): ArrayList<Any>{
        val pref = context.getSharedPreferences("widget_associations", Context.MODE_PRIVATE)
        val editor = pref.edit()
        var buttonInfo = pref.getString(widgetID.toString(),"")
        if(buttonInfo.isNullOrEmpty()){
            val queuedButton = pref.getString("queued_button","")
            Toast.makeText(context, queuedButton, Toast.LENGTH_LONG).show()
            if(!queuedButton.isNullOrEmpty()){
                editor.putString(widgetID.toString(), queuedButton)
                editor.putString("queued_button", "")
                editor.apply()
                buttonInfo = pref.getString(widgetID.toString(),"")
            }else{
            //    Toast.makeText(context,"Button Not configured, Click to configure",Toast.LENGTH_LONG).show()
                setConfigureOnClick(context, widgetID, views)
                return ArrayList()
            }
        }
        try{

            val remoteProp = RemoteProperties(File(context.filesDir.absolutePath + File.separator + MainActivity.REMOTE_CONFIG_DIR + File.separator + buttonInfo!!.split(",")[0]),null)
            val buttonProps = remoteProp.getButtons()
            for(i in 0 until buttonProps.length()){
                val jsonObj = buttonProps.getJSONObject(i)
                if(jsonObj.optLong("buttonID") == buttonInfo.split(",")[1].toLong()){
                    return arrayListOf(remoteProp, ButtonProperties(jsonObj))
                }
            }
        }catch(ex:FileNotFoundException){
            Toast.makeText(context,"Remote Configuration Deleted, click button to configure. $ex", Toast.LENGTH_LONG).show()
            setConfigureOnClick(context, widgetID, views)
            return ArrayList()
        }
        Toast.makeText(context, "Button is deleted from Remote Configuration, click widget to configure.", Toast.LENGTH_LONG).show()
        setConfigureOnClick(context, widgetID, views)
        return ArrayList()
    }

    private fun setConfigureOnClick(context:Context,widgetID: Int,views: RemoteViews){
        val intent = Intent(context, WidgetConfiguratorActivity::class.java)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID)
        val pendingIntent = PendingIntent.getActivity(context, widgetID, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE
            else 0
        )
        views.setOnClickPendingIntent(R.id.widget_button,pendingIntent)
    }

    companion object {
        const val BUTTON_CLICK = "automaticWidgetSyncButtonClick"
    }
}