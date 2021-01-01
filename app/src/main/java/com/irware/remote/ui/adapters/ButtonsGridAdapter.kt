package com.irware.remote.ui.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.irware.remote.ButtonWidgetProvider
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.WidgetConfiguratorActivity
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.net.IrSendListener
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.buttons.RemoteButton
import com.irware.remote.ui.dialogs.RemoteDialog
import kotlinx.android.synthetic.main.create_remote_layout.*
import org.json.JSONException
import org.json.JSONObject


class ButtonsGridAdapter(private var arrayList:ArrayList<ButtonProperties?>, private val remoteDialog:RemoteDialog, private val address: String,
                         private  val userName: String, private val password: String) :RecyclerView.Adapter<ButtonsGridAdapter.ViewHolder>(),
    View.OnDragListener,View.OnLongClickListener{

    class ViewHolder(var container: LinearLayout,var button:RemoteButton):RecyclerView.ViewHolder(container)
    private val vibe = remoteDialog.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private val anim = AnimationUtils.loadAnimation(remoteDialog.context, R.anim.anim_button_show)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val container = LinearLayout(context)
        container.gravity = Gravity.CENTER
        container.layoutParams = LinearLayout.LayoutParams(RemoteButton.BTN_WIDTH+20, LinearLayout.LayoutParams.WRAP_CONTENT)
        container.minimumHeight = RemoteButton.MIN_HEIGHT
        container.id = 0
        val btn = RemoteButton(context)
        if(remoteDialog.mode == RemoteDialog.MODE_VIEW_EDIT){
            btn.setOnLongClickListener(this)
        }
        btn.setOnClickListener{
            if(remoteDialog.mode == RemoteDialog.MODE_VIEW_EDIT){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibe.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                }else{
                    with(vibe) {
                        @Suppress("DEPRECATION")
                        vibrate(50)
                    }
                }
                SocketClient.sendIrCode(address, userName , password,(it as RemoteButton).getProperties().jsonObj, object : IrSendListener {
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
        container.addView(btn)
        container.setOnDragListener(this)
        return ViewHolder(container,btn)
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val container = holder.container
        container.id = position
        val context = holder.container.context
        val buttonProp = arrayList[position]
        holder.button.initialize(buttonProp)
        if(arrayList[position] == null) return

        container.layoutParams = LinearLayout.LayoutParams(RemoteButton.BTN_WIDTH+20, LinearLayout.LayoutParams.WRAP_CONTENT)
        if (buttonProp!!.buttonShowAnimation)
            holder.button.startAnimation(anim)
        else buttonProp.buttonShowAnimation = true
        if(remoteDialog.mode == RemoteDialog.MODE_SELECT_BUTTON){
            holder.button.setOnClickListener {
                associateWidget(context,buttonProp)
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun associateWidget(context: Context, buttonProp:ButtonProperties){
        val pref = context.getSharedPreferences("widget_associations",Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString(WidgetConfiguratorActivity.activity?.widgetId.toString(),buttonProp.parent?.remoteConfigFile?.name+","+buttonProp.buttonId)
        editor.commit()
        updateWidgets()
        Handler().postDelayed({
            WidgetConfiguratorActivity.activity?.finish()
        },20)
    }

    fun getGetEmptyPosition():Int{
        arrayList.withIndex().forEach {
            indexedValue: IndexedValue<ButtonProperties?> -> if(indexedValue.value == null) return indexedValue.index
        }
        arrayList.add(null)
        return arrayList.size-1
    }

    private fun notifyItemChanged(position:Int, animation:Boolean){
        arrayList[position]?.buttonShowAnimation = animation
        notifyItemChanged(position)
    }
    /*
     * Notify adapter without loading item animation
     */
    fun notifyDataSetChanged(animation:Boolean){
        if(!animation) arrayList.forEach { it?.buttonShowAnimation = false }
        notifyDataSetChanged()
    }

    override fun onDrag(lin: View, event: DragEvent): Boolean {
        lin as LinearLayout
        when (event.action) {
            DragEvent.ACTION_DROP -> {
                val origBtn = event.localState as RemoteButton
                lin.post {
                    val prop = origBtn.getProperties()
                    if(arrayList[lin.id] == null) {
                        arrayList[lin.id] = prop
                        arrayList[prop.btnPosition] = null
                        prop.btnPosition = lin.id
                        notifyDataSetChanged(false)
                    }
                }
            }

            DragEvent.ACTION_DRAG_ENDED ->{
                val view = event.localState as RemoteButton?
                notifyItemChanged(view?.getProperties()?.btnPosition?:0,false)
                lin.background = null
                remoteDialog.image_view_delete.visibility = View.INVISIBLE
                remoteDialog.image_view_home.visibility = View.INVISIBLE
                remoteDialog.image_view_btn_settings.visibility = View.INVISIBLE
                remoteDialog.create_remote_info_layout.visibility = View.VISIBLE
            }

            DragEvent.ACTION_DRAG_ENTERED ->{
                lin.background = when{
                    lin.getChildAt(0)?.visibility == View.VISIBLE -> null
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP-> ContextCompat.getDrawable(lin.context, R.drawable.round_corner)
                    else -> with(lin) {
                        @Suppress("DEPRECATION")
                        context.resources.getDrawable(R.drawable.round_corner)
                    }
                }


            }

            DragEvent.ACTION_DRAG_EXITED -> {
                lin.background = null
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
        remoteDialog.image_view_home.visibility = View.VISIBLE
        remoteDialog.image_view_btn_settings.visibility = View.VISIBLE
        remoteDialog.create_remote_info_layout.visibility = View.INVISIBLE
        return true
    }

    private fun updateWidgets(){
        val intent = Intent(remoteDialog.context, ButtonWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val  ids = AppWidgetManager.getInstance(remoteDialog.context).getAppWidgetIds(ComponentName(remoteDialog.context,ButtonWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        remoteDialog.context.sendBroadcast(intent)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, WidgetConfiguratorActivity.activity!!.widgetId)
        }
        WidgetConfiguratorActivity.activity!!.setResult(Activity.RESULT_OK, resultValue)
        WidgetConfiguratorActivity.activity!!.finish()
    }
}
