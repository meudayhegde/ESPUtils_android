package com.github.meudayhegde.esputils.ui.dialogs

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.github.meudayhegde.esputils.ESPUtilsApp
import com.github.meudayhegde.esputils.MainActivity
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.databinding.CreateButtonDialogLayoutBinding
import com.github.meudayhegde.esputils.holders.ButtonProperties
import com.github.meudayhegde.esputils.listeners.IrCodeListener
import com.github.meudayhegde.esputils.listeners.OnRemoteButtonSelectedListener
import com.github.meudayhegde.esputils.net.SocketClient
import com.github.meudayhegde.esputils.ui.buttons.RemoteButton
import com.madrapps.pikolo.ColorPicker
import com.madrapps.pikolo.HSLColorPicker
import com.madrapps.pikolo.RGBColorPicker
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.roundToInt

class ButtonPropertiesDialog(
    context: Context,
    private var listener: OnRemoteButtonSelectedListener,
    override var mode: Int,
    private val address: String,
    private val userName: String,
    private val password: String
) : AlertDialog(context, R.style.AppTheme_AlertDialog), IrCodeListener {
    override var parentDialog: AlertDialog? = null

    private val buttonPositive: Button
    private val buttonNegative: Button
    private val buttonNeutral: Button

    private var colorPicker: HSLColorPicker
    private val clrPkr: ColorPicker
    private var capturedCount = 0
    private var buttonProperties: ButtonProperties? = null
    private val handler = Handler(Looper.getMainLooper())

    private val dialogBinding: CreateButtonDialogLayoutBinding

    init {
        parentDialog = this
        dialogBinding = CreateButtonDialogLayoutBinding.inflate(
            layoutInflater, window?.decorView as ViewGroup, false
        )

        setButton(
            DialogInterface.BUTTON_NEUTRAL,
            context.getString(R.string.dialog_btn_prop_button_recapture)
        ) { dialog, _ -> dialog!!.dismiss() }
        setButton(
            DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel)
        ) { dialog, _ -> dialog!!.dismiss() }
        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.add)) { _, _ -> }

        show()
        setCanceledOnTouchOutside(false)
        setTitle(R.string.dialog_title_ir_capture)
        buttonPositive = getButton(DialogInterface.BUTTON_POSITIVE)
        buttonNegative = getButton(DialogInterface.BUTTON_NEGATIVE)
        buttonNeutral = getButton(DialogInterface.BUTTON_NEUTRAL)

        val width = min(MainActivity.layoutParams.width, MainActivity.layoutParams.height)
        window?.setLayout((width * 0.86).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)

        colorPicker = HSLColorPicker(context)
        dialogBinding.layoutColorPicker.addView(colorPicker)

        val layoutParam =
            RelativeLayout.LayoutParams((width * 0.8F).roundToInt(), (width * 0.8F).roundToInt())
        layoutParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        colorPicker.layoutParams = layoutParam

        clrPkr = RGBColorPicker(context)
        dialogBinding.layoutColorPicker.addView(clrPkr)

        val lparam =
            RelativeLayout.LayoutParams((width * 0.55F).roundToInt(), (width * 0.55F).roundToInt())
        lparam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        clrPkr.layoutParams = lparam

        clrPkr.setColorSelectionListener(object : SimpleColorSelectionListener() {
            override fun onColorSelected(color: Int) {
                buttonProperties?.textColor = color
            }
        })
    }

    fun captureInit(jsonObj: JSONObject?) {
        dialogBinding.timeRemainingText.visibility = View.VISIBLE
        dialogBinding.timeRemainingText.text = ""
        dialogBinding.buttonPropLayout.visibility = View.GONE
        dialogBinding.irCaptureLayout.visibility = View.VISIBLE
        dialogBinding.irCaptureSuccessLogo.visibility = View.GONE
        buttonPositive.visibility = View.GONE
        buttonNeutral.visibility = View.GONE

        dialogBinding.irCaptureProgress.visibility = View.VISIBLE
        dialogBinding.irCaptureErrorLogo.visibility = View.GONE

        dialogBinding.irCaptureStatus.text = context.getString(R.string.message_waiting_ir_code)
        dialogBinding.irCaptureInstruction.visibility = View.VISIBLE
        if (mode == MODE_MULTI) dialogBinding.irCaptureInstruction.text =
            context.getString(R.string.message_ir_capture_instruction) + "\n" + context.getString(R.string.message_multi_capture_hint)
        SocketClient.readIrCode(address, userName, password, this, jsonObj)
    }

    override fun onIrRead(jsonObj: JSONObject) {
        handler.post {
            if (mode == MODE_SINGLE) {
                dialogBinding.irCaptureLayout.visibility = View.GONE
                dialogBinding.buttonPropLayout.visibility = View.VISIBLE
                buttonPositive.text = context.getString(R.string.apply)
                manageButtonProperties(jsonObj)
            } else {
                capturedCount++
                listener.onSelected(jsonObj)
            }

        }
    }

    override fun onTimeout() {
        handler.post {
            dialogBinding.timeRemainingText.visibility = View.GONE
            Toast.makeText(context, context.getString(R.string.timeout), Toast.LENGTH_LONG).show()
            dialogBinding.irCaptureProgress.visibility = View.GONE
            dialogBinding.irCaptureErrorLogo.visibility = View.VISIBLE
            dialogBinding.irCaptureStatus.text =
                context.getString(R.string.message_ir_cap_status_timeout)
            dialogBinding.irCaptureInstruction.visibility = View.GONE

            buttonPositive.text = context.getString(R.string.retry)
            buttonPositive.visibility = View.VISIBLE
            buttonPositive.setOnClickListener {
                captureInit(null)
            }
            textInt = 0
            if (mode == MODE_MULTI && capturedCount > 0) {
                dialogBinding.irCaptureErrorLogo.visibility = View.GONE
                dialogBinding.irCaptureSuccessLogo.visibility = View.VISIBLE
                dialogBinding.irCaptureStatus.text =
                    context.getString(R.string.message_multi_capture_count, capturedCount)
            }
        }
    }

    override fun onDeny(err_info: String?) {
        handler.post {
            dialogBinding.timeRemainingText.visibility = View.GONE
            Toast.makeText(context, err_info, Toast.LENGTH_LONG).show()
            dialogBinding.irCaptureProgress.visibility = View.GONE
            dialogBinding.irCaptureErrorLogo.visibility = View.VISIBLE
            dialogBinding.irCaptureInstruction.visibility = View.GONE
            dialogBinding.irCaptureStatus.text = err_info

            buttonNegative.text = context.getString(R.string.exit)
            buttonNegative.setOnClickListener {
                MainActivity.activity?.finish()
            }

            buttonPositive.visibility = View.VISIBLE
            buttonPositive.text = context.getString(R.string.restart)
            buttonPositive.setOnClickListener {
                MainActivity.activity?.recreate()
            }
        }
    }

    override fun onProgress(value: Int) {
        dialogBinding.timeRemainingText.text = value.toString()
    }

    private fun manageButtonProperties(jsonObj: JSONObject) {
        setTitle(context.getString(R.string.dialog_title_btn_prop))
        try {
            jsonObj.getInt(Strings.btnPropBtnPosition)
        } catch (ex: JSONException) {
            jsonObj.put(Strings.btnPropBtnPosition, -1)
        }
        ButtonPropertiesDialog.jsonObj = jsonObj
        buttonProperties = ButtonProperties(jsonObj)
        dialogBinding.remoteModelButton.initialize(buttonProperties)
        buttonNeutral.visibility = View.VISIBLE
        buttonNeutral.setOnClickListener {
            captureInit(jsonObj)
        }

        dialogBinding.btnEditText.setText(buttonProperties?.text)

        buttonProperties?.color?.let { colorPicker.setColor(it) }
        colorPicker.setColorSelectionListener(object : SimpleColorSelectionListener() {
            override fun onColorSelected(color: Int) {
                buttonProperties?.color = color
            }
        })

        val text = context.getString(
            R.string.length_of_captured_ir_signal,
            jsonObj.getString(Strings.btnPropLength).toLong(16).toString()
        )
        dialogBinding.textIrSize.text = text
        dialogBinding.textIrSize.setOnLongClickListener {
            Toast.makeText(
                context,
                "Protocol :" + jsonObj.getString(Strings.btnPropProtocol) + " " + jsonObj.getString(
                    Strings.btnPropIrcode
                ).replace(" ", "").replace("\n", ""),
                Toast.LENGTH_LONG
            ).show()
            true
        }

        dialogBinding.remoteModelButton.setOnClickListener {
            val popup = PopupMenu(context, dialogBinding.remoteModelButton)
            popup.menuInflater.inflate(R.menu.btn_style_menu, popup.menu)
            popup.setOnMenuItemClickListener {
                buttonProperties?.iconType = arrayOf(
                    R.id.round_button_small,
                    R.id.button_horizontal,
                    R.id.button_vertical,
                    R.id.round_button_large
                ).indexOf(it.itemId)
                true
            }
            popup.show()
        }

        buttonProperties?.textColor?.let { clrPkr.setColor(it) }


        dialogBinding.btnIcon.setOnClickListener {
            val iconAdapter = object : ArrayAdapter<Int>(
                context, R.layout.drawable_layout, ESPUtilsApp.iconDrawableList.toTypedArray()
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val drawable = ContextCompat.getDrawable(context, getItem(position)!!)
                    DrawableCompat.setTint(drawable!!, MainActivity.colorOnBackground)
                    var returnView = convertView
                    if (returnView == null) {
                        returnView = layoutInflater.inflate(R.layout.drawable_layout, parent, false)
                    }
                    val iv =
                        (returnView as LinearLayout).findViewById<ImageView>(R.id.btn_icon_grid)
                    val lparam = LinearLayout.LayoutParams(
                        MainActivity.layoutParams.width / (MainActivity.NUM_COLUMNS + 2),
                        MainActivity.layoutParams.width / (MainActivity.NUM_COLUMNS + 2)
                    )
                    iv.layoutParams = lparam
                    iv.setImageDrawable(drawable)
                    return returnView
                }
            }

            val iconDialog =
                Builder(context, R.style.AppTheme_AlertDialog).setView(R.layout.icon_grid_dialog)
                    .setTitle(context.getString(R.string.dialog_title_remote_icon)).create()
            iconDialog.setOnShowListener {
                val iconGrid = iconDialog.findViewById<GridView>(R.id.icon_grid)
                iconGrid?.post {
                    iconGrid.columnWidth =
                        MainActivity.layoutParams.width * 9 / (iconGrid.numColumns * 10)
                }
                iconGrid?.adapter = iconAdapter
                buttonProperties?.icon?.let { it1 -> iconGrid?.setSelection(it1) }
                iconGrid?.onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, position, _ ->
                        buttonProperties?.icon = position
                        iconDialog.dismiss()
                    }
            }
            iconDialog.show()
        }

        dialogBinding.btnEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                dialogBinding.remoteModelButton.text = s
                buttonProperties?.text = s.toString()
            }
        })

        buttonPositive.visibility = View.VISIBLE
        buttonPositive.setOnClickListener {
            listener.onSelected(JSONObject(jsonObj.toString()))
            dismiss()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        Toast.makeText(context, R.string.message_cancel_to_quit, Toast.LENGTH_SHORT).show()
    }

    companion object {
        var textInt = 0
        var MODE_SINGLE = 0
        var MODE_MULTI = 1
        var jsonObj =
            JSONObject("""{"iconType":${RemoteButton.TYPE_RECT_HOR},"color":${Color.GRAY},"icon":0,"textColor":${Color.WHITE}}""")
    }

    private operator fun IntArray.times(count: Int): IntArray {
        val ia = IntArray(size * count)
        for (i in 1..count) ia.plus(this)
        return ia
    }

    private operator fun <E> ArrayList<E>.times(size: Int): ArrayList<E> {
        val al = ArrayList<E>()
        for (i in 1..size) al.addAll(this)
        return al
    }
}