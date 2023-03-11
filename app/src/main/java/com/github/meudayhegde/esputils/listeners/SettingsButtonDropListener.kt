package com.github.meudayhegde.esputils.listeners

import android.os.Build
import android.view.DragEvent
import android.view.View
import androidx.core.graphics.drawable.DrawableCompat
import com.github.meudayhegde.esputils.MainActivity
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.ui.buttons.RemoteButton
import com.github.meudayhegde.esputils.ui.dialogs.ButtonPropertiesDialog
import com.github.meudayhegde.esputils.ui.dialogs.RemoteDialog
import org.json.JSONObject

class SettingsButtonDropListener(private val remoteDialog: RemoteDialog): View.OnDragListener{
    override fun onDrag(v: View?, event: DragEvent?): Boolean {
        when (event?.action) {
            DragEvent.ACTION_DROP -> {
                val view = event.localState as RemoteButton
                view.visibility = View.VISIBLE
                val btnProp = view.getProperties()
                val properties = remoteDialog.properties
                val dialog = ButtonPropertiesDialog(view.context, remoteDialog,
                    ButtonPropertiesDialog.MODE_SINGLE, properties.deviceProperties.ipAddress,
                    properties.deviceProperties.userName, properties.deviceProperties.password)
                dialog.show()
                dialog.onIrRead(JSONObject(btnProp.jsonObj.toString()))
            }
            DragEvent.ACTION_DRAG_ENDED ->{
                remoteDialog.dialogBinding.imageViewBtnSettings.visibility = View.INVISIBLE
                DrawableCompat.setTint(remoteDialog.dialogBinding.imageViewBtnSettings.drawable, MainActivity.colorOnBackground)
                remoteDialog.dialogBinding.createRemoteInfoLayout.visibility = View.VISIBLE
            }
            DragEvent.ACTION_DRAG_ENTERED ->{

                DrawableCompat.setTint(remoteDialog.dialogBinding.imageViewBtnSettings.drawable, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { remoteDialog.context.getColor(
                    R.color.sky_blue)} else remoteDialog.context.resources.getColor(R.color.sky_blue))
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                DrawableCompat.setTint(remoteDialog.dialogBinding.imageViewBtnSettings.drawable, MainActivity.colorOnBackground)
            }
        }
        return true
    }
}