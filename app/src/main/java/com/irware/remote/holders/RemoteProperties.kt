package com.irware.remote.holders

import android.text.TextUtils
import com.irware.remote.Strings
import com.irware.remote.ESPUtilsApp
import com.irware.remote.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class RemoteProperties(val remoteConfigFile: File, private val eventListener: EventListener? = null)  {

    private var jsonObj : JSONObject = getJSONObject()

    var fileName: String = jsonObj.optString(Strings.remotePropFileName)
        set(value){
            field = value
            jsonObj.put(Strings.remotePropFileName, value)
            update()
        }

    var remoteVendor: String = jsonObj.optString(Strings.remotePropVendor)
        set(value){
            field = value
            jsonObj.put(Strings.remotePropVendor, value)
            update()
        }
    var remoteName: String = jsonObj.optString(Strings.remotePropName)
        set(value){
            field = value
            jsonObj.put(Strings.remotePropName, value)
            update()
        }

    var remoteID: String = jsonObj.optString(Strings.remotePropID)
        set(value){
            field = value
            jsonObj.put(Strings.remotePropID, value)
            update()
        }

    var description: String = jsonObj.optString(Strings.remotePropDescription)
        set(value){
            field = value
            jsonObj.put(Strings.remotePropDescription, value)
            update()
        }

    private var buttonArray: JSONArray = getButtons()
        set(value){
            field = value
            jsonObj.put(Strings.remotePropButtonsArray, value)
            update()
        }


    var deviceConfigFileName: String = jsonObj.optString(Strings.remotePropDevPropFileName, "")
        set(value){
            field = value
            jsonObj.put(Strings.remotePropDevPropFileName, value)
            update()
            deviceProperties = ESPUtilsApp.devicePropList.find { it.deviceConfigFile.name == deviceConfigFileName } ?:
                    DeviceProperties(ESPUtilsApp.getPrivateFile(Strings.nameDirDeviceConfig, deviceConfigFileName))
        }

    var deviceProperties = ESPUtilsApp.devicePropList.find { it.deviceConfigFile.name == deviceConfigFileName } ?:
    DeviceProperties(ESPUtilsApp.getPrivateFile(Strings.nameDirDeviceConfig, deviceConfigFileName))

    fun addButton(button:JSONObject):JSONObject?{
        val index = buttonArray.index(button)
        if( index>=0){
            return buttonArray.getJSONObject(index)
        }
        buttonArray.put(button)
        return button
    }

    fun removeButton(button:JSONObject):Boolean{
        val index = buttonArray.index(button)
        if(index<0) return false
        buttonArray.remove(index)
        update()
        return true
    }

    fun getButtons():JSONArray{
        return try{
            jsonObj.getJSONArray(Strings.remotePropButtonsArray)
        }catch(ex:JSONException){
            jsonObj.put(Strings.remotePropButtonsArray, JSONArray())
            getButtons()
        }
    }

    private fun getJSONObject():JSONObject{
        val isr = InputStreamReader(remoteConfigFile.inputStream())
        val content = TextUtils.join("\n",isr.readLines())
        isr.close()
        return try{
            JSONObject(content)
        }catch(ex:JSONException){
            if(content.isNotEmpty()) eventListener?.jsonLoadError(ex)
            JSONObject()
        }
    }

    fun update(){
        val osr = OutputStreamWriter(remoteConfigFile.outputStream())
        osr.write(jsonObj.toString(4))
        osr.flush()
        osr.close()
    }


    override fun toString(): String {
        return jsonObj.toString(0).replace("\n","")
    }

    companion object{
        interface EventListener{
            fun jsonLoadError(ex:Exception)
        }
    }

    private fun JSONArray.index(obj: JSONObject):Int{
        for(position in 0 until this.length()){
            if(getJSONObject(position).optLong(Strings.btnPropBtnId) == obj.getLong(Strings.btnPropBtnId))
                return position
        }
        return -1
    }

    private operator fun JSONArray.contains(obj: JSONObject): Boolean {
        if(index(obj) == -1)
            return false
        return true
    }
}