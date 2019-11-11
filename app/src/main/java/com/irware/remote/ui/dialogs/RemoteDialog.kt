package com.irware.remote.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.AbsListView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import com.github.clans.fab.FloatingActionButton
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.listeners.ButtonDragListener
import com.irware.remote.listeners.ButtonLongClickListener
import com.irware.remote.net.IrSendListener
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.adapters.ButtonLayoutAdapter
import com.irware.remote.ui.buttons.RemoteButton
import org.json.JSONException
import org.json.JSONObject

class RemoteDialog(context: Context,private val properties:RemoteProperties, private val mode:Int) : Dialog(context, R.style.AppTheme),OnSelectedListener {

    private var lv:ListView? = null
    init {
        val layoutList = ArrayList<LinearLayout>()
        setContentView(R.layout.create_remote_layout)
        lv = findViewById<ListView>(R.id.btn_layout_listview)

        val fab = findViewById<FloatingActionButton>(R.id.fab_new_button)
        if(mode == MODE_VIEW_ONLY) fab.visibility = View.GONE

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
                child.setOnDragListener(ButtonDragListener())
                layout.addView(child)
            }
            layoutList.add(layout)
        }
        val adapter = ButtonLayoutAdapter(layoutList)
        lv!!.adapter = adapter

        if(mode == MODE_EDIT) fab.setOnClickListener {
            val dialog = ButtonPropertiesDialog(context, this)
            dialog.show()
            dialog.captureInit()
        }

        val buttons = properties.getButtons()
        if(buttons.length() > 0){
            for(i in 0 until buttons.length()){
                val obj = buttons.getJSONObject(i)
                val btnProp = ButtonProperties(obj,properties)
                val btn= RemoteButton(lv?.context,btnProp)
                if(mode == MODE_EDIT) btn.setOnLongClickListener(ButtonLongClickListener())
                (lv?.adapter as ButtonLayoutAdapter).getChildLayout(btn.getProperties().btnPosition).addView(btn)
                btn.setOnClickListener{

                    when(mode){
                        MODE_EDIT ->{
                            val dialog = ButtonPropertiesDialog(context, this)
                            dialog.show()
                            dialog.onIrRead((it as RemoteButton).getProperties().jsonObj)
                        }
                        MODE_VIEW_ONLY -> {
                            SocketClient.sendIrCode(obj.getInt("length"),obj.getJSONArray("irCode"),object:IrSendListener{
                                override fun onIrSend(result:String) {
                                    MainActivity.activity?.runOnUiThread {
                                        try {
                                            val jsonObj = JSONObject(result)
                                            if (jsonObj.getString("response").equals("success", true))
                                                Toast.makeText(context, "send signal success", Toast.LENGTH_SHORT).show()
                                        }catch(ex:JSONException){
                                            Toast.makeText(context, "response unknown", Toast.LENGTH_SHORT).show()
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
            val btn = RemoteButton(lv?.context,btnProp)
            btn.setOnClickListener{
                val dialog = ButtonPropertiesDialog(context, this)
                dialog.show()
                dialog.onIrRead((it as RemoteButton).getProperties().jsonObj)
            }
            btn.setOnLongClickListener(ButtonLongClickListener())
            (lv?.adapter as ButtonLayoutAdapter).getChildLayout(btn.getProperties().btnPosition).addView(btn)
        }
    }

    companion object{
        val MODE_EDIT = 1
        val MODE_VIEW_ONLY = 0
    }
}
