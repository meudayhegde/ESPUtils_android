package com.irware.remote.holders

import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import com.irware.remote.ESPUtilsApp
import com.irware.remote.R
import com.irware.remote.listeners.OnDeviceStatusListener
import com.irware.remote.net.SocketClient
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class DeviceProperties(val deviceConfigFile: File = File(ESPUtilsApp.FILES_DIR + File.separator + "Dummy.json"))  {
    var onDeviceStatusListener: OnDeviceStatusListener? = null
    private var jsonObj : JSONObject = getJSONObject()
    var isConnected = false
        set(value){
            field = value
            Handler(Looper.getMainLooper()).post{
                onDeviceStatusListener?.onStatusUpdate(value)
            }
        }

    var nickName: String = jsonObj.optString(ESPUtilsApp.getString(R.string.device_prop_nickname), "")
        get(){
            return jsonObj.optString(ESPUtilsApp.getString(R.string.device_prop_nickname), "")
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.device_prop_nickname), value)
            update()
        }

    var userName: String = jsonObj.optString(ESPUtilsApp.getString(R.string.device_prop_username), "")
        get(){
            return jsonObj.optString(ESPUtilsApp.getString(R.string.device_prop_username), "")
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.device_prop_username), value)
            update()
        }

    var password: String = jsonObj.optString(ESPUtilsApp.getString(R.string.device_prop_password), "")
        get(){
            return jsonObj.optString(ESPUtilsApp.getString(R.string.device_prop_password), "")
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.device_prop_password), value)
            update()
        }

    var macAddress: String = jsonObj.optString(ESPUtilsApp.getString(R.string.device_prop_mac_address))
        get(){
            return jsonObj.optString(ESPUtilsApp.getString(R.string.device_prop_mac_address), "")
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.device_prop_mac_address), value)
            update()
        }

    val ipAddress: String
        get(){
            return ESPUtilsApp.arpTable.getIpFromMac(macAddress)?: ""
        }

    var description: String = jsonObj.optString(ESPUtilsApp.getString(R.string.device_prop_description), "")
        get(){
            return jsonObj.optString(ESPUtilsApp.getString(R.string.device_prop_description), "")
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.device_prop_description), value)
            update()
        }

    var pinConfig = ArrayList<GPIOObject>()

    fun getIpAddress(listener: ((address: String?) -> Unit)){
        Handler(Looper.getMainLooper()).post{
            onDeviceStatusListener?.onBeginRefresh()
        }
        ESPUtilsApp.arpTable.getIpFromMac(macAddress){
            isConnected = !(it == null || it.isEmpty())
            listener.invoke(it)
        }
    }

    private fun getJSONObject():JSONObject{
        if(!deviceConfigFile.exists()) return JSONObject()
        val isr = InputStreamReader(deviceConfigFile.inputStream())
        val content = TextUtils.join("\n", isr.readLines())
        isr.close()
        return try{
            JSONObject(content)
        }catch(ex:JSONException){
            JSONObject()
        }
    }

    fun update(){
        val osr = OutputStreamWriter(deviceConfigFile.outputStream())
        osr.write(jsonObj.toString(4))
        osr.flush()
        osr.close()
    }

    override fun toString(): String {
        return nickName
    }


    fun refreshGPIOStatus(){
        Handler(Looper.getMainLooper()).post{
            pinConfig.forEach {
                it.onGPIORefreshListener?.onRefreshBegin()
            }
        }
        getIpAddress{ address ->
            if (isConnected){
                val connector = SocketClient.Connector(address!!)
                connector.sendLine(ESPUtilsApp.getString(R.string.esp_command_get_gpio, userName, password))
                val response = connector.readLine()
                connector.close()
                val pinJson = JSONArray(response)

                for(j in 0 until pinJson.length()){
                    val gpioObj = pinJson.getJSONObject(j)
                    for(gpio in pinConfig){
                        if(gpio.gpioNumber == gpioObj.getInt(ESPUtilsApp.getString(R.string.esp_response_pin_number))){
                            gpio.pinValue = gpioObj.getInt(ESPUtilsApp.getString(R.string.esp_response_pin_value))
                            Handler(Looper.getMainLooper()).post{
                                gpio.onGPIORefreshListener?.onRefresh(gpio.pinValue)
                            }
                        }
                    }
                }
            }else{
                Handler(Looper.getMainLooper()).post{
                    pinConfig.forEach { it.onGPIORefreshListener?.onRefresh(-1) }
                }
            }
        }
    }

    fun delete(){
        deviceConfigFile.delete()
        pinConfig.forEach {
            it.delete()
            ESPUtilsApp.gpioObjectList.remove(it)
        }
    }
}


