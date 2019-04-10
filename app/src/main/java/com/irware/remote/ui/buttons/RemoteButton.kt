package com.irware.remote.ui.buttons

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import com.irware.remote.MainActivity
import com.irware.remote.R

class RemoteButton : Button {
    private var irCode:String?=null
    private var type=TYPE_RECT_HOR
    constructor(context: Context?,type:Int,bg_res:Int) : super(context){
        this.type=type
        setColorResource(bg_res)
        setType(type)
    }

    fun setColorResource(bg_res:Int){
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
        val COLOR_GREY=R.drawable.round_btn_bg_grey
        val COLOR_RED=R.drawable.round_btn_bg_red
        val COLOR_GREEN=R.drawable.round_btn_bg_green
        val COLOR_BLUE=R.drawable.round_btn_bg_blue
        val COLOR_YELLOW=R.drawable.round_btn_bg_yellow

        val TYPE_ROUND=0
        val TYPE_RECT_HOR=1
        val TYPE_RECT_VER=2
    }
}