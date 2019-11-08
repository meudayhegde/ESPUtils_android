package com.irware.remote.ui.dialogs

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.*
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.net.IrCodeListener
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.adapters.ButtonColorAdapter
import com.irware.remote.ui.adapters.ButtonStyleAdapter
import com.irware.remote.ui.buttons.RemoteButton
import org.json.JSONObject
import top.defaults.colorpicker.ColorPickerPopup


class ButtonPropertiesDialog(context:Context, private var listener: OnSelectedListener): AlertDialog(context),IrCodeListener {

    private val buttonPositive: Button
    private val buttonNegative: Button

    private val irCapLayout:LinearLayout
    private val btnPropTbl:LinearLayout

    private val irProgress:ProgressBar
    private val irCapErrLogo:ImageView
    private val irCapStatus:TextView
    private val irCapInst:TextView

    private val remModButton:Button
    private val colrBtn:Button
    private val styleBtn:Button
    private val iconBtn:Button

    private var colorSelected:Int = Color.GRAY
    private val colorDrawable = GradientDrawable()
    private var btnStyle = RemoteButton.TYPE_RECT_HOR

    private val colorDrawableList=intArrayOf(R.drawable.round_btn_bg_grey,R.drawable.round_btn_bg_blue,
        R.drawable.round_btn_bg_green, R.drawable.round_btn_bg_yellow,
        R.drawable.round_btn_bg_red)
    private val iconDrawableList=intArrayOf(0,android.R.drawable.ic_lock_power_off, android.R.drawable.btn_plus,
        android.R.drawable.btn_minus,android.R.drawable.stat_notify_call_mute,
        android.R.drawable.ic_menu_info_details,android.R.drawable.arrow_up_float,
        android.R.drawable.arrow_down_float,android.R.drawable.ic_media_play,
        android.R.drawable.ic_media_pause,android.R.drawable.ic_media_next,
        android.R.drawable.ic_media_previous,android.R.drawable.ic_input_delete)
    private val styleParamList=ArrayList<LinearLayout.LayoutParams>()

    init{
        setView(layoutInflater.inflate(R.layout.create_button_dialog_layout,null))
        setButton(DialogInterface.BUTTON_NEGATIVE,"cancel") { dialog, _ -> dialog!!.dismiss() }
        setButton(DialogInterface.BUTTON_POSITIVE,"add"){ _, _ -> }
        show()
        setCanceledOnTouchOutside(false)
        setTitle("Capture IR Code")
        buttonPositive=getButton(DialogInterface.BUTTON_POSITIVE)
        buttonNegative=getButton(DialogInterface.BUTTON_NEGATIVE)

        irCapLayout = findViewById<LinearLayout>(R.id.ir_capture_layout)!!
        btnPropTbl = findViewById<LinearLayout>(R.id.button_prop_layout)!!

        irProgress = findViewById<ProgressBar>(R.id.ir_capture_progress)!!
        irCapErrLogo = findViewById<ImageView>(R.id.ir_capture_error_logo)!!
        irCapStatus = findViewById<TextView>(R.id.ir_capture_status)!!
        irCapInst = findViewById<TextView>(R.id.ir_capture_instruction)!!

        remModButton = findViewById<Button>(R.id.remote_model_button)!!
        colrBtn = findViewById<Button>(R.id.btn_choose_color)!!
        iconBtn = findViewById<Button>(R.id.btn_choose_icon)!!
        styleBtn = findViewById<Button>(R.id.btn_choose_style)!!

        captureInit()
    }

    private fun captureInit(){
        btnPropTbl.visibility=View.GONE
        irCapLayout.visibility =View.VISIBLE
        buttonPositive.visibility=View.GONE

        irProgress.visibility = View.VISIBLE
        irCapErrLogo.visibility = View.GONE

        irCapStatus.text = context.getString(R.string.waiting_for_ir_signal)
        irCapInst.visibility = View.VISIBLE

        SocketClient.readIrCode(this)
    }

