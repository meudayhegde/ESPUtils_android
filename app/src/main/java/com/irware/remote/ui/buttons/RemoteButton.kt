package com.irware.remote.ui.buttons

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.graphics.drawable.DrawableCompat
import com.irware.remote.MainActivity
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.holders.OnModificationListener
import kotlin.math.min


class RemoteButton : Button {
    private var properties:ButtonProperties?=null
    private val gd = GradientDrawable()
    private val gdPressed = GradientDrawable()
    private val stateDrawable = StateListDrawable()

    constructor(context:Context):super(context){ visibility = View.GONE }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs){ visibility = View.GONE }
    constructor(context: Context?, attrs: AttributeSet?, int:Int) : super(context, attrs,int){visibility = View.GONE}

    fun initialize(properties:ButtonProperties?){
        this.properties=properties
        if(properties == null) {
            visibility = View.GONE
            return
        }
        visibility = View.VISIBLE
        setTextColor(Color.WHITE)
        setButtonProperties(properties)

        properties.setOnModificationListener(object:OnModificationListener{
            override fun onTextColorChanged() {
                setTextColor(properties.textColor)
                onIconModified()
            }

            override fun onTypeModified() {
                if(parent is RelativeLayout) setType(properties.iconType,RelativeLayout.CENTER_IN_PARENT)
                else setType(properties.iconType)
                onIconModified()
            }

            override fun onIconModified() {
                setIcon(MainActivity.iconDrawableList[properties.icon])

            }

            override fun onTextModified() {
                text = properties.text
            }

            override fun onColorModified() {
                setBackgroundColor(properties.color)
            }

            override fun onIrModified() {}
            override fun onPositionModified() {}

        })
    }

    private fun setButtonProperties(btnProperties:ButtonProperties){
        properties = btnProperties
        if(parent is RelativeLayout) setType(btnProperties.iconType,RelativeLayout.CENTER_IN_PARENT)
        else setType(btnProperties.iconType)
        text = btnProperties.text
        gravity = Gravity.CENTER
        setTextSize(AUTO_SIZE_TEXT_TYPE_UNIFORM, 12F)

        gd.cornerRadius = 100F;gd.orientation = GradientDrawable.Orientation.BOTTOM_TOP
        gdPressed.cornerRadius = 100F;gdPressed.orientation = GradientDrawable.Orientation.BOTTOM_TOP
        setBackgroundColor(btnProperties.color)

        stateDrawable.addState(intArrayOf(android.R.attr.state_pressed),gdPressed)
        stateDrawable.addState(intArrayOf(),gd)

        background = stateDrawable

        setTextColor(btnProperties.textColor)
        setIcon(MainActivity.iconDrawableList[btnProperties.icon])
    }

    override fun setBackgroundColor(color: Int) {
        setButtonDrawableColor(gd,gdPressed,properties!!.color)
    }

    fun getProperties():ButtonProperties{
        return properties!!
    }


    fun setIcon(drawable_resid:Int){
        if(drawable_resid != MainActivity.iconDrawableList[0]){
            var drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) context.getDrawable(drawable_resid)
            else with(context) {
                @Suppress("DEPRECATION")
                resources.getDrawable(drawable_resid)
            }
            drawable = drawable?.mutate()
            DrawableCompat.setTint(drawable!!,properties!!.textColor)
            if(properties!!.iconType == TYPE_RECT_VER) {
                setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
                setPadding(0,10,0,0)
            }
            else {
                setCompoundDrawablesWithIntrinsicBounds(drawable,null,null,null)
                setPadding(10,0,0,0)
            }
        }else{
            setCompoundDrawablesWithIntrinsicBounds(null,null,null,null)
        }

    }

    fun setType(type:Int,vararg params:Int){
        when(type){
            TYPE_RECT_HOR -> {
                layoutParams = when (parent) {
                    is LinearLayout -> LinearLayout.LayoutParams(BTN_WIDTH, MIN_HEIGHT)
                    is RelativeLayout -> RelativeLayout.LayoutParams(BTN_WIDTH, MIN_HEIGHT)
                    else -> ViewGroup.LayoutParams(BTN_WIDTH, MIN_HEIGHT)
                }
            }
            TYPE_ROUND_MINI -> {
                layoutParams = when (parent) {
                    is LinearLayout -> LinearLayout.LayoutParams(MIN_HEIGHT, MIN_HEIGHT)
                    is RelativeLayout -> RelativeLayout.LayoutParams(MIN_HEIGHT, MIN_HEIGHT)
                    else -> ViewGroup.LayoutParams(MIN_HEIGHT, MIN_HEIGHT)
                }
            }
            TYPE_ROUND_MEDIUM -> {
                layoutParams = when (parent) {
                    is LinearLayout -> LinearLayout.LayoutParams(BTN_WIDTH, BTN_WIDTH)
                    is RelativeLayout -> RelativeLayout.LayoutParams(BTN_WIDTH, BTN_WIDTH)
                    else -> ViewGroup.LayoutParams(BTN_WIDTH, BTN_WIDTH)
                }
            }
            TYPE_RECT_VER -> {
                layoutParams = when (parent) {
                    is LinearLayout -> LinearLayout.LayoutParams(MIN_HEIGHT, BTN_WIDTH)
                    is RelativeLayout -> RelativeLayout.LayoutParams(MIN_HEIGHT, BTN_WIDTH)
                    else -> ViewGroup.LayoutParams(MIN_HEIGHT, BTN_WIDTH)
                }
            }
        }

        if(layoutParams is LinearLayout.LayoutParams){
            (layoutParams as LinearLayout.LayoutParams).setMargins(12,12,12,12)
            params.forEach { (layoutParams as RelativeLayout.LayoutParams).addRule(it) }
        }else{
            (layoutParams as RelativeLayout.LayoutParams).setMargins(12,12,12,12)
            params.forEach { (layoutParams as RelativeLayout.LayoutParams).addRule(it) }
        }
    }

    companion object{

        const val TYPE_ROUND_MINI=0
        const val TYPE_RECT_HOR=1
        const val TYPE_RECT_VER=2
        const val TYPE_ROUND_MEDIUM=3

        var BTN_WIDTH=160
        var MIN_HEIGHT=80

        fun onConfigChanged(){
            val x = min(MainActivity.size.x,MainActivity.size.y)
            BTN_WIDTH = ((x-x/(1.3*MainActivity.NUM_COLUMNS)) / MainActivity.NUM_COLUMNS).toInt()
            MIN_HEIGHT = BTN_WIDTH / 2
        }

        /*
         * Set Normal and Pressed gradient color from a single color int
         */
        fun setButtonDrawableColor(drawableNormal:GradientDrawable, drawablePressed:GradientDrawable, colorInt:Int){
            val red = Color.red(colorInt)
            val green = Color.green(colorInt)
            val blue = Color.blue(colorInt)
            val newColorInt = Color.argb(0xFF,
                when{red>0x80->red-0x30;else -> red+0x30},
                when{green>0x80->green-0x30;else -> green+0x30},
                when{blue>0x80->blue-0x30;else -> blue+0x30})
            drawableNormal.colors = intArrayOf(colorInt,newColorInt)
            drawableNormal.setStroke(8,newColorInt)

            val pressedColorInt = Color.argb(0xFF,
                when{red>0x80->red-0x20;else -> red+0x20},
                when{green>0x80->green-0x20;else -> green+0x20},
                when{blue>0x80->blue-0x20;else -> blue+0x20})
            val pressedColorGradientInt = Color.argb(0xFF,
                when{red>0x80->red-0x50;else -> red+0x50},
                when{green>0x80->green-0x50;else -> green+0x50},
                when{blue>0x80->blue-0x50;else -> blue+0x50})
            drawablePressed.colors = intArrayOf(pressedColorInt,pressedColorGradientInt)
            drawablePressed.setStroke(8,pressedColorGradientInt)
        }
    }
}
