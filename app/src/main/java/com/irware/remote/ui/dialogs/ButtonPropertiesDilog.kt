package com.irware.remote.ui.dialogs

import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewTreeObserver
import android.widget.*
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.net.IrCodeListener
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.adapters.ButtonColorAdapter
import com.irware.remote.ui.adapters.ButtonStyleAdapter
import com.irware.remote.ui.buttons.RemoteButton
import org.json.JSONObject


class ButtonPropertiesDialog(context:Context, private var listener: OnSelectedListener): AlertDialog(context),IrCodeListener {

    private var buttonPositive: Button
    private var buttonNegative: Button

    private var irCapLayout:LinearLayout?
    private var btnPropTbl:LinearLayout?

    private var irProgress:ProgressBar?
    private var irCapErrLogo:ImageView?
    private var irCapStatus:TextView?
    private var irCapInst:TextView?

    private val colorDrawableList=intArrayOf(R.drawable.round_btn_bg_grey,R.drawable.round_btn_bg_blue,
        R.drawable.round_btn_bg_green, R.drawable.round_btn_bg_yellow,
        R.drawable.round_btn_bg_red)
    private val iconDrawableList=intArrayOf(0,android.R.drawable.ic_lock_power_off, android.R.drawable.btn_plus,
        android.R.drawable.btn_minus,android.R.drawable.stat_notify_call_mute,
        android.R.drawable.ic_menu_info_details,android.R.drawable.arrow_up_float,
        android.R.drawable.arrow_down_float,android.R.drawable.ic_media_play,
        android.R.drawable.ic_media_pause,android.R.drawable.ic_media_next,
        android.R.drawable.ic_media_previous,android.R.drawable.ic_input_delete)
    private val styleParamList=ArrayList<AbsListView.LayoutParams>()

    init{
        setView(layoutInflater.inflate(R.layout.create_button_dialog_layout,null))
        setButton(DialogInterface.BUTTON_NEGATIVE,"cancel") { dialog, _ -> dialog!!.dismiss() }
        setButton(DialogInterface.BUTTON_POSITIVE,"add"){ _, _ -> }
        show()
        setCanceledOnTouchOutside(false)
        setTitle("Capture IR Code")
        buttonPositive=getButton(DialogInterface.BUTTON_POSITIVE)
        buttonNegative=getButton(DialogInterface.BUTTON_NEGATIVE)

        irCapLayout = findViewById<LinearLayout>(R.id.ir_capture_layout)
        btnPropTbl = findViewById<LinearLayout>(R.id.button_prop_table)

        irProgress = findViewById<ProgressBar>(R.id.ir_capture_progress)
        irCapErrLogo = findViewById<ImageView>(R.id.ir_capture_error_logo)
        irCapStatus = findViewById<TextView>(R.id.ir_capture_status)
        irCapInst = findViewById<TextView>(R.id.ir_capture_instruction)

        captureInit()
    }

    private fun captureInit(){
        btnPropTbl?.visibility=View.GONE
        irCapLayout?.visibility =View.VISIBLE
        buttonPositive.visibility=View.GONE

        irProgress?.visibility = View.VISIBLE
        irCapErrLogo?.visibility = View.GONE

        irCapStatus?.text = context.getString(R.string.waiting_for_ir_signal)
        irCapInst?.visibility = View.VISIBLE

        SocketClient.readIrCode(this)
    }

    override fun onIrRead(result: String){
        MainActivity.activity?.runOnUiThread {
            irCapLayout?.visibility= View.GONE
            btnPropTbl?.visibility=View.VISIBLE
            buttonPositive.text = context.getString(R.string.add_btn)
            manageButtonProperties(result)
        }
    }

    override fun onTimeout() {
        MainActivity.activity?.runOnUiThread{
            Toast.makeText(context,"TimeOut",Toast.LENGTH_LONG).show()
            irProgress?.visibility = View.GONE
            irCapErrLogo?.visibility = View.VISIBLE
            irCapStatus?.text = context.getString(R.string.ir_cap_status_timeout)
            irCapInst?.visibility = View.GONE

            buttonPositive.text = context.getString(R.string.retry)
            buttonPositive.visibility = View.VISIBLE
            buttonPositive.setOnClickListener {
                captureInit()
            }
        }
    }

