package com.github.meudayhegde.esputils.holders

import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.listeners.OnRemoteButtonModificationListener
import org.json.JSONObject

class ButtonProperties(var jsonObj: JSONObject) {

    var parent:RemoteProperties? = null
    constructor(json:JSONObject, parent: RemoteProperties) : this(json) {
        this.parent = parent
        val curProp = jsonObj
        buttonId
        jsonObj = parent.addButton(jsonObj)?: jsonObj
        curProp.keys().forEach {
            jsonObj.put(it, curProp.get(it))
        }
        parent.update()
    }

    var length: String = jsonObj.getString(Strings.btnPropLength)
        get(){
            return jsonObj.getString(Strings.btnPropLength)
        }
        set(value){
            field = value
            jsonObj.put(Strings.btnPropLength, value)
            parent?.update()
        }

    var irCode: String = jsonObj.getString(Strings.btnPropIrcode)
        get(){
            return jsonObj.getString(Strings.btnPropIrcode)
        }
        set(value){
            field = value
            jsonObj.put(Strings.btnPropIrcode, value)
            listener?.onIrModified()
            parent?.update()
        }

    var text: String = jsonObj.getString(Strings.btnPropText)
        get(){
            return jsonObj.getString(Strings.btnPropText)
        }
        set(value){
            field = value
            jsonObj.put(Strings.btnPropText, value)
            listener?.onTextModified()
            parent?.update()
        }

    var iconType = jsonObj.getInt(Strings.btnPropIconType)
        get(){
            return jsonObj.getInt(Strings.btnPropIconType)
        }
        set(value){
            field = value
            jsonObj.put(Strings.btnPropIconType, value)
            listener?.onTypeModified()
            parent?.update()
        }

    var color = jsonObj.getInt(Strings.btnPropColor)
        get(){
            return  jsonObj.getInt(Strings.btnPropColor)
        }
        set(value){
            field = value
            jsonObj.put(Strings.btnPropColor, value)
            listener?.onColorModified()
            parent?.update()
        }

    var icon = jsonObj.getInt(Strings.btnPropIcon)
        get(){
            return jsonObj.getInt(Strings.btnPropIcon)
        }
        set(value){
            field = value
            jsonObj.put(Strings.btnPropIcon, value)
            listener?.onIconModified()
            parent?.update()
        }

    var textColor = jsonObj.getInt(Strings.btnPropTextColor)
        get(){
            return jsonObj.getInt(Strings.btnPropTextColor)
        }
        set(value){
            field = value
            jsonObj.put(Strings.btnPropTextColor, value)
            listener?.onTextColorChanged()
            parent?.update()
        }

    var btnPosition  = jsonObj.getInt(Strings.btnPropBtnPosition)
        get(){
            return jsonObj.getInt(Strings.btnPropBtnPosition)
        }
        set(value){
            field = value
            jsonObj.put(Strings.btnPropBtnPosition, value)
            listener?.onPositionModified()
            parent?.update()
        }

    var buttonId: Long = jsonObj.optLong(Strings.btnPropBtnId)
        get(){
            if(jsonObj.optLong(Strings.btnPropBtnId) == 0L){
                jsonObj.put(Strings.btnPropBtnId, System.currentTimeMillis())
                parent?.update()
            }
            return jsonObj.getLong(Strings.btnPropBtnId)
        }
        set(value){
            field = value
            jsonObj.put(Strings.btnPropBtnId, value)
            parent?.update()
        }

    var buttonShowAnimation = true

    private var listener: OnRemoteButtonModificationListener?= null

    fun setOnModificationListener(listener: OnRemoteButtonModificationListener){
        this.listener=listener
    }
}