package com.irware.remote.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.view.DragEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.github.clans.fab.FloatingActionButton
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.ui.adapters.ButtonsGridAdapter
import com.irware.remote.ui.buttons.RemoteButton
import kotlinx.android.synthetic.main.create_remote_layout.*
import org.json.JSONException
import org.json.JSONObject

class RemoteDialog(context: Context,private val properties:RemoteProperties, val mode:Int) : Dialog(context,R.style.AppTheme),OnSelectedListener {
    private val arrayList:ArrayList<ButtonProperties?> = ArrayList()
    private val adapter:ButtonsGridAdapter

    init {
        window?.attributes?.windowAnimations = R.style.DialogAnimationTheme
        setContentView(R.layout.create_remote_layout)
        if(mode == MODE_VIEW_ONLY) {
            fam_manage_button_actions.visibility = View.GONE
            create_remote_info_layout.visibility = View.GONE
        }
        var length = MainActivity.NUM_COLUMNS*MainActivity.size.y/(RemoteButton.MIN_HEIGHT+12)
        arrayList.addAll(arrayOfNulls(length))
        val buttons = properties.getButtons()
        for(i in 0 until buttons.length()){
            val obj = buttons.getJSONObject(i)
            val btnProp = ButtonProperties(obj,properties)
            if(length<btnProp.btnPosition) {
                arrayList.addAll(arrayOfNulls(btnProp.btnPosition-length+1))
                length = btnProp.btnPosition
            }
            arrayList[btnProp.btnPosition] = btnProp
        }

        adapter = ButtonsGridAdapter(arrayList,this)

        layout_del_button.setOnDragListener { _, event ->
            val view = event.localState as RemoteButton
            when (event?.action) {
                DragEvent.ACTION_DROP -> {
                    AlertDialog.Builder(context)
                        .setTitle("Confirm")
                        .setMessage("This action can't be undone.\nAre you sure you want to delete this button?")
                        .setNegativeButton("cancel") { dialog, _ -> dialog.dismiss()}
                        .setPositiveButton("delete"){ dialog, _ ->
                            properties.removeButton(view.getProperties().jsonObj)
                            arrayList[view.getProperties().btnPosition] = null
                            adapter.notifyDataSetChanged()
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
            true
        }

        buttons_layout_recycler_view.layoutManager = GridLayoutManager(context,MainActivity.NUM_COLUMNS)
        buttons_layout_recycler_view.adapter = adapter

        if(mode == MODE_EDIT) {
            fam_manage_button_actions.hideMenuButton(false)
            Handler().postDelayed({
                fam_manage_button_actions.showMenuButton(true)
            },800)
            fam_manage_button_actions.setClosedOnTouchOutside(true)
            setOnFabClickListener(fab_new_button,ButtonPropertiesDialog.MODE_SINGLE)
            setOnFabClickListener(fab_multi_capture,ButtonPropertiesDialog.MODE_MULTI)
        }
    }

    private fun setOnFabClickListener(fab:FloatingActionButton,mode:Int){
        fab.setOnClickListener {
            val dialog = ButtonPropertiesDialog(context, this,mode)
            dialog.show()
            dialog.captureInit(null)
            fam_manage_button_actions.close(true)
        }
    }

    override fun onSelected(prop: JSONObject) {
        try{
            val pos = prop.getInt("btnPosition")
            val btnProp = ButtonProperties(prop,properties)
            arrayList[pos] = btnProp
            adapter.notifyDataSetChanged()
        }catch(ex:JSONException){
            val pos=adapter.getGetEmptyPosition()
            prop.put("btnPosition",pos)
            val btnProp = ButtonProperties(prop,properties)
            arrayList[pos] = btnProp
            adapter.notifyDataSetChanged()
        }
    }

    companion object{
        const val MODE_EDIT = 1
        const val MODE_VIEW_ONLY = 0
    }
}

