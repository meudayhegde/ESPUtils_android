package com.github.meudayhegde.esputils.listeners

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.DragEvent
import android.view.View
import android.widget.Toast
import androidx.core.graphics.drawable.DrawableCompat
import com.github.meudayhegde.esputils.MainActivity
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.RemoteButtonWidget
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.holders.ButtonProperties
import com.github.meudayhegde.esputils.ui.buttons.RemoteButton
import com.github.meudayhegde.esputils.ui.dialogs.RemoteDialog

class HomeButtonDropListener(private val remoteDialog: RemoteDialog) : View.OnDragListener {
    override fun onDrag(v: View?, event: DragEvent?): Boolean {
        when (event?.action) {
            DragEvent.ACTION_DROP -> {
                val view = event.localState as RemoteButton
                view.visibility = View.VISIBLE
                val btnProp = view.getProperties()
                requestPinWidget(btnProp)
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                remoteDialog.dialogBinding.imageViewHome.visibility = View.INVISIBLE
                DrawableCompat.setTint(
                    remoteDialog.dialogBinding.imageViewHome.drawable,
                    MainActivity.colorOnBackground
                )
                remoteDialog.dialogBinding.createRemoteInfoLayout.visibility = View.VISIBLE
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                DrawableCompat.setTint(
                    remoteDialog.dialogBinding.imageViewHome.drawable, Color.GREEN
                )
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                DrawableCompat.setTint(
                    remoteDialog.dialogBinding.imageViewHome.drawable,
                    MainActivity.colorOnBackground
                )
            }
        }
        return true
    }

    private fun requestPinWidget(properties: ButtonProperties) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager: AppWidgetManager =
                remoteDialog.context.getSystemService(AppWidgetManager::class.java)
            val myProvider = ComponentName(remoteDialog.context, RemoteButtonWidget::class.java)

            val pref = remoteDialog.context.getSharedPreferences(
                Strings.sharedPrefNameWidgetAssociations, Context.MODE_PRIVATE
            )
            val editor = pref.edit()
            editor.putString(
                Strings.sharedPrefItemQueuedButton,
                properties.parent?.remoteConfigFile?.name + "," + properties.buttonId
            )
            editor.apply()

            Toast.makeText(
                remoteDialog.context,
                pref.getString(Strings.sharedPrefItemQueuedButton, ""),
                Toast.LENGTH_LONG
            ).show()

            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(myProvider, null, null)
            } else {
                Toast.makeText(
                    remoteDialog.context, R.string.message_launcher_no_support, Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                remoteDialog.context, R.string.message_android_version_no_support, Toast.LENGTH_LONG
            ).show()
        }
    }
}