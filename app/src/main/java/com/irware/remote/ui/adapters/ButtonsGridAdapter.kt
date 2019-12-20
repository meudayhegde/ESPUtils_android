package com.irware.remote.ui.adapters

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.net.IrSendListener
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.buttons.RemoteButton
import com.irware.remote.ui.dialogs.ButtonPropertiesDialog
import com.irware.remote.ui.dialogs.RemoteDialog
import kotlinx.android.synthetic.main.create_remote_layout.*
import org.json.JSONException
import org.json.JSONObject

class ButtonsGridAdapter(private var arrayList:ArrayList<ButtonProperties?>,private val remoteDialog:RemoteDialog) :RecyclerView.Adapter<ButtonsGridAdapter.ViewHolder>(),
    View.OnDragListener,View.OnLongClickListener{

    class ViewHolder(var container: com.irware.remote.ui.adapters.LinearLayout,var button:RemoteButton):RecyclerView.ViewHolder(container)
    private val vibe = remoteDialog.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val container = com.irware.remote.ui.adapters.LinearLayout(context)
        container.gravity = Gravity.CENTER
        val layoutParams = LinearLayout.LayoutParams(RemoteButton.BTN_WIDTH+20, LinearLayout.LayoutParams.WRAP_CONTENT)
        container.layoutParams = layoutParams
        container.minimumHeight = RemoteButton.MIN_HEIGHT
        val btn = RemoteButton(context)
        if(remoteDialog.mode == RemoteDialog.MODE_EDIT){
            btn.setOnLongClickListener(this)
        }
        btn.setOnClickListener{
            when(remoteDialog.mode){
                RemoteDialog.MODE_EDIT ->{
                    val dialog = ButtonPropertiesDialog(context, remoteDialog,ButtonPropertiesDialog.MODE_SINGLE)
                    dialog.show()
                    dialog.onIrRead((it as RemoteButton).getProperties().jsonObj)
                }
                RemoteDialog.MODE_VIEW_ONLY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibe.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    }else{
                        with(vibe) {
                            @Suppress("DEPRECATION")
                            vibrate(50)
                        }
                    }
                    SocketClient.sendIrCode((it as RemoteButton).getProperties().jsonObj, object : IrSendListener {
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
        container.addView(btn)
        container.setOnDragListener(this)
        return ViewHolder(container,btn)
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val container = holder.container
        container.position = position
        holder.button.initialize(arrayList[position])
    }

    fun getGetEmptyPosition():Int{
        for(i in 0 until arrayList.size){
            if(arrayList[i] == null){
                return i
            }
        }
        arrayList.add(null)
        return arrayList.size-1
    }

    override fun onDrag(v: View, event: DragEvent): Boolean {
        v as com.irware.remote.ui.adapters.LinearLayout
        when (event.action) {

            DragEvent.ACTION_DROP -> {
                val orig = event.localState as RemoteButton
                v.post {
                    val prop = orig.getProperties()
                    if(arrayList[v.position] == null) {
                        arrayList[v.position] = prop
                        arrayList[prop.btnPosition] = null
                        prop.btnPosition = v.position
                        notifyDataSetChanged()
                    }
                }
            }

            DragEvent.ACTION_DRAG_ENDED ->{
                val view = event.localState as View
                v.post{
                    view.visibility=View.VISIBLE
                }
                v.background = null
                remoteDialog.image_view_delete.visibility = View.INVISIBLE
                remoteDialog.create_remote_info_layout.visibility = View.VISIBLE
            }

            DragEvent.ACTION_DRAG_ENTERED ->{
                v.background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    v.context.getDrawable(R.drawable.round_corner)
                else  with(v) {
                    @Suppress("DEPRECATION")
                    context.resources.getDrawable(R.drawable.round_corner)
                }
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                v.background = null
            }
        }
        return true
    }

    override fun onLongClick(view: View?): Boolean {
        val data = ClipData.newPlainText("", "")
        val shadowBuilder = View.DragShadowBuilder(view)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            view?.startDragAndDrop(data,shadowBuilder,view,0)
        }else{
            @Suppress("DEPRECATION")
            view?.startDrag(data, shadowBuilder, view, 0)
        }
        view?.visibility = View.INVISIBLE
        remoteDialog.image_view_delete.visibility = View.VISIBLE
        remoteDialog.create_remote_info_layout.visibility = View.INVISIBLE
        return true
    }
}

class LinearLayout(context:Context):LinearLayout(context){
    var position = 0
}