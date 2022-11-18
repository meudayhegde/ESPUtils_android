package com.irware.remote.holders

import android.text.TextUtils
import com.irware.remote.ESPUtilsApp
import com.irware.remote.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class RemoteProperties(val remoteConfigFile: File, private val eventListener: EventListener?)  {

    private var jsonObj : JSONObject = getJSONObject()

    var fileName: String = jsonObj.optString(ESPUtilsApp.getString(R.string.remote_prop_filename))
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.remote_prop_filename), value)
            update()
        }

    var remoteVendor: String = jsonObj.optString(ESPUtilsApp.getString(R.string.remote_prop_vendor))
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.remote_prop_vendor), value)
            update()
        }
    var remoteName: String = jsonObj.optString(ESPUtilsApp.getString(R.string.remote_prop_name))
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.remote_prop_name), value)
            update()
        }

    var remoteID: String = jsonObj.optString(ESPUtilsApp.getString(R.string.remote_prop_id))
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.remote_prop_id), value)
            update()
        }

    var description: String = jsonObj.optString(ESPUtilsApp.getString(R.string.remote_prop_description))
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.remote_prop_description), value)
            update()
        }

    private var buttonArray: JSONArray = getButtons()
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.remote_prop_buttons_array), value)
            update()
        }


    var deviceConfigFileName: String = jsonObj.optString(ESPUtilsApp.getString(R.string.remote_prop_dev_prop_file_name), "")
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.remote_prop_dev_prop_file_name), value)
            update()
            deviceProperties = ESPUtilsApp.devicePropList.find { it.deviceConfigFile.name == deviceConfigFileName } ?:
                    DeviceProperties(File(remoteConfigFile.parent!! + File.separator + ESPUtilsApp.DEVICE_CONFIG_DIR, deviceConfigFileName))
        }

    var deviceProperties = ESPUtilsApp.devicePropList.find { it.deviceConfigFile.name == deviceConfigFileName } ?:
    DeviceProperties(File(remoteConfigFile.parent!! + File.separator + ESPUtilsApp.DEVICE_CONFIG_DIR, deviceConfigFileName))

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
            jsonObj.getJSONArray(ESPUtilsApp.getString(R.string.remote_prop_buttons_array))
        }catch(ex:JSONException){
            jsonObj.put(ESPUtilsApp.getString(R.string.remote_prop_buttons_array), JSONArray())
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
            if(getJSONObject(position).optLong(ESPUtilsApp.getString(R.string.button_prop_btn_id)) == obj.getLong(ESPUtilsApp.getString(R.string.button_prop_btn_id)))
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