package com.irware.remote.ui.buttons

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.DrawableCompat
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.holders.OnModificationListener


class RemoteButton : Button {
    private var properties:ButtonProperties?=null
    private val dr = GradientDrawable()

    constructor(context:Context):super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, int:Int) : super(context, attrs,int)
    constructor(context: Context?, properties:ButtonProperties):super(context){
        this.properties=properties
        setTextColor(Color.WHITE)
        setButtonProperties()

        properties.setOnModificationListener(object:OnModificationListener{
            override fun onTextColorChanged() {
                setTextColor(properties.getTextColor())
                onIconModified()
            }

            override fun onTypeModified() {
                setType(type = properties.getAlignType())
                onIconModified()
            }

            override fun onIconModified() {
                setIcon(MainActivity.iconDrawableList[properties.getIcon()])
            }

            override fun onTextModified() {
                text = properties.getText()
            }

            override fun onColorModified() {
                dr.setColor(properties.getColor())
            }

            override fun onIrModified() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onPositionModified() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        })
    }

    @SuppressLint("InlinedApi")
    private fun setButtonProperties(){
        setType(type = properties?.getAlignType()!!)
        text = properties?.getText()!!
        dr.setColor(properties?.getColor()!!)
        gravity = Gravity.CENTER
        setTextSize(AUTO_SIZE_TEXT_TYPE_UNIFORM, 12F)
        dr.setStroke(2,Color.BLACK)
        dr.cornerRadius = 100F
        dr.setColor(properties!!.getColor())
        background = dr
        text = properties!!.getText()
        setTextColor(properties!!.getTextColor())
        setIcon(MainActivity.iconDrawableList[properties!!.getIcon()])
    }

    fun getProperties():ButtonProperties{
        return properties!!
    }


    fun setIcon(drawable_resid:Int){
        if(drawable_resid != MainActivity.iconDrawableList[0]){
            var drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) context.getDrawable(drawable_resid)
            else context.resources.getDrawable(drawable_resid)
            drawable = drawable?.mutate()
            DrawableCompat.setTint(drawable!!,properties!!.getTextColor())
            if(properties!!.getAlignType() == TYPE_RECT_VER) {
                setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
                setPadding(0,0,0,0)
            }
            else {
                setCompoundDrawablesWithIntrinsicBounds(drawable,null,null,null)
                setPadding(10,0,0,0)
            }
        }else{
            setCompoundDrawablesWithIntrinsicBounds(null,null,null,null)
        }

    }

    fun setType(type:Int){
        when(type){
            TYPE_RECT_HOR -> {
                layoutParams= LinearLayout.LayoutParams(BTN_WIDTH, MIN_HIGHT)
            }
            TYPE_ROUND_MINI -> {
                layoutParams= LinearLayout.LayoutParams(MIN_HIGHT,MIN_HIGHT)
            }
            TYPE_ROUND_MEDIUM -> {
                layoutParams= LinearLayout.LayoutParams(BTN_WIDTH,BTN_WIDTH)
            }
            TYPE_RECT_VER -> {
                layoutParams= LinearLayout.LayoutParams(MIN_HIGHT,BTN_WIDTH)
            }
        }
        (layoutParams as LinearLayout.LayoutParams).setMargins(12,12,12,12)
    }

    companion object{

        const val TYPE_ROUND_MINI=0
        const val TYPE_RECT_HOR=1
        const val TYPE_RECT_VER=2
        const val TYPE_ROUND_MEDIUM=3

        var BTN_WIDTH=160
        var MIN_HIGHT=80

        fun onActivityLoad(){
            BTN_WIDTH = (MainActivity.size.x-MainActivity.size.x/(2*MainActivity.NUM_COLUMNS)) / MainActivity.NUM_COLUMNS
            MIN_HIGHT = BTN_WIDTH / 2
        }
    }
}