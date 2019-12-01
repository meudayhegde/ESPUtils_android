package com.irware.remote.holders

import android.text.TextUtils
import android.widget.Toast
import com.irware.remote.MainActivity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class RemoteProperties(private val remoteConfig: File, private val eventListener: EventListener?)  {

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
            Toast.makeText(MainActivity.activity!!,value,Toast.LENGTH_LONG).show()
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

    fun addButton(button:JSONObject):Boolean{
        if( button !in buttonArray ){
            buttonArray.put(button)
            update()
            return true
        }
        return false
    }

    fun removeButton(button:JSONObject):Boolean{
        if( button !in buttonArray)
            return false
        buttonArray.remove(buttonArray.index(button))
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
        val isr = InputStreamReader(remoteConfig.inputStream())
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
        val osr =OutputStreamWriter(remoteConfig.outputStream())
        osr.write(jsonObj.toString().replace("\n",""))
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
}

private fun JSONArray.index(any: Any):Int{
    for(item in 0 until this.length()){
        if(get(item) == any)
            return item
    }
    return -1
}

private operator fun JSONArray.contains(any: Any): Boolean {
    if(index(any) == -1)
        return false
    return true
}




