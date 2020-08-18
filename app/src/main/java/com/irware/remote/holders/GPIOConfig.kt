package com.irware.remote.holders

import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class GPIOConfig(private val gpioConfigFile: File)  {

    private var jsonObj : JSONObject = getJSONObject()

    var GPIOObjectArray: JSONArray? = jsonObj.optJSONArray("GPIOObjectArray")
        set(value){
            field = value
            jsonObj.put("GPIOObjectArray", value)
            update()
        }

    var description: String = jsonObj.optString("description")
        set(value){
            field = value
            jsonObj.put("description",value)
            update()
        }

    private fun getJSONObject():JSONObject{
        val isr = InputStreamReader(gpioConfigFile.inputStream())
        val content = TextUtils.join("\n",isr.readLines())
        isr.close()
        return try{
            JSONObject(content)
        }catch(ex:JSONException){
            JSONObject()
        }
    }

    fun update(){
        val osr = OutputStreamWriter(gpioConfigFile.outputStream())
        osr.write(jsonObj.toString().replace("\n",""))
        osr.flush()
        osr.close()
    }

    override fun toString(): String {
        return jsonObj.toString(0).replace("\n","")
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