    override fun onIrRead(result: String){
        MainActivity.activity?.runOnUiThread {
            irCapLayout.visibility= View.GONE
            btnPropTbl.visibility=View.VISIBLE
            buttonPositive.text = context.getString(R.string.add_btn)
            manageButtonProperties(result)
        }
    }

    override fun onTimeout() {
        MainActivity.activity?.runOnUiThread{
            Toast.makeText(context,"TimeOut",Toast.LENGTH_LONG).show()
            irProgress.visibility = View.GONE
            irCapErrLogo.visibility = View.VISIBLE
            irCapStatus.text = context.getString(R.string.ir_cap_status_timeout)
            irCapInst.visibility = View.GONE

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
            irProgress.visibility = View.GONE
            irCapErrLogo.visibility = View.VISIBLE
            irCapInst.visibility = View.GONE

            irCapStatus.text = context.getString(R.string.auth_failed_login_again)

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

        colorDrawable.cornerRadius = 100F
        colorDrawable.shape = GradientDrawable.RECTANGLE
        colorDrawable.setStroke(2,Color.BLACK)
        colorDrawable.setColor(colorSelected)
        remModButton.background = colorDrawable

        styleParamList.add(LinearLayout.LayoutParams(RemoteButton.MIN_HIGHT -(2* RemoteButton.BTN_PADDING), RemoteButton.MIN_HIGHT -(2* RemoteButton.BTN_PADDING)))
        styleParamList.add(LinearLayout.LayoutParams(RemoteButton.BTN_WIDTH -(2* RemoteButton.BTN_PADDING), RemoteButton.MIN_HIGHT -(2* RemoteButton.BTN_PADDING)))
        styleParamList.add(LinearLayout.LayoutParams(RemoteButton.MIN_HIGHT -(2* RemoteButton.BTN_PADDING), RemoteButton.BTN_WIDTH -(2* RemoteButton.BTN_PADDING)))
        styleParamList.add(LinearLayout.LayoutParams(RemoteButton.BTN_WIDTH -(2* RemoteButton.BTN_PADDING), RemoteButton.BTN_WIDTH -(2* RemoteButton.BTN_PADDING)))

        remModButton.layoutParams = styleParamList[btnStyle]

        colrBtn.setOnClickListener {

            ColorPickerPopup.Builder(context)
                .initialColor(colorSelected) // Set initial color
                .enableBrightness(true) // Enable brightness slider or not
                .enableAlpha(true) // Enable alpha slider or not
                .okTitle("Choose")
                .cancelTitle("Cancel")
                .showIndicator(true)
                .showValue(true)
                .build()
                .show(colrBtn,object : ColorPickerPopup.ColorPickerObserver() {
                    override fun onColorPicked(color: Int) {
                        colorSelected = color
                        colorDrawable.setColor(color)
                    }
                })
        }

        styleBtn.setOnClickListener {
            Builder(context).setAdapter(ArrayAdapter(context,android.R.layout.simple_list_item_1,
                arrayListOf("Small Round Button","Button Horizontal","Button Vertical", "Large Button Round"))) { dialog, which ->
                btnStyle = which;remModButton.layoutParams = styleParamList[btnStyle]; dialog.dismiss()
            }.show()
        }

        val iconGrid= Spinner(context,Spinner.MODE_DIALOG)
        iconGrid.adapter=ButtonColorAdapter(iconDrawableList)
        iconGrid.setSelection(0,true)


        buttonPositive.visibility=View.VISIBLE
        buttonPositive.setOnClickListener {
            val obj=JSONObject()
            obj.put("code",result)
            obj.put("type",btnStyle)
            obj.put("color",colorSelected)
            obj.put("text",findViewById<EditText>(R.id.btn_edit_text)?.text)
            Toast.makeText(context,colorSelected.toString()+" "+iconGrid.selectedItemPosition,Toast.LENGTH_LONG).show()
            listener.onSelected(obj)
            dismiss()
        }
    }

    override fun onBackPressed() {
        Toast.makeText(context,"Click on cancel to quit...",Toast.LENGTH_SHORT).show()
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