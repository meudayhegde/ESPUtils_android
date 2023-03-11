package com.github.meudayhegde.esputils

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
import com.github.meudayhegde.esputils.holders.ButtonProperties
import com.github.meudayhegde.esputils.holders.RemoteProperties
import com.github.meudayhegde.esputils.net.SocketClient
import org.json.JSONObject
import java.io.FileNotFoundException


class RemoteButtonWidget: AppWidgetProvider() {

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        context?.let { ESPUtilsApp.updateStaticContext(it) }
        val thisWidget = ComponentName(context!!, RemoteButtonWidget::class.java)
        val allWidgetIds = appWidgetManager!!.getAppWidgetIds(thisWidget)

        val arr = context.resources.obtainTypedArray(R.array.icons)
        val iconDrawableList = IntArray(arr.length())
        for(i in 0 until arr.length())
            iconDrawableList[i] = arr.getResourceId(i,0)
        arr.recycle()

        for (widgetId in allWidgetIds) {

            val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout_remote_button)
            val objList = getJSONObject(context, widgetId, remoteViews)
            if(objList.isEmpty()) return
            val buttonProp = objList[1] as ButtonProperties

            remoteViews.setTextViewText(R.id.widget_button,buttonProp.text)
            if(buttonProp.icon!=0) remoteViews.setTextViewCompoundDrawables(R.id.widget_button,iconDrawableList[buttonProp.icon],0,0,0)

            val intent = Intent(context, javaClass)
            intent.action = Strings.widgetIntentActionClick
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            intent.putExtra(Strings.intentExtraWidgetID, widgetId)
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
        context?.let { ESPUtilsApp.updateStaticContext(it) }
        if(intent?.action == Strings.widgetIntentActionClick){
            val manager = AppWidgetManager.getInstance(context)
            val remoteViews = RemoteViews(context?.packageName, R.layout.widget_layout_remote_button)
            val watchWidget = ComponentName(context!!, RemoteButtonWidget::class.java)

            val widgetID = intent.getIntExtra(Strings.intentExtraWidgetID, 0)
            val objList = getJSONObject(context, widgetID, remoteViews)
            if(objList.isEmpty()) return
            val remoteProp = objList[0] as RemoteProperties
            val buttonProp = objList[1] as ButtonProperties
            val handler = Handler(Looper.getMainLooper())

            remoteProp.deviceProperties.getIpAddress { address ->
                    val userName = remoteProp.deviceProperties.userName
                    val password = remoteProp.deviceProperties.password

                    if (address  == null){
                        handler.post {
                            Toast.makeText(context, R.string.message_device_offline, Toast.LENGTH_LONG).show()
                            manager.updateAppWidget(watchWidget, remoteViews)
                        }
                        return@getIpAddress
                    }
                    SocketClient.sendIrCode(address, userName, password, buttonProp.jsonObj) { result ->
                            handler.post {
                                if(result.contains(Strings.espResponseSuccess))
                                    Toast.makeText(context,
                                        buttonProp.text + ": " + JSONObject(result).getString(Strings.espResponse),
                                        Toast.LENGTH_LONG).show()
                                else Toast.makeText(context, context.getString(R.string.message_device_not_connected), Toast.LENGTH_LONG).show()
                            }
                        }
                    manager.updateAppWidget(watchWidget, remoteViews)
                }
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        context?.let { ESPUtilsApp.updateStaticContext(it) }
        appWidgetIds?.forEach {
            val pref = context?.getSharedPreferences(Strings.sharedPrefNameWidgetAssociations, Context.MODE_PRIVATE)
            val editor = pref?.edit()
            editor?.remove(it.toString())
            editor?.apply()
        }
    }

    private fun getJSONObject(context: Context, widgetID: Int, views: RemoteViews): ArrayList<Any>{
        val pref = context.getSharedPreferences(Strings.sharedPrefNameWidgetAssociations, Context.MODE_PRIVATE)
        val editor = pref.edit()
        var buttonInfo = pref.getString(widgetID.toString(), "")
        if(buttonInfo.isNullOrEmpty()){
            val queuedButton = pref.getString(Strings.sharedPrefItemQueuedButton,"")
            Toast.makeText(context, queuedButton, Toast.LENGTH_LONG).show()
            if(!queuedButton.isNullOrEmpty()){
                editor.putString(widgetID.toString(), queuedButton)
                editor.putString(Strings.sharedPrefItemQueuedButton, "")
                editor.apply()
                buttonInfo = pref.getString(widgetID.toString(),"")
            }else{
                Toast.makeText(context,R.string.message_remote_btn_not_configured, Toast.LENGTH_LONG).show()
                setConfigureOnClick(context, widgetID, views)
                return ArrayList()
            }
        }
        try{
            val remoteProp = RemoteProperties(
                ESPUtilsApp.getPrivateFile(Strings.nameDirRemoteConfig, buttonInfo!!.split(",")[0])
            )
            val buttonProps = remoteProp.getButtons()
            for(i in 0 until buttonProps.length()){
                val jsonObj = buttonProps.getJSONObject(i)
                if(jsonObj.optLong(Strings.btnPropBtnId) == buttonInfo.split(",")[1].toLong()){
                    return arrayListOf(remoteProp, ButtonProperties(jsonObj))
                }
            }
        }catch(ex: FileNotFoundException){
            Toast.makeText(context,R.string.message_remote_conf_deleted, Toast.LENGTH_LONG).show()
            setConfigureOnClick(context, widgetID, views)
            return ArrayList()
        }
        Toast.makeText(context, R.string.message_remote_btn_deleted, Toast.LENGTH_LONG).show()
        setConfigureOnClick(context, widgetID, views)
        return ArrayList()
    }

    private fun setConfigureOnClick(context:Context, widgetID: Int, views: RemoteViews){
        val intent = Intent(context, RemoteBtnWidgetConfActivity::class.java)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID)
        val pendingIntent = PendingIntent.getActivity(context, widgetID, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE
            else 0
        )
        views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)
    }
}