package com.irware.remote.ui.buttons

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.holders.OnModificationListener

@SuppressLint("ViewConstructor")
class RemoteButton(context: Context?, properties:ButtonProperties) : Button(context) {

    private var properties:ButtonProperties?=null
    init {
        this.properties=properties
        setButtonProperties()
        properties.setOnModificationListener({
            setButtonProperties()
        } as OnModificationListener)
    }

    fun setButtonProperties(){
        setColorMode(properties?.getColorMode()!!)
        setType(properties?.getIconType()!!)
        setText(properties?.getText()!!)
    }

    fun getProperties():ButtonProperties{
        return properties!!
    }

    fun setColorMode(bg_type:Int){
        var bg_res=0
        when(bg_type){
            COLOR_GREY->bg_res = R.drawable.round_btn_bg_grey
            COLOR_RED->bg_res=R.drawable.round_btn_bg_red
            COLOR_GREEN->bg_res=R.drawable.round_btn_bg_green
            COLOR_BLUE->bg_res=R.drawable.round_btn_bg_blue
            COLOR_YELLOW->bg_res=R.drawable.round_btn_bg_yellow
        }
        setBackgroundResource(bg_res)
    }

    fun setIcon(drawable_resid:Int){
        setCompoundDrawablesWithIntrinsicBounds(drawable_resid,0,0,0)
    }

    fun setType(type:Int){
        when(type){
            TYPE_RECT_HOR -> {
                layoutParams= ViewGroup.LayoutParams(MainActivity.size.x/7,MainActivity.size.x/12)
            }
            TYPE_ROUND -> {
                layoutParams= ViewGroup.LayoutParams(MainActivity.size.x/10,MainActivity.size.x/10)
            }
            TYPE_RECT_VER -> {
                layoutParams= ViewGroup.LayoutParams(MainActivity.size.x/12,MainActivity.size.x/7)
            }
        }
    }

    companion object{
        const val COLOR_GREY=0
        const val COLOR_RED=1
        const val COLOR_GREEN=2
        const val COLOR_BLUE=3
        const val COLOR_YELLOW=4

        const val TYPE_ROUND=0
        const val TYPE_RECT_HOR=1
        const val TYPE_RECT_VER=2
    }
}