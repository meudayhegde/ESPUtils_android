package com.irware.remote.ui.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.LinearLayout
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.net.IrCodeListener

class ButtonPropertiesDilog(context:Context): AlertDialog(context),IrCodeListener {

    init{
        setView(layoutInflater.inflate(R.layout.create_button_dialog_layout,null))
        setButton(DialogInterface.BUTTON_NEGATIVE,"cancel") { dialog, which -> dialog!!.dismiss() }
        show()
        findViewById<LinearLayout>(R.id.ir_capture_layout)!!.visibility =View.VISIBLE
        findViewById<LinearLayout>(R.id.button_prop_table)!!.visibility=View.GONE
        MainActivity.socketClient.readIrCode(this)
    }

    override fun onIrRead(code: String){
        findViewById<LinearLayout>(R.id.ir_capture_layout)!!.visibility= View.GONE
        findViewById<LinearLayout>(R.id.button_prop_table)!!.visibility=View.VISIBLE
    }
}