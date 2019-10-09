package com.irware.remote.ui.dialogs

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.*
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.net.IrCodeListener
import com.irware.remote.ui.adapters.ButtonColorAdapter
import com.irware.remote.ui.adapters.ButtonStyleAdapter
import com.irware.remote.ui.buttons.RemoteButton
import android.view.ViewTreeObserver
import com.irware.remote.net.SocketClient
import org.json.JSONObject


class ButtonPropertiesDilog(context:Context,listener:OnSelectedListener): AlertDialog(context),IrCodeListener {
    var listener=listener
    var buttonPositive: Button
    var buttonNegative:Button
    val colorDrawableList=intArrayOf(R.drawable.round_btn_bg_grey,R.drawable.round_btn_bg_blue,
        R.drawable.round_btn_bg_green, R.drawable.round_btn_bg_yellow,
        R.drawable.round_btn_bg_red)
    val iconDrawableList=intArrayOf(0,android.R.drawable.ic_lock_power_off, android.R.drawable.btn_plus,
        android.R.drawable.btn_minus,android.R.drawable.stat_notify_call_mute,
        android.R.drawable.ic_menu_info_details,android.R.drawable.arrow_up_float,
        android.R.drawable.arrow_down_float,android.R.drawable.ic_media_play,
        android.R.drawable.ic_media_pause,android.R.drawable.ic_media_next,
        android.R.drawable.ic_media_previous,android.R.drawable.ic_input_delete)
    val styleParamList=ArrayList<AbsListView.LayoutParams>()

    init{
        setView(layoutInflater.inflate(R.layout.create_button_dialog_layout,null))
        setButton(DialogInterface.BUTTON_NEGATIVE,"cancel") { dialog, which -> dialog!!.dismiss() }
        setButton(DialogInterface.BUTTON_POSITIVE,"add"){dialog, which -> }
        show()
        setCanceledOnTouchOutside(false)
        setTitle("Capture IR Code")
        buttonPositive=getButton(DialogInterface.BUTTON_POSITIVE)
        buttonNegative=getButton(DialogInterface.BUTTON_NEGATIVE)
        buttonPositive.visibility=View.GONE
        findViewById<LinearLayout>(R.id.ir_capture_layout)!!.visibility =View.VISIBLE
        findViewById<LinearLayout>(R.id.button_prop_table)!!.visibility=View.GONE

        var pref=context.getSharedPreferences("ip_config",Context.MODE_PRIVATE)
        SocketClient.readIrCode(this,pref)
    }

    override fun onIrRead(code: String){

        var obj=JSONObject(code)
        var array=obj.get("code") as IntArray
        findViewById<LinearLayout>(R.id.ir_capture_layout)!!.visibility= View.GONE
        findViewById<LinearLayout>(R.id.button_prop_table)!!.visibility=View.VISIBLE
        manageButtonProperties(array)
    }

