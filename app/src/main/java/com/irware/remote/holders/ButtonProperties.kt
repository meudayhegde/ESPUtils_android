package com.irware.remote.holders

import org.json.JSONObject

class ButtonProperties(val jsonObj:JSONObject,private val parent:RemoteProperties) {

    private var attached = parent.addButton(jsonObj)
    var length= jsonObj.getString("length")
        set(value){ field = value;jsonObj.put("length",value);if(attached)parent.update()}
    var irCode:String = jsonObj.getString("irCode")
        set(value){field = value;jsonObj.put("irCode",value);listener?.onIrModified();if(attached)parent.update()}
    var text: String = jsonObj.getString("text")
        set(value){field = value;jsonObj.put("text",value);listener?.onTextModified();if(attached)parent.update()}
    var iconType = jsonObj.getInt("iconType")
        set(value){field = value;jsonObj.put("iconType",value);listener?.onIconModified();if(attached)parent.update()}
    var color = jsonObj.getInt("color")
        set(value){field = value;jsonObj.put("color",value);listener?.onColorModified();if(attached)parent.update()}
    var icon = jsonObj.getInt("icon")
        set(value){field = value;jsonObj.put("icon",value);listener?.onIconModified();if(attached)parent.update()}
    var textColor = jsonObj.getInt("textColor")
        set(value){field = value;jsonObj.put("textColor",value);listener?.onTextColorChanged();if(attached)parent.update()}
    var btnPosition  = jsonObj.getInt("btnPosition")
        set(value){field = value;jsonObj.put("btnPosition",value);listener?.onPositionModified();if(attached)parent.update()}
    
    private var listener:OnModificationListener?= null

    init{
        if(!attached){
            parent.addButton(jsonObj)
        }
        parent.update()
    }

    fun setOnModificationListener(listener: OnModificationListener){
        this.listener=listener
    }
}

interface OnModificationListener{
    fun onIconModified()
    fun onTypeModified()
    fun onTextModified()
    fun onColorModified()
    fun onIrModified()
    fun onPositionModified()
    fun onTextColorChanged()
}