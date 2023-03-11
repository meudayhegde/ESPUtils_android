package com.github.meudayhegde.esputils.ui.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.*
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.RemoteBtnWidgetConfActivity
import com.github.meudayhegde.esputils.RemoteButtonWidget
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.holders.ButtonProperties
import com.github.meudayhegde.esputils.net.SocketClient
import com.github.meudayhegde.esputils.ui.buttons.RemoteButton
import com.github.meudayhegde.esputils.ui.dialogs.RemoteDialog
import org.json.JSONException
import org.json.JSONObject


class ButtonsGridAdapter(private var arrayList: ArrayList<ButtonProperties?>, private val remoteDialog: RemoteDialog, private val address: String,
                         private  val userName: String, private val password: String): RecyclerView.Adapter<ButtonsGridAdapter.ButtonGridViewHolder>(),
    View.OnDragListener, View.OnLongClickListener{

    class ButtonGridViewHolder(var container: LinearLayout, var button: RemoteButton): RecyclerView.ViewHolder(container)
    private val vibe = remoteDialog.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private val anim = AnimationUtils.loadAnimation(remoteDialog.context, R.anim.anim_button_show)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonGridViewHolder {
        val context = parent.context
        val container = LinearLayout(context)
        container.gravity = Gravity.CENTER
        container.layoutParams = LinearLayout.LayoutParams(RemoteButton.BTN_WIDTH + 20, LinearLayout.LayoutParams.WRAP_CONTENT)
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
                        vibrate(50)
                    }
                }
                SocketClient.sendIrCode(address, userName , password, (it as RemoteButton).getProperties().jsonObj) { result ->
                    Handler(Looper.getMainLooper()).post{
                        try {
                            val jsonObj = JSONObject(result)
                            Toast.makeText(context, jsonObj.getString(Strings.espResponse), Toast.LENGTH_SHORT).show()
                        } catch (ex: JSONException) {
                            Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        container.addView(btn)
        container.setOnDragListener(this)
        return ButtonGridViewHolder(container,btn)
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    override fun onBindViewHolder(holder: ButtonGridViewHolder, position: Int) {
        val container = holder.container
        container.id = position
        val context = holder.container.context
        val buttonProp = arrayList[position]
        holder.button.initialize(buttonProp)
        if(arrayList[position] == null) return

        container.layoutParams = LinearLayout.LayoutParams(RemoteButton.BTN_WIDTH + 20, LinearLayout.LayoutParams.WRAP_CONTENT)
        if (buttonProp!!.buttonShowAnimation)
            holder.button.startAnimation(anim)
        else buttonProp.buttonShowAnimation = true
        if(remoteDialog.mode == RemoteDialog.MODE_SELECT_BUTTON){
            holder.button.setOnClickListener {
                associateWidget(context, buttonProp)
            }
        }
    }

    private fun associateWidget(context: Context, buttonProp: ButtonProperties){
        val pref = context.getSharedPreferences(Strings.sharedPrefNameWidgetAssociations, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString(RemoteBtnWidgetConfActivity.activity?.widgetId.toString(), buttonProp.parent?.remoteConfigFile?.name + "," + buttonProp.buttonId)
        editor.apply()
        updateWidgets()
        Handler(Looper.getMainLooper()).postDelayed({
            RemoteBtnWidgetConfActivity.activity?.finish()
        }, 20)
    }

    fun getEmptyPosition():Int{
        arrayList.withIndex().forEach {
            indexedValue: IndexedValue<ButtonProperties?> -> if(indexedValue.value == null) return indexedValue.index
        }
        arrayList.add(null)
        return arrayList.size-1
    }

    private fun notifyItemChanged(position: Int, animation: Boolean){
        arrayList[position]?.buttonShowAnimation = animation
        notifyItemChanged(position)
    }
    /*
     * Notify adapter without loading item animation
     */
    fun notifyDataSetChanged(animation: Boolean){
        if(!animation) arrayList.forEach { it?.buttonShowAnimation = false }
        notifyDataSetChanged()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
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
                remoteDialog.dialogBinding.imageViewDelete.visibility = View.INVISIBLE
                remoteDialog.dialogBinding.imageViewHome.visibility = View.INVISIBLE
                remoteDialog.dialogBinding.imageViewBtnSettings.visibility = View.INVISIBLE
                remoteDialog.dialogBinding.createRemoteInfoLayout.visibility = View.VISIBLE
            }

            DragEvent.ACTION_DRAG_ENTERED ->{
                lin.background = when (lin.getChildAt(0)?.visibility) {
                    View.VISIBLE -> null
                    else -> ContextCompat.getDrawable(
                        lin.context,
                        R.drawable.layout_border_round_corner
                    )
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
        remoteDialog.dialogBinding.imageViewDelete.visibility = View.VISIBLE
        remoteDialog.dialogBinding.imageViewHome.visibility = View.VISIBLE
        remoteDialog.dialogBinding.imageViewBtnSettings.visibility = View.VISIBLE
        remoteDialog.dialogBinding.createRemoteInfoLayout.visibility = View.INVISIBLE
        return true
    }

    private fun updateWidgets(){
        val intent = Intent(remoteDialog.context, RemoteButtonWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val  ids = AppWidgetManager.getInstance(remoteDialog.context).getAppWidgetIds(ComponentName(remoteDialog.context,RemoteButtonWidget::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        remoteDialog.context.sendBroadcast(intent)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, RemoteBtnWidgetConfActivity.activity!!.widgetId)
        }
        RemoteBtnWidgetConfActivity.activity!!.setResult(Activity.RESULT_OK, resultValue)
        RemoteBtnWidgetConfActivity.activity!!.finish()
    }
}
