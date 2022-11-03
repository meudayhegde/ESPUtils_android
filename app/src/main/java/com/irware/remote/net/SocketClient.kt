package com.irware.remote.net

import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import com.irware.ThreadHandler
import com.irware.remote.ESPUtils
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.listeners.IrCodeListener
import com.irware.remote.ui.dialogs.ButtonPropertiesDialog
import org.json.JSONObject
import java.io.*
import java.net.Socket

object SocketClient{

    class Connector(address:String){
        private val adr = address.split(":")
        private val soc = if(adr.size == 2) Socket(adr[0], adr[1].toInt()) else Socket(address, ESPUtils.ESP_COM_PORT)
        private val br = BufferedReader(InputStreamReader(soc.getInputStream()))
        private val bw = BufferedWriter(OutputStreamWriter(soc.getOutputStream()))

        fun sendLine(content:String){
            bw.write(content)
            bw.newLine()
            bw.flush()
        }

        fun readLine(): String{
            return br.readLine()
        }

        fun close(){
            soc.close()
        }

        fun isConnected():Boolean{
            return !soc.isClosed
        }
    }

    fun readIrCode(address: String, userName: String, password: String, irlistener: IrCodeListener, jsonObj:JSONObject?) {
        ThreadHandler.runOnFreeThread {
            var canceled = false
            try {
                val connector = Connector(address)
                Handler(Looper.getMainLooper()).post {
                    irlistener.parentDialog?.setButton(DialogInterface.BUTTON_NEGATIVE,"Cancel") { dialog, _ ->
                        canceled = true
                        connector.close()
                        dialog?.dismiss()
                    }
                }

                connector.sendLine(
                    "{\"request\":\"ir_capture\",\"username\":\"${userName}\",\"password\":\"${password}\",\"capture_mode\":${irlistener.mode}}")
                while(connector.isConnected()) {
                    val result = JSONObject(connector.readLine())
                    when (result.getString("response")) {
                        "success" -> {
                            result.remove("response")

                            if (jsonObj != null) {
                                jsonObj.put("length", result.getString("length"))
                                jsonObj.put("irCode", result.getString("irCode"))
                                irlistener.onIrRead(jsonObj)
                            } else {
                                if(irlistener.mode == ButtonPropertiesDialog.MODE_SINGLE) result.put("text", "")
                                else{
                                    result.put("text",ButtonPropertiesDialog.textInt)
                                    ButtonPropertiesDialog.textInt++
                                }
                                result.put("iconType", ButtonPropertiesDialog.jsonObj.getInt("iconType"))
                                result.put("color", ButtonPropertiesDialog.jsonObj.getInt("color"))
                                result.put("icon", ButtonPropertiesDialog.jsonObj.getInt("icon"))
                                result.put("textColor", ButtonPropertiesDialog.jsonObj.getInt("textColor"))
                                irlistener.onIrRead(result)
                            }
                        }
                        "timeout" -> {
                            irlistener.onTimeout()
                            break
                        }
                        "progress" -> {
                            Handler(Looper.getMainLooper()).post  {
                                irlistener.onProgress(result.getInt("value"))
                            }
                        }
                        else -> irlistener.onDeny(MainActivity.activity?.getString(R.string.auth_failed_login_again))
                    }
                }
            }catch(ex:IOException){
                if(!canceled)
                    irlistener.onDeny(ex.toString())
            }catch(ex:IllegalStateException){
                 //irlistener.onTimeout()
            }
        }
    }

    fun sendIrCode(address: String, userName: String, password: String, jsonObj:JSONObject, irSendListener: ((result: String) -> Unit)) {
        ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
            try {
                val connector = Connector(address)
                connector.sendLine(
                    "{\"request\":\"ir_send\",\"username\":\"${userName}\",\"password\":\"${password}\",\"length\":\""
                            + jsonObj.getString("length") + "\",\"protocol\":\"" + jsonObj.getString("protocol") + "\",\"irCode\":\""
                            + jsonObj.getString("irCode") + "\"}")
                val result = connector.readLine()
                connector.close()
                irSendListener.invoke(result)
            }catch(ex:Exception){
                irSendListener.invoke(ex.toString())
            }
        }
    }

}