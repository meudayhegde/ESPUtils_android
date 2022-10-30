package com.irware.remote.holders

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

    var length: String = jsonObj.getString("length")
        get() { return jsonObj.getString("length")}
        set(value){ field = value; jsonObj.put("length", value); parent?.update()}
    var irCode: String = jsonObj.getString("irCode")
        get() { return jsonObj.getString("irCode")}
        set(value){ field = value; jsonObj.put("irCode", value); listener?.onIrModified(); parent?.update()}
    var text: String = jsonObj.getString("text")
        get() { return jsonObj.getString("text")}
        set(value){ field = value; jsonObj.put("text", value); listener?.onTextModified(); parent?.update()}
    var iconType = jsonObj.getInt("iconType")
        get(){ return jsonObj.getInt("iconType")}
        set(value){ field = value; jsonObj.put("iconType", value); listener?.onTypeModified(); parent?.update()}
    var color = jsonObj.getInt("color")
        get(){ return  jsonObj.getInt("color")}
        set(value){ field = value; jsonObj.put("color", value); listener?.onColorModified(); parent?.update()}
    var icon = jsonObj.getInt("icon")
        get() { return jsonObj.getInt("icon")}
        set(value){ field = value; jsonObj.put("icon", value); listener?.onIconModified(); parent?.update()}
    var textColor = jsonObj.getInt("textColor")
        get() { return jsonObj.getInt("textColor") }
        set(value){ field = value; jsonObj.put("textColor", value); listener?.onTextColorChanged(); parent?.update()}
    var btnPosition  = jsonObj.getInt("btnPosition")
        get(){ return jsonObj.getInt("btnPosition")}
        set(value){field = value; jsonObj.put("btnPosition", value); listener?.onPositionModified(); parent?.update()}
    var buttonId: Long = jsonObj.optLong("buttonID")
        get() { if(jsonObj.optLong("buttonID") == 0L) { jsonObj.put("buttonID", System.currentTimeMillis()); parent?.update()}; return jsonObj.getLong("buttonID")}
        set(value){ field = value; jsonObj.put("buttonID", value); parent?.update()}

    var buttonShowAnimation = true

    private var listener: OnRemoteButtonModificationListener?= null

    fun setOnModificationListener(listener: OnRemoteButtonModificationListener){
        this.listener=listener
    }
}