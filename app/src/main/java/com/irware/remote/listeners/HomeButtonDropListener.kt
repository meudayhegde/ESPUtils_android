package com.irware.remote.listeners

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.DragEvent
import android.view.View
import android.widget.Toast
import androidx.core.graphics.drawable.DrawableCompat
import com.irware.remote.ButtonWidgetProvider
import com.irware.remote.ESPUtilsApp
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.ui.buttons.RemoteButton
import com.irware.remote.ui.dialogs.RemoteDialog
import kotlinx.android.synthetic.main.create_remote_layout.*

class HomeButtonDropListener(private val remoteDialog: RemoteDialog): View.OnDragListener{
    override fun onDrag(v: View?, event: DragEvent?): Boolean {
        when (event?.action) {
            DragEvent.ACTION_DROP -> {
                val view = event.localState as RemoteButton
                view.visibility = View.VISIBLE
                val btnProp = view.getProperties()
                requestPinWidget(btnProp)
            }
            DragEvent.ACTION_DRAG_ENDED ->{
                remoteDialog.image_view_home.visibility = View.INVISIBLE
                DrawableCompat.setTint(remoteDialog.image_view_home.drawable, MainActivity.colorOnBackground)
                remoteDialog.create_remote_info_layout.visibility = View.VISIBLE
            }
            DragEvent.ACTION_DRAG_ENTERED ->{
                DrawableCompat.setTint(remoteDialog.image_view_home.drawable, Color.GREEN)
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                DrawableCompat.setTint(remoteDialog.image_view_home.drawable, MainActivity.colorOnBackground)
            }
        }
        return true
    }

    private fun requestPinWidget(properties: ButtonProperties){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager: AppWidgetManager = remoteDialog.context.getSystemService(
                AppWidgetManager::class.java)
            val myProvider = ComponentName(remoteDialog.context, ButtonWidgetProvider::class.java)

            val pref = remoteDialog.context.getSharedPreferences(ESPUtilsApp.getString(R.string.shared_pref_name_widget_associations), Context.MODE_PRIVATE)
            val editor = pref.edit()
            editor.putString(ESPUtilsApp.getString(R.string.shared_pref_item_queued_button), properties.parent?.remoteConfigFile?.name + "," + properties.buttonId)
            editor.apply()

            Toast.makeText(remoteDialog.context, pref.getString(ESPUtilsApp.getString(R.string.shared_pref_item_queued_button), ""), Toast.LENGTH_LONG).show()

            if(appWidgetManager.isRequestPinAppWidgetSupported){
                appWidgetManager.requestPinAppWidget(myProvider, null, null)
            }
            else{
                Toast.makeText(remoteDialog.context, R.string.launcher_no_support_feature, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(remoteDialog.context,R.string.android_version_no_support_feature, Toast.LENGTH_LONG).show()
        }
    }
}