package com.github.meudayhegde.esputils.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.DragEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.github.clans.fab.FloatingActionButton
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.MainActivity
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.holders.ButtonProperties
import com.github.meudayhegde.esputils.holders.RemoteProperties
import com.github.meudayhegde.esputils.listeners.HomeButtonDropListener
import com.github.meudayhegde.esputils.listeners.OnRemoteButtonSelectedListener
import com.github.meudayhegde.esputils.listeners.SettingsButtonDropListener
import com.github.meudayhegde.esputils.ui.adapters.ButtonsGridAdapter
import com.github.meudayhegde.esputils.ui.buttons.RemoteButton
import kotlinx.android.synthetic.main.create_remote_layout.*
import org.json.JSONException
import org.json.JSONObject

class RemoteDialog(context: Context,val properties:RemoteProperties, val mode:Int) : Dialog(context,R.style.AppTheme),
    OnRemoteButtonSelectedListener, View.OnDragListener {
    private val arrayList:ArrayList<ButtonProperties?> = ArrayList()
    private val adapter:ButtonsGridAdapter

    init {
        window?.attributes?.windowAnimations = R.style.DialogAnimationTheme
        setContentView(R.layout.create_remote_layout)
        when(mode) {
            MODE_SELECT_BUTTON ->{
                fam_manage_button_actions.visibility = View.GONE
                create_remote_info_layout.visibility = View.GONE
            }
        }
        var length = MainActivity.NUM_COLUMNS * MainActivity.layoutParams.width / (RemoteButton.MIN_HEIGHT + 12)
        arrayList.addAll(arrayOfNulls(length))
        val buttons = properties.getButtons()
        for(i in 0 until buttons.length()){
            val obj = buttons.getJSONObject(i)
            val btnProp = ButtonProperties(obj, properties)
            if(btnProp.btnPosition >= length) {
                arrayList.addAll(arrayOfNulls(btnProp.btnPosition - length + 1))
                length = btnProp.btnPosition
            }
            arrayList[btnProp.btnPosition] = btnProp
        }

        adapter = ButtonsGridAdapter(arrayList,this, properties.deviceProperties.ipAddress,
            properties.deviceProperties.userName, properties.deviceProperties.password)
        button_refresh_layout.setOnRefreshListener {
            button_refresh_layout.isRefreshing = true
            adapter.notifyDataSetChanged(true)
            button_refresh_layout.isRefreshing = false
        }

        if(mode == MODE_VIEW_EDIT){
            layout_add_to_home.layoutParams.width = MainActivity.layoutParams.width / 2
            layout_del_button.setOnDragListener(this)
            layout_add_to_home.setOnDragListener(HomeButtonDropListener(this))
            layout_button_settings.setOnDragListener(SettingsButtonDropListener(this))
        }


        buttons_layout_recycler_view.layoutManager = GridLayoutManager(context,MainActivity.NUM_COLUMNS)
        Handler(Looper.getMainLooper()).postDelayed({
            buttons_layout_recycler_view.adapter = adapter
        },200)

        if(mode == MODE_VIEW_EDIT) {
            fam_manage_button_actions.hideMenuButton(false)
            Handler(Looper.getMainLooper()).postDelayed({
                fam_manage_button_actions.showMenuButton(true)
            },800)
            fam_manage_button_actions.setClosedOnTouchOutside(true)
            setOnFabClickListener(fab_new_button,ButtonPropertiesDialog.MODE_SINGLE)
            setOnFabClickListener(fab_multi_capture,ButtonPropertiesDialog.MODE_MULTI)
        }
    }

    private fun setOnFabClickListener(fab: FloatingActionButton, mode: Int){
        fab.setOnClickListener {
            val dialog = ButtonPropertiesDialog(context, this, mode, properties.deviceProperties.ipAddress,
                properties.deviceProperties.userName, properties.deviceProperties.password)
            dialog.show()
            dialog.captureInit(null)
            fam_manage_button_actions.close(true)
        }
    }

    override fun onSelected(jsonObject: JSONObject) {
        try{
            var pos = jsonObject.getInt(Strings.btnPropBtnPosition)
            if(pos < 0){
                pos = adapter.getEmptyPosition()
                jsonObject.put(Strings.btnPropBtnPosition, pos)
            }
            val btnProp = ButtonProperties(jsonObject, properties)
            arrayList[pos] = btnProp
            adapter.notifyItemChanged(pos)
        }catch(ex:JSONException){
            val pos= adapter.getEmptyPosition()
            jsonObject.put(Strings.btnPropBtnPosition, pos)
            val btnProp = ButtonProperties(jsonObject, properties)
            arrayList[pos] = btnProp
            adapter.notifyItemChanged(pos)
        }
    }

    override fun onDrag(v: View?, event: DragEvent?): Boolean {
        when (event?.action) {
            DragEvent.ACTION_DROP -> {
                val view = event.localState as RemoteButton
                AlertDialog.Builder(context)
                    .setTitle(R.string.confirm)
                    .setMessage(R.string.message_dialog_delete_remote_button)
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss()}
                    .setPositiveButton(R.string.delete){ dialog, _ ->
                        val position = view.getProperties().btnPosition
                        properties.removeButton(view.getProperties().jsonObj)
                        arrayList[position] = null
                        adapter.notifyDataSetChanged(false)
                        dialog.dismiss()
                    }
                    .show()
            }
            DragEvent.ACTION_DRAG_ENDED ->{
                image_view_delete.visibility = View.INVISIBLE
                DrawableCompat.setTint(image_view_delete.drawable,MainActivity.colorOnBackground)
                create_remote_info_layout.visibility = View.VISIBLE
            }
            DragEvent.ACTION_DRAG_ENTERED ->{
                DrawableCompat.setTint(image_view_delete.drawable,Color.RED)
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                DrawableCompat.setTint(image_view_delete.drawable, MainActivity.colorOnBackground)
            }
        }
        return true
    }

    companion object{
        const val MODE_SELECT_BUTTON = 1
        const val MODE_VIEW_EDIT = 0
    }
}
