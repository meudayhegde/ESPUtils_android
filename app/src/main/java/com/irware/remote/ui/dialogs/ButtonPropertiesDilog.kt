package com.irware.remote.ui.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.DrawableCompat
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.net.IrCodeListener
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.buttons.RemoteButton
import com.madrapps.pikolo.ColorPicker
import com.madrapps.pikolo.HSLColorPicker
import com.madrapps.pikolo.RGBColorPicker
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener
import org.json.JSONObject
import kotlin.math.roundToInt


class ButtonPropertiesDialog(context:Context, private var listener: OnSelectedListener): AlertDialog(context),IrCodeListener {
    override var parentDialog: AlertDialog? = null

    private val buttonPositive: Button
    private val buttonNegative: Button
    private val buttonNeutral: Button

    private val irCapLayout:LinearLayout
    private val btnPropTbl:LinearLayout

    private val irProgress:ProgressBar
    private val irCapErrLogo:ImageView
    private val irCapStatus:TextView
    private val irCapInst:TextView

    private val remModButton:Button
    private val iconButton: Button
    private val btnEditText : EditText
    private var colorPicker : HSLColorPicker
    private val clrPkr: ColorPicker
    private val sizeTextView:TextView

    private val styleParamList=ArrayList<RelativeLayout.LayoutParams>()

