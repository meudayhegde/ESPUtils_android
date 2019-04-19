package com.irware.remote.holders

class ButtonProperties(ir_code:String,align_type:Int,color_mode:Int,btn_position:Int,btn_text:String) {
    private var IR_CODE:String?=null
    private var BTN_TEXT:String=""
    private var ICON_TYPE=0
    private var COLOR_MODE=0
    private var BTN_POSITION=0
    private var listener:OnModificationListener?=null

    init{
        IR_CODE=ir_code
        ICON_TYPE=align_type
        COLOR_MODE=color_mode
        BTN_POSITION=btn_position
        BTN_TEXT=btn_text
    }

    fun getPosition():Int{
        return BTN_POSITION
    }

    fun getIrCode():String{
        return IR_CODE!!
    }

    fun getColorMode():Int{
        return COLOR_MODE
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

    fun setColorMode(color_mode: Int){
        COLOR_MODE=color_mode
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