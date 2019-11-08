package com.irware.remote.ui.buttons

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.irware.remote.R
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.holders.OnModificationListener


class RemoteButton : Button {
    private var properties:ButtonProperties?=null
    constructor(context:Context):super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, int:Int) : super(context, attrs,int)
    constructor(context: Context?, properties:ButtonProperties):super(context){
        this.properties=properties
        setTextColor(Color.WHITE)
        setButtonProperties()
        gravity= Gravity.CENTER
        setTextSize(AUTO_SIZE_TEXT_TYPE_UNIFORM, 12F)
        properties.setOnModificationListener(object:OnModificationListener{
            override fun onTypeModified() {
                setType(type = properties.getAlignType())
            }

            override fun onIconModified() {

            }

            override fun onTextModified() {
                text = properties.getText()
            }

            override fun onColorModified() {
                setColorMode(bg_type = properties.getColor())
            }

            override fun onIrModified() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onPositionModified() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        })
    }

    fun setButtonProperties(){
        setType(type = properties?.getAlignType()!!)
        text = properties?.getText()!!
        setColorMode(bg_type = properties?.getColor()!!)
    }

    fun getProperties():ButtonProperties{
        return properties!!
    }

    fun setColorMode(bg_type:Int){
        var bgRes=0
        when(bg_type){
            COLOR_GREY->bgRes = R.drawable.round_btn_bg_grey
            COLOR_RED->bgRes=R.drawable.round_btn_bg_red
            COLOR_GREEN->bgRes=R.drawable.round_btn_bg_green
            COLOR_BLUE->bgRes=R.drawable.round_btn_bg_blue
            COLOR_YELLOW->bgRes=R.drawable.round_btn_bg_yellow
        }
        setBackgroundResource(bgRes)
    }

    fun setIcon(drawable_resid:Int){
        setCompoundDrawablesWithIntrinsicBounds(drawable_resid,0,0,0)
    }

    fun setType(type:Int){
        when(type){
            TYPE_RECT_HOR -> {
                    layoutParams= LinearLayout.LayoutParams(BTN_WIDTH-(2* BTN_PADDING), MIN_HIGHT-(2* BTN_PADDING))
            }
            TYPE_ROUND_MINI -> {
                layoutParams= LinearLayout.LayoutParams(MIN_HIGHT-(2* BTN_PADDING),MIN_HIGHT-(2* BTN_PADDING))
            }
            TYPE_ROUND_MEDIUM -> {
                layoutParams= LinearLayout.LayoutParams(BTN_WIDTH-(2* BTN_PADDING),BTN_WIDTH-(2* BTN_PADDING))
            }
            TYPE_RECT_VER -> {
                layoutParams= ViewGroup.LayoutParams(MIN_HIGHT-(2* BTN_PADDING),BTN_WIDTH-(2* BTN_PADDING))
            }
        }
    }

    companion object{
        const val COLOR_GREY=0
        const val COLOR_RED=4
        const val COLOR_GREEN=2
        const val COLOR_BLUE=1
        const val COLOR_YELLOW=3

        const val TYPE_ROUND_MINI=0
        const val TYPE_RECT_HOR=1
        const val TYPE_RECT_VER=2
        const val TYPE_ROUND_MEDIUM=3

        const val BTN_WIDTH=160
        const val MIN_HIGHT=80
        const val BTN_PADDING=10
    }
}