    init{
        parentDialog = this
        setView(layoutInflater.inflate(R.layout.create_button_dialog_layout,null))
        setButton(DialogInterface.BUTTON_NEUTRAL,"reCapture") { dialog, _ -> dialog!!.dismiss() }
        setButton(DialogInterface.BUTTON_NEGATIVE,"Cancel") { dialog, _ -> dialog!!.dismiss() }
        setButton(DialogInterface.BUTTON_POSITIVE,"add"){ _, _ -> }
        show()
        setCanceledOnTouchOutside(false)
        setTitle("Capture IR Code")
        buttonPositive=getButton(DialogInterface.BUTTON_POSITIVE)
        buttonNegative=getButton(DialogInterface.BUTTON_NEGATIVE)
        buttonNeutral = getButton(DialogInterface.BUTTON_NEUTRAL)

        irCapLayout = findViewById<LinearLayout>(R.id.ir_capture_layout)!!
        btnPropTbl = findViewById<LinearLayout>(R.id.button_prop_layout)!!

        irProgress = findViewById<ProgressBar>(R.id.ir_capture_progress)!!
        irCapErrLogo = findViewById<ImageView>(R.id.ir_capture_error_logo)!!
        irCapStatus = findViewById<TextView>(R.id.ir_capture_status)!!
        irCapInst = findViewById<TextView>(R.id.ir_capture_instruction)!!
        btnEditText = findViewById<EditText>(R.id.btn_edit_text)!!
        val colorPickerLayout = findViewById<RelativeLayout>(R.id.layout_color_picker)!!

        window?.setBackgroundDrawableResource(R.drawable.layout_border_round_corner)
        window?.setLayout((MainActivity.size.x*0.86).toInt(),WindowManager.LayoutParams.WRAP_CONTENT)

        colorDrawable.cornerRadius = 100F
        colorDrawable.shape = GradientDrawable.RECTANGLE
        colorDrawable.setStroke(2,MainActivity.colorOnBackground)
        colorDrawable.setColor(colorSelected)

        colorPicker = HSLColorPicker(context)
        colorPickerLayout.addView(colorPicker)

        val layoutParam = RelativeLayout.LayoutParams((MainActivity.size.x*0.8F).roundToInt(),(MainActivity.size.x*0.8F).roundToInt())
        layoutParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        colorPicker.layoutParams = layoutParam

        clrPkr = RGBColorPicker(context)
        colorPickerLayout.addView(clrPkr)

        val lparam = RelativeLayout.LayoutParams((MainActivity.size.x*0.55F).roundToInt(),(MainActivity.size.x*0.55F).roundToInt())
        lparam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        clrPkr.layoutParams = lparam

        remModButton = findViewById<Button>(R.id.remote_model_button)!!
        iconButton = findViewById<Button>(R.id.btn_icon)!!

        clrPkr.setColorSelectionListener(object:SimpleColorSelectionListener(){
            override fun onColorSelected(color: Int) {
                colorContentSelected = color
                remModButton.setTextColor(color)
                setIconDrawable(iconSelected)
            }
        })

        sizeTextView = findViewById<TextView>(R.id.text_ir_size)!!

        remModButton.background = colorDrawable

        btnEditText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                remModButton.text = s
            }
        })
    }

    fun captureInit(jsonObj:JSONObject?){
        btnPropTbl.visibility=View.GONE
        irCapLayout.visibility =View.VISIBLE
        buttonPositive.visibility=View.GONE
        buttonNeutral.visibility = View.GONE

        irProgress.visibility = View.VISIBLE
        irCapErrLogo.visibility = View.GONE

        irCapStatus.text = context.getString(R.string.waiting_for_ir_signal)
        irCapInst.visibility = View.VISIBLE

        SocketClient.readIrCode(this,jsonObj)
    }

    override fun onIrRead(jsonObj:JSONObject) {
        MainActivity.activity?.runOnUiThread {
            irCapLayout.visibility= View.GONE
            btnPropTbl.visibility=View.VISIBLE
            buttonPositive.text = context.getString(R.string.apply)
            manageButtonProperties(jsonObj)
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
                captureInit(null)
            }
        }
    }

    override fun onDeny(err_info:String?) {
        MainActivity.activity?.runOnUiThread {
            Toast.makeText(context,err_info,Toast.LENGTH_LONG).show()
            irProgress.visibility = View.GONE
            irCapErrLogo.visibility = View.VISIBLE
            irCapInst.visibility = View.GONE

            irCapStatus.text = err_info

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

    @SuppressLint("InflateParams")
    private fun manageButtonProperties(jsonObj:JSONObject){
        setTitle("Set Button Properties")

        buttonNeutral.visibility = View.VISIBLE
        buttonNeutral.setOnClickListener {
            captureInit(jsonObj)
        }

        styleParamList.add(RelativeLayout.LayoutParams(RemoteButton.MIN_HIGHT , RemoteButton.MIN_HIGHT))
        styleParamList.add(RelativeLayout.LayoutParams(RemoteButton.BTN_WIDTH , RemoteButton.MIN_HIGHT))
        styleParamList.add(RelativeLayout.LayoutParams(RemoteButton.MIN_HIGHT, RemoteButton.BTN_WIDTH ))
        styleParamList.add(RelativeLayout.LayoutParams(RemoteButton.BTN_WIDTH, RemoteButton.BTN_WIDTH))

        for(param in styleParamList)
            param.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)

        btnStyle = jsonObj.getInt("iconType")
        colorSelected = jsonObj.getInt("color")
        colorDrawable.setColor(colorSelected)


        colorContentSelected = jsonObj.getInt("textColor")
        iconSelected = jsonObj.getInt("icon")
        btnEditText.setText(jsonObj.getString("text"))
        setIconDrawable(iconSelected)

        remModButton.layoutParams = styleParamList[btnStyle]

        colorPicker.setColor(colorSelected)
        colorPicker.setColorSelectionListener(object : SimpleColorSelectionListener() {
            override fun onColorSelected(color: Int) {
                colorSelected = color
                colorDrawable.setColor(color)
            }
        })

        val text = context.resources.getString(R.string.length_of_captured_ir_signal) + " "+jsonObj.getString("length").toLong(16)+" pulses"
        sizeTextView.text = text
        sizeTextView.setOnLongClickListener{
            Toast.makeText(context,"Protocol :"+jsonObj.getString("protocol")+" "+jsonObj.getString("irCode").replace(" ","").replace("\n",""),Toast.LENGTH_LONG).show()
            true
        }

        remModButton.setOnClickListener {
            val popup = PopupMenu(context, remModButton)
            //Inflating the Popup using xml file
            popup.menuInflater.inflate(R.menu.btn_style_menu, popup.menu)

            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.round_button_small -> btnStyle = RemoteButton.TYPE_ROUND_MINI
                    R.id.button_horizontal -> btnStyle = RemoteButton.TYPE_RECT_HOR
                    R.id.button_vertical -> btnStyle = RemoteButton.TYPE_RECT_VER
                    R.id.round_button_large -> btnStyle = RemoteButton.TYPE_ROUND_MEDIUM
                }
                remModButton.layoutParams = styleParamList[btnStyle]
                setIconDrawable(iconSelected)
                true }

            popup.show()//showing popup menu
        }

        remModButton.setTextColor(colorContentSelected)
        clrPkr.setColor(colorContentSelected)


        iconButton.setOnClickListener {
            val iconAdapter = object:ArrayAdapter<Int>(context,R.layout.drawable_layout,MainActivity.iconDrawableList.toTypedArray()){
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) context.getDrawable(getItem(position)!!)
                        else with(context) { resources.getDrawable(getItem(position)!!) }
                    DrawableCompat.setTint(drawable!!,MainActivity.colorOnBackground)
                    var returnView = convertView
                    if(returnView==null){
                        returnView = layoutInflater.inflate(R.layout.drawable_layout,null)
                    }
                    val iv = (returnView as LinearLayout).findViewById<ImageView>(R.id.btn_icon_grid)
                    val lparam = LinearLayout.LayoutParams(MainActivity.size.x/(MainActivity.NUM_COLUMNS+2),MainActivity.size.x/(MainActivity.NUM_COLUMNS+2))
                    iv.setImageDrawable(drawable)
                    iv.layoutParams = lparam
                    return returnView
                }
            }
            val iconGridLayout = layoutInflater.inflate(R.layout.icon_grid_dialog,null) as LinearLayout
            val iconGrid = iconGridLayout.findViewById<GridView>(R.id.icon_grid)
            iconGrid.setSelection(iconSelected);iconGrid.adapter = iconAdapter
            val dialog = Builder(context).setView(iconGridLayout).setTitle("Button Icon").show()
            dialog.window?.setLayout((MainActivity.size.x*0.85).toInt(),WindowManager.LayoutParams.WRAP_CONTENT)
            dialog.window?.setBackgroundDrawableResource(R.drawable.layout_border_round_corner)

            iconGrid.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                setIconDrawable(position)
                dialog.dismiss()
            }
        }

        buttonPositive.visibility=View.VISIBLE
        buttonPositive.setOnClickListener {
            jsonObj.put("text",findViewById<EditText>(R.id.btn_edit_text)?.text.toString())
            jsonObj.put("iconType",btnStyle)
            jsonObj.put("color",colorSelected)
            jsonObj.put("icon",iconSelected)
            jsonObj.put("textColor",colorContentSelected)

            listener.onSelected(jsonObj)
            dismiss()
        }
    }

    fun setIconDrawable(position:Int){
        iconSelected = position
        if(iconSelected == 0) remModButton.setCompoundDrawablesWithIntrinsicBounds(null,null,null,null)
        else {
            val drawable =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) context.getDrawable(
                    MainActivity.iconDrawableList[iconSelected])
                else with(context) { resources.getDrawable(MainActivity.iconDrawableList[iconSelected]) }
            DrawableCompat.setTint(drawable!!, colorContentSelected)
            if (btnStyle == RemoteButton.TYPE_RECT_VER){
                remModButton.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
                remModButton.setPadding(0,0,0,0)
            }
            else{
                remModButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                remModButton.setPadding(8,0,0,0)
            }

        }
    }

    override fun onBackPressed() {
        Toast.makeText(context,"Click on cancel to quit...",Toast.LENGTH_SHORT).show()
    }

    companion object{
        var colorSelected:Int = Color.GRAY
        var colorContentSelected = Color.WHITE
        var iconSelected = 0
        var btnStyle = RemoteButton.TYPE_RECT_HOR
        private val colorDrawable = GradientDrawable()
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