    override fun onDeny() {
        MainActivity.activity?.runOnUiThread {
            Toast.makeText(context,"Authentication failed, please login again",Toast.LENGTH_LONG).show()
            irProgress?.visibility = View.GONE
            irCapErrLogo?.visibility = View.VISIBLE
            irCapInst?.visibility = View.GONE

            irCapStatus?.text = context.getString(R.string.auth_failed_login_again)

            buttonNegative.text = context.getString(R.string.exit)
            buttonNegative.setOnClickListener {
                MainActivity.activity?.finish()
            }

            buttonPositive.visibility = View.VISIBLE
            buttonPositive.text = context.getString(R.string.restart)
            buttonPositive.setOnClickListener{
                MainActivity.activity?.recreate()
            }
        }
    }
    private fun manageButtonProperties(result:String){
        setTitle("New Button")

        val colorGrid=findViewById<GridView>(R.id.gridview_color)
        colorGrid!!.adapter=ButtonColorAdapter(colorDrawableList)
        colorGrid.setItemChecked(0,true)
        //colorGrid.getViewTreeObserver().addOnGlobalLayoutListener(GridViewProperHeight(colorGrid))

        styleParamList.add(AbsListView.LayoutParams(RemoteButton.MIN_HIGHT -(2* RemoteButton.BTN_PADDING), RemoteButton.MIN_HIGHT -(2* RemoteButton.BTN_PADDING)))
        styleParamList.add(AbsListView.LayoutParams(RemoteButton.BTN_WIDTH -(2* RemoteButton.BTN_PADDING), RemoteButton.MIN_HIGHT -(2* RemoteButton.BTN_PADDING)))
        styleParamList.add(AbsListView.LayoutParams(RemoteButton.MIN_HIGHT -(2* RemoteButton.BTN_PADDING), RemoteButton.BTN_WIDTH -(2* RemoteButton.BTN_PADDING)))
        styleParamList.add(AbsListView.LayoutParams(RemoteButton.BTN_WIDTH -(2* RemoteButton.BTN_PADDING), RemoteButton.BTN_WIDTH -(2* RemoteButton.BTN_PADDING)))

        val styleGrid=findViewById<GridView>(R.id.gridview_btn_style)
        styleGrid!!.adapter= ButtonStyleAdapter(styleParamList,colorDrawableList[0])
        styleGrid.setItemChecked(0,true)
        styleGrid.viewTreeObserver.addOnGlobalLayoutListener(GridViewProperHeight(styleGrid))
        styleGrid.setOnItemClickListener { _, _, position, _ ->
            styleGrid.setItemChecked(position,true)
        }
        colorGrid.setOnItemClickListener { _, _, position, _ ->
            colorGrid.setItemChecked(position,true)
            (styleGrid.adapter as ButtonStyleAdapter).setIconres(colorDrawableList[position])
            styleGrid.invalidateViews()
        }

        val iconGrid=findViewById<GridView>(R.id.gridview_icons)
        iconGrid!!.adapter=ButtonColorAdapter(iconDrawableList)
        iconGrid.setItemChecked(0,true)
        iconGrid.viewTreeObserver.addOnGlobalLayoutListener(GridViewProperHeight(iconGrid))
        iconGrid.setOnItemClickListener { _, _, position, _ ->
            iconGrid.setItemChecked(position,true)
        }
        buttonPositive.visibility=View.VISIBLE
        buttonPositive.setOnClickListener {
            val obj=JSONObject()
            obj.put("code",result)
            obj.put("type",styleGrid.checkedItemPosition)
            obj.put("color",colorGrid.checkedItemPosition)
            obj.put("text",findViewById<EditText>(R.id.btn_edit_text)?.text)
            Toast.makeText(context,colorGrid.checkedItemPosition.toString()+" "+iconGrid.checkedItemPosition,Toast.LENGTH_LONG).show()
            listener.onSelected(obj)
            dismiss()
        }
    }

    override fun onBackPressed() {
        Toast.makeText(context,"Click on cancel to quit...",Toast.LENGTH_SHORT).show()
    }

    internal inner class GridViewProperHeight(private val gridView: GridView) : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            val adapter=gridView.adapter
            val numColumns=gridView.numColumns
            val numRows=adapter.count/numColumns
            var height=0
            for(i in 0 until numRows){
                var tmpHeight=0
                for(j in 0 until numColumns){
                    val ht= adapter.getView(j+(i*numColumns),null,gridView).layoutParams.height
                    if(ht>tmpHeight)
                        tmpHeight=ht
                }
                height+=tmpHeight
            }
            var tmpHeight = 0
            for (i in numColumns*numRows until adapter.count) {
                val ht = adapter.getView(i, null, gridView).layoutParams.height
                if (ht > tmpHeight)
                    tmpHeight = ht
            }
            Toast.makeText(context,tmpHeight.toString(),Toast.LENGTH_LONG).show()
            height += (numRows + hasToAddOne()) * gridView.horizontalSpacing+tmpHeight
            if(height>0)
                gridView.layoutParams.height=height
        }

        private fun hasToAddOne():Int{
            if(gridView.adapter.count%gridView.numColumns==0)
                return 0
            return 1
        }
    }
}

private operator fun IntArray.times(count: Int): IntArray {
    val ia=IntArray(size*count)
    for(i in 1..count)
        ia.plus(this)
    return ia
}

private operator fun <E> ArrayList<E>.times(size: Int): ArrayList<E> {
    val al=ArrayList<E>()
    for(i in 1..size)
        al.addAll(this)
    return al
}

interface OnSelectedListener{
    fun onSelected(prop:JSONObject)
}