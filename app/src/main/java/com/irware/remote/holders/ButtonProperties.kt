package com.irware.remote.holders

import com.irware.remote.ESPUtilsApp
import com.irware.remote.R
import com.irware.remote.listeners.OnRemoteButtonModificationListener
import org.json.JSONObject

class ButtonProperties(var jsonObj:JSONObject) {

    var parent:RemoteProperties? = null
    constructor(json:JSONObject, parent:RemoteProperties) : this(json) {
        this.parent = parent
        val curProp = jsonObj
        buttonId
        jsonObj = parent.addButton(jsonObj)?: jsonObj
        curProp.keys().forEach {
            jsonObj.put(it, curProp.get(it))
        }
        parent.update()
    }

    var length: String = jsonObj.getString(ESPUtilsApp.getString(R.string.button_prop_length))
        get(){
            return jsonObj.getString(ESPUtilsApp.getString(R.string.button_prop_length))
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.button_prop_length), value)
            parent?.update()
        }

    var irCode: String = jsonObj.getString(ESPUtilsApp.getString(R.string.button_prop_ircode))
        get(){
            return jsonObj.getString(ESPUtilsApp.getString(R.string.button_prop_ircode))
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.button_prop_ircode), value)
            listener?.onIrModified()
            parent?.update()
        }

    var text: String = jsonObj.getString(ESPUtilsApp.getString(R.string.button_prop_text))
        get(){
            return jsonObj.getString(ESPUtilsApp.getString(R.string.button_prop_text))
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.button_prop_text), value)
            listener?.onTextModified()
            parent?.update()
        }

    var iconType = jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_icon_type))
        get(){
            return jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_icon_type))
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.button_prop_icon_type), value)
            listener?.onTypeModified()
            parent?.update()
        }

    var color = jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_color))
        get(){
            return  jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_color))
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.button_prop_color), value)
            listener?.onColorModified()
            parent?.update()
        }

    var icon = jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_icon))
        get(){
            return jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_icon))
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.button_prop_icon), value)
            listener?.onIconModified()
            parent?.update()
        }

    var textColor = jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_text_color))
        get(){
            return jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_text_color))
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.button_prop_text_color), value)
            listener?.onTextColorChanged()
            parent?.update()
        }

    var btnPosition  = jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_btn_position))
        get(){
            return jsonObj.getInt(ESPUtilsApp.getString(R.string.button_prop_btn_position))
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.button_prop_btn_position), value)
            listener?.onPositionModified()
            parent?.update()
        }

    var buttonId: Long = jsonObj.optLong(ESPUtilsApp.getString(R.string.button_prop_btn_id))
        get(){
            if(jsonObj.optLong(ESPUtilsApp.getString(R.string.button_prop_btn_id)) == 0L){
                jsonObj.put(ESPUtilsApp.getString(R.string.button_prop_btn_id), System.currentTimeMillis())
                parent?.update()
            }
            return jsonObj.getLong(ESPUtilsApp.getString(R.string.button_prop_btn_id))
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.button_prop_btn_id), value)
            parent?.update()
        }

    var buttonShowAnimation = true

    private var listener: OnRemoteButtonModificationListener?= null

    fun setOnModificationListener(listener: OnRemoteButtonModificationListener){
        this.listener=listener
    }
}