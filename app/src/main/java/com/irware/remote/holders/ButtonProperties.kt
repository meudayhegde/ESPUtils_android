package com.irware.remote.holders

import android.graphics.Color
import com.irware.remote.ui.buttons.RemoteButton
import org.json.JSONObject

class ButtonProperties {
    private var IR_CODE:String?=null
    private var BTN_TEXT:String=""
    private var ICON_TYPE=0
    private var COLOR=0
    private var BTN_POSITION=0
    private var ICON = 0
    private var TEXT_COLOR = Color.WHITE
    private var listener:OnModificationListener?=null

    constructor(ir_code: String, align_type: Int, color: Int, btn_position: Int, btn_text: String, textColor:Int, icon:Int) {
        IR_CODE = ir_code
        ICON_TYPE = align_type
        COLOR = color
        BTN_POSITION = btn_position
        BTN_TEXT = btn_text
        TEXT_COLOR = textColor
        ICON = icon
    }
    constructor(obj:JSONObject){
        IR_CODE = obj.get("code") as String
        ICON_TYPE = obj.getInt("type")
        COLOR = obj.getInt("color")
        BTN_POSITION = obj.getInt("position")
        BTN_TEXT = obj.getString("text")
        TEXT_COLOR = obj.getInt("textColor")
        ICON = obj.getInt("icon")
    }

    fun getPosition():Int{
        return BTN_POSITION
    }

    fun getIrCode():String{
        return IR_CODE!!
    }

    fun getColor():Int{
        return COLOR
    }
    fun getAlignType():Int{
        return ICON_TYPE
    }
    fun getText():String{
        return BTN_TEXT
    }

    fun getTextColor():Int{
        return TEXT_COLOR
    }

    fun getIcon():Int{
        return ICON
    }

    fun setIcon(icon:Int){
        ICON = icon
    }

    fun setPosition(btn_position:Int){
        BTN_POSITION=btn_position
        listener?.onPositionModified()
    }

    fun setIrCode(ir_code: String){
        IR_CODE=ir_code
        listener?.onIrModified()
    }

    fun setColor(color: Int){
        COLOR=color
        listener?.onColorModified()
    }
    fun setAlignType(icon_type: Int){
        ICON_TYPE=icon_type
        listener?.onIconModified()
    }
    fun setText(btn_text:String){
        BTN_TEXT=btn_text
        listener?.onTextModified()
    }

    fun setTextColor(color:Int){
        TEXT_COLOR = color
        listener?.onTextColorChanged()
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