    private fun manageButtonProperties(array:IntArray){
        setTitle("New Button")

        var colorGrid=findViewById<GridView>(R.id.gridview_color)
        colorGrid!!.adapter=ButtonColorAdapter(colorDrawableList)
        colorGrid.setItemChecked(0,true)
        //colorGrid.getViewTreeObserver().addOnGlobalLayoutListener(GridViewProperHeight(colorGrid))

        styleParamList.add(AbsListView.LayoutParams(RemoteButton.MIN_HIGHT -(2* RemoteButton.BTN_PADDING), RemoteButton.MIN_HIGHT -(2* RemoteButton.BTN_PADDING)))
        styleParamList.add(AbsListView.LayoutParams(RemoteButton.BTN_WIDTH -(2* RemoteButton.BTN_PADDING), RemoteButton.MIN_HIGHT -(2* RemoteButton.BTN_PADDING)))
        styleParamList.add(AbsListView.LayoutParams(RemoteButton.MIN_HIGHT -(2* RemoteButton.BTN_PADDING), RemoteButton.BTN_WIDTH -(2* RemoteButton.BTN_PADDING)))
        styleParamList.add(AbsListView.LayoutParams(RemoteButton.BTN_WIDTH -(2* RemoteButton.BTN_PADDING), RemoteButton.BTN_WIDTH -(2* RemoteButton.BTN_PADDING)))

        var styleGrid=findViewById<GridView>(R.id.gridview_btn_style)
        styleGrid!!.adapter= ButtonStyleAdapter(styleParamList,colorDrawableList[0])
        styleGrid.setItemChecked(0,true)
        styleGrid.getViewTreeObserver().addOnGlobalLayoutListener(GridViewProperHeight(styleGrid))
        styleGrid.setOnItemClickListener { parent, view, position, id ->
            styleGrid.setItemChecked(position,true)
        }
        colorGrid.setOnItemClickListener { parent, view, position, id ->
            colorGrid.setItemChecked(position,true)
            (styleGrid.adapter as ButtonStyleAdapter).setIconres(colorDrawableList[position])
            styleGrid.invalidateViews()
        }

        var iconGrid=findViewById<GridView>(R.id.gridview_icons)
        iconGrid!!.adapter=ButtonColorAdapter(iconDrawableList)
        iconGrid.setItemChecked(0,true)
        iconGrid.getViewTreeObserver().addOnGlobalLayoutListener(GridViewProperHeight(iconGrid))
        iconGrid.setOnItemClickListener { parent, view, position, id ->
            iconGrid.setItemChecked(position,true)
        }
        buttonPositive.visibility=View.VISIBLE
        buttonPositive.setOnClickListener {
            var obj=JSONObject()
            obj.put("code",array)
            obj.put("style",styleGrid.checkedItemPosition)
            obj.put("color",colorGrid.checkedItemPosition)
            obj.put("text",findViewById<EditText>(R.id.btn_edit_text)?.text)
            Toast.makeText(context,colorGrid.checkedItemPosition.toString()+" "+iconGrid.checkedItemPosition,Toast.LENGTH_LONG).show()
            listener.onSelected(obj)
        }
    }

    override fun onBackPressed() {
        Toast.makeText(context,"Click on cancel to quit...",Toast.LENGTH_SHORT).show()
    }

    internal inner class GridViewProperHeight(gridView: GridView) : ViewTreeObserver.OnGlobalLayoutListener {
        val gridView=gridView
        override fun onGlobalLayout() {
            val adapter=gridView.adapter
            val numColumns=gridView.numColumns
            val numRows=adapter.count/numColumns
            var height=0
            for(i in 0..numRows-1){
                var tmpHeight=0
                for(j in 0..numColumns-1){
                    val ht= adapter.getView(j+(i*numColumns),null,gridView).layoutParams.height
                    if(ht>tmpHeight)
                        tmpHeight=ht
                }
                height+=tmpHeight
            }
            var tmpHeight = 0
            for (i in numColumns*numRows..adapter.count-1) {
                val ht = adapter.getView(i, null, gridView).layoutParams.height
                if (ht > tmpHeight)
                    tmpHeight = ht
            }
            Toast.makeText(context,tmpHeight.toString(),Toast.LENGTH_LONG).show()
            height += (numRows + hasToAddOne()) * gridView.horizontalSpacing+tmpHeight
            if(height>0)
                gridView.layoutParams.height=height
        }

        fun hasToAddOne():Int{
            if(gridView.adapter.count%gridView.numColumns==0)
                return 0
            return 1
        }
    }
}

private operator fun IntArray.times(count: Int): IntArray {
    var ia=IntArray(size*count)
    for(i in 1..count)
        ia.plus(this)
    return ia
}

private operator fun <E> ArrayList<E>.times(size: Int): ArrayList<E> {
    var al=ArrayList<E>()
    for(i in 1..size)
        al.addAll(this)
    return al
}

interface OnSelectedListener{
    fun onSelected(prop:JSONObject)
}