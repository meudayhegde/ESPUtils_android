package com.github.meudayhegde.esputils.net

import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import com.github.meudayhegde.ThreadHandler
import com.github.meudayhegde.esputils.ESPUtilsApp
import com.github.meudayhegde.esputils.MainActivity
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.listeners.IrCodeListener
import com.github.meudayhegde.esputils.ui.dialogs.ButtonPropertiesDialog
import org.json.JSONObject
import java.io.*
import java.net.Socket

object SocketClient {

    class Connector(address: String) {
        private val adr = address.split(":")
        private val soc = if (adr.size == 2) Socket(adr[0], adr[1].toInt()) else Socket(
            address, ESPUtilsApp.ESP_COM_PORT
        )
        private val br = BufferedReader(InputStreamReader(soc.getInputStream()))
        private val bw = BufferedWriter(OutputStreamWriter(soc.getOutputStream()))

        fun sendLine(content: String) {
            bw.write(content)
            bw.newLine()
            bw.flush()
        }

        fun readLine(): String {
            return br.readLine()
        }

        fun close() {
            soc.close()
        }

        fun isConnected(): Boolean {
            return !soc.isClosed
        }
    }

    fun readIrCode(
        address: String,
        userName: String,
        password: String,
        irlistener: IrCodeListener,
        jsonObj: JSONObject?
    ) {
        ThreadHandler.runOnFreeThread {
            var canceled = false
            try {
                val connector = Connector(address)
                Handler(Looper.getMainLooper()).post {
                    irlistener.parentDialog?.setButton(
                        DialogInterface.BUTTON_NEGATIVE, ESPUtilsApp.getString(R.string.cancel)
                    ) { dialog, _ ->
                        canceled = true
                        connector.close()
                        dialog?.dismiss()
                    }
                }

                connector.sendLine(
                    Strings.espCommandReadIrcode(
                        userName, password, irlistener.mode
                    )
                )
                while (connector.isConnected()) {
                    val result = JSONObject(connector.readLine())
                    when (result.getString(Strings.espResponse)) {
                        Strings.espResponseSuccess -> {
                            result.remove(Strings.espResponse)

                            if (jsonObj != null) {
                                jsonObj.put(
                                    Strings.btnPropLength, result.getString(Strings.btnPropLength)
                                )
                                jsonObj.put(
                                    Strings.btnPropIrcode, result.getString(Strings.btnPropIrcode)
                                )
                                irlistener.onIrRead(jsonObj)
                            } else {
                                if (irlistener.mode == ButtonPropertiesDialog.MODE_SINGLE) result.put(
                                    Strings.btnPropText, ""
                                )
                                else {
                                    result.put(Strings.btnPropText, ButtonPropertiesDialog.textInt)
                                    ButtonPropertiesDialog.textInt++
                                }
                                result.put(
                                    Strings.btnPropIconType,
                                    ButtonPropertiesDialog.jsonObj.getInt(Strings.btnPropIconType)
                                )
                                result.put(
                                    Strings.btnPropColor,
                                    ButtonPropertiesDialog.jsonObj.getInt(Strings.btnPropColor)
                                )
                                result.put(
                                    Strings.btnPropIcon,
                                    ButtonPropertiesDialog.jsonObj.getInt(Strings.btnPropIcon)
                                )
                                result.put(
                                    Strings.btnPropTextColor,
                                    ButtonPropertiesDialog.jsonObj.getInt(Strings.btnPropTextColor)
                                )
                                irlistener.onIrRead(result)
                            }
                        }
                        Strings.espResponseTimeout -> {
                            irlistener.onTimeout()
                            break
                        }
                        Strings.espResponseProgress -> {
                            Handler(Looper.getMainLooper()).post {
                                irlistener.onProgress(result.getInt(Strings.espResponseValue))
                            }
                        }
                        else -> irlistener.onDeny(MainActivity.activity?.getString(R.string.message_auth_failed_login_again))
                    }
                }
            } catch (ex: IOException) {
                if (!canceled) irlistener.onDeny(ex.toString())
            } catch (ex: IllegalStateException) {
                //irlistener.onTimeout()
            }
        }
    }

    fun sendIrCode(
        address: String,
        userName: String,
        password: String,
        jsonObj: JSONObject,
        irSendListener: ((result: String) -> Unit)
    ) {
        ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
            try {
                val connector = Connector(address)
                connector.sendLine(
                    Strings.espCommandSendIrcode(
                        userName,
                        password,
                        jsonObj.getString(Strings.btnPropLength),
                        jsonObj.getString(Strings.btnPropProtocol),
                        jsonObj.getString(Strings.btnPropIrcode)
                    )
                )
                val result = connector.readLine()
                connector.close()
                irSendListener.invoke(result)
            } catch (ex: Exception) {
                irSendListener.invoke(ex.toString())
            }
        }
    }
}