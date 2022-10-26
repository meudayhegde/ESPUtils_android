package com.irware.remote.holders

import android.text.TextUtils
import com.irware.remote.ESPUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class RemoteProperties(val remoteConfigFile: File, private val eventListener: EventListener?)  {

    private var jsonObj : JSONObject = getJSONObject()

    var fileName: String = jsonObj.optString("fileName")
        set(value){
            field = value
            jsonObj.put("fileName",value)
            update()
        }

    var remoteVendor: String = jsonObj.optString("vendor")
        set(value){
            field = value
            jsonObj.put("vendor",value)
            update()
        }
    var remoteName: String = jsonObj.optString("name")
        set(value){
            field = value
            jsonObj.put("name",value)
            update()
        }

    var remoteID: String = jsonObj.optString("id")
        set(value){
            field = value
            jsonObj.put("id",value)
            update()
        }

    var description: String = jsonObj.optString("description")
        set(value){
            field = value
            jsonObj.put("description",value)
            update()
        }

    private var buttonArray: JSONArray = getButtons()
        set(value){
            field = value
            jsonObj.put("buttons",value)
            update()
        }


    var deviceConfigFileName: String = jsonObj.optString("deviceConfigFileName", "")
        set(value){
            field = value
            jsonObj.put("deviceConfigFileName", value)
            update()
            deviceProperties = ESPUtils.devicePropList.find { it.deviceConfigFile.name == deviceConfigFileName } ?:
                    DeviceProperties(File(remoteConfigFile.parent!! + File.separator + ESPUtils.DEVICE_CONFIG_DIR, deviceConfigFileName))
        }

    var deviceProperties = ESPUtils.devicePropList.find { it.deviceConfigFile.name == deviceConfigFileName } ?:
    DeviceProperties(File(remoteConfigFile.parent!! + File.separator + ESPUtils.DEVICE_CONFIG_DIR, deviceConfigFileName))

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
            jsonObj.getJSONArray("buttons")
        }catch(ex:JSONException){
            jsonObj.put("buttons",JSONArray())
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
            if(getJSONObject(position).optLong("buttonID") == obj.getLong("buttonID"))
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




