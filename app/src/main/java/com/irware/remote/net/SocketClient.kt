package com.irware.remote.net

import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import com.irware.ThreadHandler
import com.irware.remote.ESPUtilsApp
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.listeners.IrCodeListener
import com.irware.remote.ui.dialogs.ButtonPropertiesDialog
import org.json.JSONObject
import java.io.*
import java.net.Socket

object SocketClient{

    class Connector(address: String){
        private val adr = address.split(":")
        private val soc = if(adr.size == 2) Socket(adr[0], adr[1].toInt()) else Socket(address, ESPUtilsApp.ESP_COM_PORT)
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
                    irlistener.parentDialog?.setButton(DialogInterface.BUTTON_NEGATIVE, ESPUtilsApp.getString(R.string.cancel)) { dialog, _ ->
                        canceled = true
                        connector.close()
                        dialog?.dismiss()
                    }
                }

                connector.sendLine(ESPUtilsApp.getString(R.string.esp_command_read_ircode, userName, password, irlistener.mode))
                while(connector.isConnected()) {
                    val result = JSONObject(connector.readLine())
                    when (result.getString(ESPUtilsApp.getString(R.string.esp_response))) {
                        ESPUtilsApp.getString(R.string.esp_response_success) -> {
                            result.remove(ESPUtilsApp.getString(R.string.esp_response))

                            if (jsonObj != null) {
                                jsonObj.put(ESPUtilsApp.getString(R.string.button_prop_length), result.getString(ESPUtilsApp.getString(R.string.button_prop_length)))
                                jsonObj.put(ESPUtilsApp.getString(R.string.button_prop_ircode), result.getString(ESPUtilsApp.getString(R.string.button_prop_ircode)))
                                irlistener.onIrRead(jsonObj)
                            } else {
                                if(irlistener.mode == ButtonPropertiesDialog.MODE_SINGLE) result.put(ESPUtilsApp.getString(R.string.button_prop_text), "")
                                else{
                                    result.put(ESPUtilsApp.getString(R.string.button_prop_text), ButtonPropertiesDialog.textInt)
                                    ButtonPropertiesDialog.textInt++
                                }
                                result.put(ESPUtilsApp.getString(R.string.button_prop_icon_type), ButtonPropertiesDialog.jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_icon_type)))
                                result.put(ESPUtilsApp.getString(R.string.button_prop_color), ButtonPropertiesDialog.jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_color)))
                                result.put(ESPUtilsApp.getString(R.string.button_prop_icon), ButtonPropertiesDialog.jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_icon)))
                                result.put(ESPUtilsApp.getString(R.string.button_prop_text_color), ButtonPropertiesDialog.jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_text_color)))
                                irlistener.onIrRead(result)
                            }
                        }
                        ESPUtilsApp.getString(R.string.esp_response_timeout) -> {
                            irlistener.onTimeout()
                            break
                        }
                        ESPUtilsApp.getString(R.string.esp_response_progress) -> {
                            Handler(Looper.getMainLooper()).post  {
                                irlistener.onProgress(result.getInt(ESPUtilsApp.getString(R.string.esp_response_value)))
                            }
                        }
                        else -> irlistener.onDeny(MainActivity.activity?.getString(R.string.auth_failed_login_again))
                    }
                }
            }catch(ex: IOException){
                if(!canceled)
                    irlistener.onDeny(ex.toString())
            }catch(ex: IllegalStateException){
                 //irlistener.onTimeout()
            }
        }
    }

    fun sendIrCode(address: String, userName: String, password: String, jsonObj:JSONObject, irSendListener: ((result: String) -> Unit)) {
        ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
            try {
                val connector = Connector(address)
                connector.sendLine(
                    ESPUtilsApp.getString(
                        R.string.esp_command_send_ircode, userName, password,
                        jsonObj.getString("length"),
                        jsonObj.getString("protocol"),
                        jsonObj.getString("irCode")
                    )
                )
                val result = connector.readLine()
                connector.close()
                irSendListener.invoke(result)
            }catch(ex:Exception){
                irSendListener.invoke(ex.toString())
            }
        }
    }
}