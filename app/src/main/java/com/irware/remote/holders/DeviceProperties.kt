package com.irware.remote.holders

import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class DeviceProperties(private val deviceConfigFile: File)  {

    private var jsonObj : JSONObject = getJSONObject()

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
    var ipAddr: JSONArray? = jsonObj.optJSONArray("ipAddr")
        get() { return jsonObj.optJSONArray("ipAddr")}
        set(value){ field = value; jsonObj.put("ipAddr", value); update() }
    var description: String = jsonObj.optString("description", "")
        get() { return jsonObj.optString("description", "")}
        set(value){ field = value; jsonObj.put("description", value); update() }

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
}




