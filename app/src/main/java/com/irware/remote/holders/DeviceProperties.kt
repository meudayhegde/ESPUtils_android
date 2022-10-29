package com.irware.remote.holders

import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import com.irware.remote.ESPUtils
import com.irware.remote.listeners.OnDeviceStatusListener
import com.irware.remote.net.SocketClient
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class DeviceProperties(val deviceConfigFile: File = File(ESPUtils.FILES_DIR + File.separator + "Dummy.json"))  {
    var onDeviceStatusListener: OnDeviceStatusListener? = null
    private var jsonObj : JSONObject = getJSONObject()
    var isConnected = false
    set(value){
        field = value
        Handler(Looper.getMainLooper()).post{
            onDeviceStatusListener?.onStatusUpdate(value)
        }
    }

    var nickName: String = jsonObj.optString("nickName", "")
        get() { return jsonObj.optString("nickName", "")}
        set(value){ field = value; jsonObj.put("nickName", value); update() }
    var userName: String = jsonObj.optString("userName", "")
        get() { return jsonObj.optString("userName", "")}
        set(value){ field = value; jsonObj.put("userName", value); update() }
    var password: String = jsonObj.optString("password", "")
        get() { return jsonObj.optString("password", "")}
        set(value){ field = value; jsonObj.put("password", value); update() }
    var macAddress: String = jsonObj.optString("macAddr")
        get() { return jsonObj.optString("macAddr", "")}
        set(value){ field = value; jsonObj.put("macAddr", value); update() }
    val ipAddress: String
        get(){return ESPUtils.arpTable.getIpFromMac(macAddress)?: ""}
    var description: String = jsonObj.optString("description", "")
        get() { return jsonObj.optString("description", "")}
        set(value){ field = value; jsonObj.put("description", value); update() }
    var pinConfig = ArrayList<GPIOObject>()

    fun getIpAddress(listener: ((address: String?) -> Unit)){
        Handler(Looper.getMainLooper()).post{
            onDeviceStatusListener?.onBeginRefresh()
        }
        ESPUtils.arpTable.getIpFromMac(macAddress){
            isConnected = !(it == null || it.isEmpty())
            listener.invoke(it)
        }
    }

    private fun getJSONObject():JSONObject{
        if(!deviceConfigFile.exists()) return JSONObject()
        val isr = InputStreamReader(deviceConfigFile.inputStream())
        val content = TextUtils.join("\n",isr.readLines())
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
                connector.sendLine("{\"request\":\"gpio_get\",\"username\":\"$userName\", \"password\": \"$password\", \"pinNumber\": -1}")
                val response = connector.readLine()
                connector.close()
                val pinJson = JSONArray(response)

                for(j in 0 until pinJson.length()){
                    val gpioObj = pinJson.getJSONObject(j)
                    for(gpio in pinConfig){
                        if(gpio.gpioNumber == gpioObj.getInt("pinNumber")){
                            gpio.pinValue = gpioObj.getInt("pinValue")
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
            ESPUtils.gpioObjectList.remove(it)
        }
    }
}


