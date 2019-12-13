package com.irware.remote.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.GridLayoutManager
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
            findViewById<TextView>(R.id.create_remote_info_layout).visibility = View.GONE
        }

        layout_del_button.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DROP -> {
                    val view = event.localState as RemoteButton
                    val owner = view.parent as ViewGroup
                    AlertDialog.Builder(context)
                        .setTitle("Confirm")
                        .setMessage("This action can't be undone.\nAre you sure you want to delete this button?")
                        .setNegativeButton("cancel") { dialog, _ -> dialog.dismiss()}
                        .setPositiveButton("delete"){ dialog, _ ->
                            properties.removeButton(view.getProperties().jsonObj)
                            owner.removeView(view)
                            dialog.dismiss()
                        }
                        .show()
                }
                DragEvent.ACTION_DRAG_ENDED ->{
                    image_view_delete.visibility = View.INVISIBLE
                    create_remote_info_layout.visibility = View.VISIBLE
                    DrawableCompat.setTint(image_view_delete.drawable,MainActivity.colorOnBackground)
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

        var length = MainActivity.NUM_COLUMNS*MainActivity.size.y/(RemoteButton.MIN_HEIGHT+12)
        for(i in 0 until length){
            arrayList.add(null)
        }

        val buttons = properties.getButtons()

        if(buttons.length() > 0){
            for(i in 0 until buttons.length()){
                val obj = buttons.getJSONObject(i)
                val btnProp = ButtonProperties(obj,properties)
                if(length<btnProp.btnPosition) {
                    for(j in length until btnProp.btnPosition){
                        arrayList.add(null)
                    }
                    length = btnProp.btnPosition
                }
                arrayList[btnProp.btnPosition] = btnProp
            }
        }

        adapter = ButtonsGridAdapter(arrayList,this)
        buttons_layout_recycler_view.layoutManager = GridLayoutManager(context,MainActivity.NUM_COLUMNS)
        buttons_layout_recycler_view.adapter = adapter

        if(mode == MODE_EDIT) {
            fab_new_button.setOnClickListener {
                val dialog = ButtonPropertiesDialog(context, this,ButtonPropertiesDialog.MODE_SINGLE)
                dialog.show()
                dialog.captureInit(null)
            }
            fab_multi_capture.setOnClickListener {
                val dialog = ButtonPropertiesDialog(context, this,ButtonPropertiesDialog.MODE_MULTI)
                dialog.show()
                dialog.captureInit(null)
            }
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

