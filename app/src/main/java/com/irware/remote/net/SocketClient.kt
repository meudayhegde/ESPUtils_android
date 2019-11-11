package com.irware.remote.net

import android.content.DialogInterface
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.ui.dialogs.ButtonPropertiesDialog
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.Socket

object SocketClient{

    class Connector(ip:String){
        private val soc = Socket(ip,MainActivity.PORT)
        private val br = BufferedReader(InputStreamReader(soc.getInputStream()))
        private val bw = BufferedWriter(OutputStreamWriter(soc.getOutputStream()))

        fun sendLine(content:String){
            bw.write(content)
            bw.newLine()
            bw.flush()
        }

        fun readLine():String{
            return br.readLine()
        }

        fun close(){
            soc.close()
        }
    }

    fun readIrCode(irlistener:IrCodeListener) {
        Thread {
            try {
                val connector = Connector(MainActivity.MCU_IP)
                MainActivity.activity?.runOnUiThread {
                    irlistener.parentDialog?.setButton(DialogInterface.BUTTON_NEGATIVE,"cancel",object:DialogInterface.OnClickListener{
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            connector.close()
                            dialog?.dismiss()
                        }
                    })
                }
                connector.sendLine(
                    "{\"request\":\"ir_capture\",\"username\":\""
                            + MainActivity.USERNAME + "\",\"password\":\"" + MainActivity.PASSWORD + "\"}")
                val result = JSONObject(connector.readLine())

                connector.close()
                when (result.getString("response")) {
                    "rawData" -> {
                        result.remove("response")
                        result.put("text","")
                        result.put("iconType",ButtonPropertiesDialog.btnStyle)
                        result.put("color",ButtonPropertiesDialog.colorSelected)
                        result.put("icon",ButtonPropertiesDialog.iconSelected)
                        result.put("textColor",ButtonPropertiesDialog.colorContentSelected)
                        irlistener.onIrRead(result)
                    }
                    "timeout" -> irlistener.onTimeout()
                    else -> irlistener.onDeny(MainActivity.activity?.getString(R.string.auth_failed_login_again))
                }
            }catch(ex:IOException){
                irlistener.onDeny(ex.toString())
            }
        }.start()
    }

    fun sendIrCode(length:Int,array:JSONArray) {
        Thread {
            val connector = Connector(MainActivity.MCU_IP)
            connector.sendLine(
                "{\"request\":\"ir_send\",\"username\":\""
                        + MainActivity.USERNAME + "\",\"password\":\"" + MainActivity.PASSWORD + "\",\"length\":"+length+",\"data\":"+array.join(",")+"}")
            val result = JSONObject(connector.readLine())

        }.start()
    }

}

private fun IntArray.getString(): String? {
    var str="["
    for(i in 0..size-2){
        str+=this[i].toString()+","
    }
    str+=this[size-1].toString()+"]"
    return str
}

interface IrCodeListener{
    var parentDialog:androidx.appcompat.app.AlertDialog?
    fun onIrRead(jsonObj:JSONObject)
    fun onTimeout()
    fun onDeny(err_info:String?)
}