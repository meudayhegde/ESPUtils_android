package com.github.meudayhegde.esputils.holders

import android.text.TextUtils
import com.github.meudayhegde.esputils.Strings
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class GPIOConfig(private val gpioConfigFile: File) {

    private var jsonObj: JSONObject = getJSONObject()

    var gpioObjectArray: JSONArray = getGPIO()
        get() = getGPIO()
        set(value) {
            field = value
            jsonObj.put(Strings.gpioConfigObjArray, value)
            update()
        }

    var description: String = jsonObj.optString(Strings.gpioConfigDescription)
        set(value) {
            field = value
            jsonObj.put(Strings.gpioConfigDescription, value)
            update()
        }

    private fun getJSONObject(): JSONObject {
        val isr = InputStreamReader(gpioConfigFile.inputStream())
        val content = TextUtils.join("\n", isr.readLines())
        isr.close()
        return try {
            JSONObject(content)
        } catch (ex: JSONException) {
            JSONObject()
        }
    }

    fun addGPIO(gpio: JSONObject): JSONObject? {
        val index = gpioObjectArray.index(gpio)
        if (index >= 0) {
            return gpioObjectArray.getJSONObject(index)
        }
        gpioObjectArray.put(gpio)
        return gpio
    }

    fun removeGPIO(gpio: JSONObject): Boolean {
        val index = gpioObjectArray.index(gpio)
        if (index < 0) return false
        gpioObjectArray.remove(index)
        update()
        return true
    }

    private fun getGPIO(): JSONArray {
        return try {
            update()
            jsonObj.getJSONArray(Strings.gpioConfigObjArray)
        } catch (ex: JSONException) {
            jsonObj.put(Strings.gpioConfigObjArray, JSONArray())
            getGPIO()
        }
    }

    fun update() {
        val osr = OutputStreamWriter(gpioConfigFile.outputStream())
        osr.write(jsonObj.toString().replace("\n", ""))
        osr.flush()
        osr.close()
    }

    override fun toString(): String {
        return jsonObj.toString(0).replace("\n", "")
    }

    private fun JSONArray.index(obj: JSONObject): Int {
        for (position in 0 until this.length()) {
            val curObj = getJSONObject(position)
            if (curObj.optString(
                    Strings.gpioObjMACAddress, ""
                ) == obj.optString(
                    Strings.gpioObjMACAddress, ""
                ) && curObj.optInt(Strings.gpioObjPinNumber) == obj.optInt(Strings.gpioObjPinNumber)
            ) return position
        }
        return -1
    }

    private operator fun JSONArray.contains(obj: JSONObject): Boolean {
        if (index(obj) == -1) return false
        return true
    }
}



