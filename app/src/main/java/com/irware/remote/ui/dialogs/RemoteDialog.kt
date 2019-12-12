package com.irware.remote.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.clans.fab.FloatingActionButton
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.ui.adapters.ButtonsGridAdapter
import com.irware.remote.ui.buttons.RemoteButton
import org.json.JSONException
import org.json.JSONObject

class RemoteDialog(context: Context,private val properties:RemoteProperties, val mode:Int) : Dialog(context,R.style.AppTheme),OnSelectedListener {

    val infoTextView:TextView
    private val delLayout:LinearLayout
    val delView:ImageView
    private val recyclerView:RecyclerView
    private val arrayList:ArrayList<ButtonProperties?> = ArrayList()
    private val adapter:ButtonsGridAdapter

    init {
        window?.attributes?.windowAnimations = R.style.DialogAnimationTheme
        setContentView(R.layout.create_remote_layout)
        recyclerView = findViewById(R.id.buttons_layout_recycler_view)

        val fab = findViewById<FloatingActionButton>(R.id.fab_new_button)
        if(mode == MODE_VIEW_ONLY) {
            fab.visibility = View.GONE
            findViewById<TextView>(R.id.create_remote_info_layout).visibility = View.GONE
        }


        delLayout = findViewById(R.id.layout_del_button)
        delView = findViewById(R.id.image_view_delete)
        infoTextView = findViewById(R.id.create_remote_info_layout)

        delLayout.setOnDragListener { _, event ->
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
                    delView.visibility = View.INVISIBLE
                    infoTextView.visibility = View.VISIBLE
                    DrawableCompat.setTint(delView.drawable,MainActivity.colorOnBackground)
                }
                DragEvent.ACTION_DRAG_ENTERED ->{
                    DrawableCompat.setTint(delView.drawable,Color.RED)
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    DrawableCompat.setTint(delView.drawable, MainActivity.colorOnBackground)
                }
            }
            true
        }

        val length = MainActivity.NUM_COLUMNS*MainActivity.size.y/RemoteButton.MIN_HEIGHT
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
                }
                arrayList[btnProp.btnPosition] = btnProp
            }
        }

        adapter = ButtonsGridAdapter(arrayList,this)
        recyclerView.layoutManager = GridLayoutManager(context,MainActivity.NUM_COLUMNS)
        recyclerView.adapter = adapter

        if(mode == MODE_EDIT) fab.setOnClickListener {
            val dialog = ButtonPropertiesDialog(context, this)
            dialog.show()
            dialog.captureInit(null)
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

