package com.irware.remote.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.DrawableCompat
import com.github.clans.fab.FloatingActionButton
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.listeners.ButtonLongClickListener
import com.irware.remote.net.IrSendListener
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.adapters.ButtonLayoutAdapter
import com.irware.remote.ui.buttons.RemoteButton
import org.json.JSONException
import org.json.JSONObject

class RemoteDialog(context: Context,private val properties:RemoteProperties, private val mode:Int) : Dialog(context, R.style.AppTheme),OnSelectedListener,
    View.OnDragListener {

    private var lv:ListView? = null
    val infoTextVew:TextView
    private val delLayout:LinearLayout
    val delView:ImageView

    init {
        window?.attributes?.windowAnimations = R.style.DialogAnimationTheme
        setContentView(R.layout.create_remote_layout)
        val layoutList = ArrayList<LinearLayout>()
        lv = findViewById<ListView>(R.id.btn_layout_listview)

        val fab = findViewById<FloatingActionButton>(R.id.fab_new_button)
        if(mode == MODE_VIEW_ONLY) {
            fab.visibility = View.GONE
            findViewById<TextView>(R.id.create_remote_info_layout).visibility = View.GONE
        }


        delLayout = findViewById(R.id.layout_del_button)
        delView = findViewById(R.id.image_view_delete)
        infoTextVew = findViewById(R.id.create_remote_info_layout)

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
                    infoTextVew.visibility = View.VISIBLE
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

        val layoutCount = MainActivity.size.y / (RemoteButton.MIN_HIGHT + 10)
        for (i in 0 until layoutCount) {
            val layout = LinearLayout(context)
            layout.layoutParams = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT)
            layout.gravity = Gravity.CENTER
            layout.orientation = LinearLayout.HORIZONTAL
            for (j in 0 until MainActivity.size.x / (RemoteButton.BTN_WIDTH)) {
                val child = com.irware.remote.ui.LinearLayout(context)
                child.gravity = Gravity.CENTER
                child.position = (i*(MainActivity.size.x / (RemoteButton.BTN_WIDTH))) + j
                val layoutParams = LinearLayout.LayoutParams(RemoteButton.BTN_WIDTH+20, LinearLayout.LayoutParams.MATCH_PARENT)
                child.layoutParams = layoutParams
                child.minimumHeight = RemoteButton.MIN_HIGHT
                child.setOnDragListener(this)
                layout.addView(child)
            }
            layoutList.add(layout)
        }
        val adapter = ButtonLayoutAdapter(layoutList,this)
        lv!!.adapter = adapter

        if(mode == MODE_EDIT) fab.setOnClickListener {
            val dialog = ButtonPropertiesDialog(context, this)
            dialog.show()
            dialog.captureInit(null)
        }
        val buttons = properties.getButtons()

        if(buttons.length() > 0){
            for(i in 0 until buttons.length()){
                val obj = buttons.getJSONObject(i)
                val btnProp = ButtonProperties(obj,properties)
                val btn= RemoteButton(context,btnProp)
                if(mode == MODE_EDIT) btn.setOnLongClickListener(ButtonLongClickListener(this))

                (lv?.adapter as ButtonLayoutAdapter).getChildLayout(btn.getProperties().btnPosition).addView(btn)

                val vibe = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                btn.setOnClickListener{
                    when(mode){
                        MODE_EDIT ->{
                            val dialog = ButtonPropertiesDialog(context, this)
                            dialog.show()
                            dialog.onIrRead((it as RemoteButton).getProperties().jsonObj)
                        }
                        MODE_VIEW_ONLY -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibe.vibrate(VibrationEffect.createOneShot(50,VibrationEffect.DEFAULT_AMPLITUDE))
                            }else{
                                vibe.vibrate(50)
                            }
                            SocketClient.sendIrCode(obj, object : IrSendListener {
                                override fun onIrSend(result: String) {
                                    MainActivity.activity?.runOnUiThread {
                                        try {
                                            val jsonObj = JSONObject(result)
                                            Toast.makeText(context, jsonObj.getString("response"), Toast.LENGTH_SHORT).show()
                                        } catch (ex: JSONException) {
                                            Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    override fun onSelected(prop: JSONObject) {
        try{
            val pos = prop.getInt("btnPosition")
            val btnProp = ButtonProperties(prop,properties)
            (((lv?.adapter as ButtonLayoutAdapter).getChildLayout(pos) as com.irware.remote.ui.LinearLayout)
                .getChildAt(0) as RemoteButton).setButtonProperties(btnProp)
        }catch(ex:JSONException){
            val pos=(lv?.adapter as ButtonLayoutAdapter).getGetEmptyPosition()
            prop.put("btnPosition",pos)
            val btnProp = ButtonProperties(prop,properties)
            val btn = RemoteButton(context,btnProp)
            btn.setOnClickListener{
                val dialog = ButtonPropertiesDialog(context, this)
                dialog.show()
                dialog.onIrRead((it as RemoteButton).getProperties().jsonObj)
            }
            btn.setOnLongClickListener(ButtonLongClickListener(this))
            (lv?.adapter as ButtonLayoutAdapter).getChildLayout(btn.getProperties().btnPosition).addView(btn)
        }
    }

    override fun onDrag(v: View, event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DROP -> {
                val view = event.localState as RemoteButton
                val owner = view.parent as ViewGroup
                val container = v as com.irware.remote.ui.LinearLayout
                if(container.childCount==0) {
                    owner.removeView(view)
                    container.addView(view)
                    view.getProperties().btnPosition = container.position
                }
                view.visibility = View.VISIBLE
            }
            DragEvent.ACTION_DRAG_ENDED ->{
                (event.localState as View).visibility=View.VISIBLE
                v.background = null
                delView.visibility = View.INVISIBLE
                infoTextVew.visibility = View.VISIBLE
            }
            DragEvent.ACTION_DRAG_ENTERED ->{
                v.background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    v.context.getDrawable(R.drawable.round_corner)
                else  v.context.resources.getDrawable(R.drawable.round_corner)
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                v.background = null
            }
        }
        return true
    }

    companion object{
        val MODE_EDIT = 1
        val MODE_VIEW_ONLY = 0
    }
}

