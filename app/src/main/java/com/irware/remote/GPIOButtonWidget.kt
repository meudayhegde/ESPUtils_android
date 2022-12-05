package com.irware.remote

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class GPIOButtonWidget: AppWidgetProvider() {
    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {}
    override fun onReceive(context: Context?, intent: Intent?){}
    override fun onDeleted(context: Context?, appWidgetIds: IntArray?){}
}