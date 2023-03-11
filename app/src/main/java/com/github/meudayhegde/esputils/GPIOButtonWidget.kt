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
import com.github.meudayhegde.ThreadHandler
import com.github.meudayhegde.esputils.net.ESPTable
import com.github.meudayhegde.esputils.net.SocketClient
import org.json.JSONArray
import org.json.JSONObject

class GPIOButtonWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?
    ) {
        context?.let { ESPUtilsApp.updateStaticContext(it) }
        val thisWidget = ComponentName(context!!, GPIOButtonWidget::class.java)
        val allWidgetIds = appWidgetManager!!.getAppWidgetIds(thisWidget)

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout_gpio_button)
        val espTable = ESPTable.getInstance(context)

        for (widgetId in allWidgetIds) {
            val sharedPref = context.getSharedPreferences(widgetId.toString(), Context.MODE_PRIVATE)
            val switchName = sharedPref.getString(Strings.sharedPrefItemGPIOTitle, "")
            val devName = sharedPref.getString(Strings.sharedPrefItemGPIODevice, "")
            val devMAC = sharedPref.getString(Strings.sharedPrefItemGPIODeviceMAC, "") ?: ""
            val pinNumber = sharedPref.getInt(Strings.sharedPrefItemGPIOPinNumber, -1)

            val username = sharedPref.getString(Strings.sharedPrefItemGPIOUsername, "") ?: ""
            val password = sharedPref.getString(Strings.sharedPrefItemGPIOPassword, "") ?: ""

            remoteViews.setTextViewText(R.id.widget_gpio_title, switchName)
            remoteViews.setTextViewText(R.id.widget_gpio_device_name, devName)
            remoteViews.setImageViewResource(R.id.widget_lamp_icon, R.drawable.icon_lamp)

            espTable.getIpFromMac(devMAC) { address ->
                if (address?.isNotEmpty() == true) {
                    val connector = SocketClient.Connector(address)
                    connector.sendLine(Strings.espCommandGetGpio(username, password))
                    val response = connector.readLine()
                    connector.close()
                    val pinJson = JSONArray(response)

                    for (j in 0 until pinJson.length()) {
                        val gpioObj = pinJson.getJSONObject(j)
                        if (pinNumber == gpioObj.getInt(Strings.espResponsePinNumber)) {
                            val pinValue = gpioObj.getInt(Strings.espResponsePinValue)
                            Handler(Looper.getMainLooper()).post {
                                if (pinValue == 1) remoteViews.setImageViewResource(
                                    R.id.widget_lamp_icon, R.drawable.icon_lamp_on
                                )
                            }
                        }
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        remoteViews.setImageViewResource(
                            R.id.widget_lamp_icon, R.drawable.icon_lamp_offline
                        )
                        Toast.makeText(context, R.string.message_device_offline, Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                Handler(Looper.getMainLooper()).post {
                    val intent = Intent(context, javaClass)
                    intent.action = Strings.widgetIntentActionClick
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    intent.putExtra(Strings.intentExtraWidgetID, widgetId)
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        widgetId,
                        intent,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE
                        else 0
                    )
                    remoteViews.setOnClickPendingIntent(R.id.layout_id_gpio_widget, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, remoteViews)
                }
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        context?.let { ESPUtilsApp.updateStaticContext(it) }
        val manager = AppWidgetManager.getInstance(context)
        val remoteViews = RemoteViews(context?.packageName, R.layout.widget_layout_gpio_button)
        val widgetID = intent?.getIntExtra(Strings.intentExtraWidgetID, 0)

        when (intent?.action) {
            Strings.widgetIntentActionClick -> {
                val espTable = ESPTable.getInstance(context)

                val sharedPref =
                    context!!.getSharedPreferences(widgetID.toString(), Context.MODE_PRIVATE)
                val editor = sharedPref.edit()

                val devMac = sharedPref.getString(Strings.sharedPrefItemGPIODeviceMAC, "") ?: ""
                val username = sharedPref.getString(Strings.sharedPrefItemGPIOUsername, "") ?: ""
                val password = sharedPref.getString(Strings.sharedPrefItemGPIOPassword, "") ?: ""
                val pinNumber = sharedPref.getInt(Strings.sharedPrefItemGPIOPinNumber, -1)

                editor.apply()

                ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
                    try {
                        val connector = SocketClient.Connector(espTable.getIpFromMac(devMac) ?: "")
                        connector.sendLine(
                            Strings.espCommandSetGpio(
                                username, password, pinNumber, -1
                            )
                        )
                        val response = JSONObject(connector.readLine())
                        connector.close()
                        Handler(Looper.getMainLooper()).post {
                            if (response.optString(Strings.espResponse) == Strings.espResponseSuccess) {
                                when (response.optInt(Strings.espResponsePinValue)) {
                                    1 -> remoteViews.setImageViewResource(
                                        R.id.widget_lamp_icon, R.drawable.icon_lamp_on
                                    )
                                    0 -> remoteViews.setImageViewResource(
                                        R.id.widget_lamp_icon, R.drawable.icon_lamp
                                    )
                                    else -> remoteViews.setImageViewResource(
                                        R.id.widget_lamp_icon, R.drawable.icon_lamp_offline
                                    )
                                }
                            } else {
                                Toast.makeText(
                                    context, R.string.message_device_offline, Toast.LENGTH_SHORT
                                ).show()
                                remoteViews.setImageViewResource(
                                    R.id.widget_lamp_icon, R.drawable.icon_lamp_offline
                                )
                            }
                            manager.updateAppWidget(widgetID!!, remoteViews)
                        }
                    } catch (_: Exception) {
                        Handler(Looper.getMainLooper()).post {
                            remoteViews.setImageViewResource(
                                R.id.widget_lamp_icon, R.drawable.icon_lamp_offline
                            )
                            Toast.makeText(
                                context, R.string.message_device_offline, Toast.LENGTH_SHORT
                            ).show()
                            manager.updateAppWidget(widgetID!!, remoteViews)
                        }
                    }
                }
            }
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        context?.let { ESPUtilsApp.updateStaticContext(it) }
        appWidgetIds?.forEach {
            context?.getSharedPreferences(it.toString(), Context.MODE_PRIVATE)?.edit()?.clear()
                ?.apply()
        }
    }
}