package com.irware.remote.net

import android.content.DialogInterface
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.ui.dialogs.ButtonPropertiesDialog
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

    fun readIrCode(irlistener:IrCodeListener,jsonObj:JSONObject?) {
        Thread {
            var canceled = false
            try {
                val connector = Connector(MainActivity.MCU_IP)
                MainActivity.activity?.runOnUiThread {
                    irlistener.parentDialog?.setButton(DialogInterface.BUTTON_NEGATIVE,"Cancel") { dialog, _ ->
                        canceled = true
                        connector.close()
                        dialog?.dismiss()
                    }
                }

                connector.sendLine(
                    "{\"request\":\"ir_capture\",\"username\":\""
                            + MainActivity.USERNAME + "\",\"password\":\"" + MainActivity.PASSWORD + "\",\"length\":\"0\",\"data\":\"_\"}")
                val result = JSONObject(connector.readLine())

                connector.close()
                when (result.getString("response")) {
                    "success" -> {
                        result.remove("response")

                        if(jsonObj != null){
                            jsonObj.put("length",result.getString("length"))
                            jsonObj.put("irCode",result.getString("irCode"))
                            irlistener.onIrRead(jsonObj)
                        }else{
                            result.put("text","")
                            result.put("iconType",ButtonPropertiesDialog.btnStyle)
                            result.put("color",ButtonPropertiesDialog.colorSelected)
                            result.put("icon",ButtonPropertiesDialog.iconSelected)
                            result.put("textColor",ButtonPropertiesDialog.colorContentSelected)
                            irlistener.onIrRead(result)
                        }
                    }
                    "timeout" -> irlistener.onTimeout()
                    else -> irlistener.onDeny(MainActivity.activity?.getString(R.string.auth_failed_login_again))
                }
            }catch(ex:IOException){
                if(!canceled)
                    irlistener.onDeny(ex.toString())
            }
        }.start()
    }

    fun sendIrCode(jsonObj:JSONObject,irSendListener: IrSendListener) {
        Thread {
            try {
                val connector = Connector(MainActivity.MCU_IP)
                connector.sendLine(
                    "{\"request\":\"ir_send\",\"username\":\""
                            + MainActivity.USERNAME + "\",\"password\":\""
                            + MainActivity.PASSWORD + "\",\"length\":\""
                            + jsonObj.getString("length") + "\",\"protocol\":\"" + jsonObj.getString(
                        "protocol"
                    ) + "\",\"irCode\":\""
                            + jsonObj.getString("irCode") + "\"}"
                )
                val result = connector.readLine()
                connector.close()
                irSendListener.onIrSend(result)
            }catch(ex:Exception){
                irSendListener.onIrSend(ex.toString())
            }
        }.start()
    }

}

interface IrCodeListener{
    var parentDialog:androidx.appcompat.app.AlertDialog?
    fun onIrRead(jsonObj:JSONObject)
    fun onTimeout()
    fun onDeny(err_info:String?)

}

interface IrSendListener{
    fun onIrSend(result:String)
}