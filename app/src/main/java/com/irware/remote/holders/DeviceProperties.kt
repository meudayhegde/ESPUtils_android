package com.irware.remote.holders

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.irware.remote.MainActivity
import com.irware.remote.net.ARPTable
import com.irware.remote.net.OnIpListener
import com.irware.remote.net.SocketClient
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class DeviceProperties(val deviceConfigFile: File)  {

    private var jsonObj : JSONObject = getJSONObject()
    private var onStatusUpdateListeners = ArrayList<OnStatusUpdateListener>()
    private var isConnected = false

    var nickName: String = jsonObj.optString("nickName", "")
        get() { return jsonObj.optString("nickName", "")}
        set(value){ field = value; jsonObj.put("nickName", value); update() }
    var userName: String = jsonObj.optString("userName", "")
        get() { return jsonObj.optString("userName", "")}
        set(value){ field = value; jsonObj.put("userName", value); update() }
    var password: String = jsonObj.optString("password", "")
        get() { return jsonObj.optString("password", "")}
        set(value){ field = value; jsonObj.put("password", value); update() }
    var macAddr: String = jsonObj.optString("macAddr")
        get() { return jsonObj.optString("macAddr", "")}
        set(value){ field = value; jsonObj.put("macAddr", value); update() }
//    var ipAddr: JSONArray = jsonObj.optJSONArray("ipAddr") ?: JSONArray()
//        get() { return jsonObj.optJSONArray("ipAddr") ?: JSONArray()}
//        set(value){ field = value; jsonObj.put("ipAddr", value); update() }
    var description: String = jsonObj.optString("description", "")
        get() { return jsonObj.optString("description", "")}
        set(value){ field = value; jsonObj.put("description", value); update() }
    var pinConfig = ArrayList<GPIOObject>()

    private fun getJSONObject():JSONObject{
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

    fun updateStatus(context: Context){
        val arpTable = MainActivity.arpTable ?: ARPTable(context, 1)
        arpTable.getIpFromMac(macAddr, object: OnIpListener {
            override val uiThread: Boolean = false

            override fun onIpResult(address: String?) {
                isConnected = ( address != null )
                try{
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
                                }
                            }
                        }
                    }
                }catch(ex: Exception){
                    Log.d("PinInfo", "Error: Failed to get pin info, $ex")
                }

                (context as Activity).runOnUiThread {
                    onStatusUpdateListeners.forEach {
                        it.onStatusUpdate(isConnected)
                    }
                }
            }
        })
    }

    fun addOnStatusUpdateListener(onStatusUpdateListener: OnStatusUpdateListener){
        for (listener in onStatusUpdateListeners){
            if (listener.listenerParent == onStatusUpdateListener.listenerParent) return
        }
        onStatusUpdateListeners.add(onStatusUpdateListener)
    }
    fun clearOnStatusUpdateListener(){
        onStatusUpdateListeners.clear()
    }
}

interface OnStatusUpdateListener{
    var listenerParent : Any?
    fun onStatusUpdate(connected: Boolean)
}





