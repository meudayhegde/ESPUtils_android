package com.irware.remote.holders

import com.irware.remote.ui.buttons.RemoteButton
import org.json.JSONObject

class ButtonProperties {
    private var IR_CODE:String?=null
    private var BTN_TEXT:String=""
    private var ICON_TYPE=0
    private var COLOR=0
    private var BTN_POSITION=0
    private var listener:OnModificationListener?=null

    constructor(ir_code: String, align_type: Int, color: Int, btn_position: Int, btn_text: String) {
        IR_CODE = ir_code
        ICON_TYPE = align_type
        COLOR = color
        BTN_POSITION = btn_position
        BTN_TEXT = btn_text
    }
    constructor(obj:JSONObject){
        IR_CODE = obj.get("code") as String
        ICON_TYPE = obj.getInt("type")
        COLOR = obj.getInt("color")
        BTN_POSITION = obj.getInt("position")
        BTN_TEXT = obj.getString("text")
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
        return BTN_TEXT!!
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
}