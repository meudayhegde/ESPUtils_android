package com.github.meudayhegde.esputils.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.GridView

class CustomGridView: GridView {
    constructor(context:Context) : super(context)
    constructor(context:Context,attrs:AttributeSet) : super(context,attrs)
    constructor(context:Context,attrs:AttributeSet,defStyle:Int) : super(context,attrs,defStyle)

    @SuppressLint("Range")
    override fun onMeasure(widthMeasureSpec:Int, heightMeasureSpec:Int) {

        val heightSpec: Int = if (layoutParams.height == LayoutParams.WRAP_CONTENT) {

            MeasureSpec.makeMeasureSpec(
                Integer.MAX_VALUE shl 2 , MeasureSpec.AT_MOST)
        } else {
            // Any other height should be respected as is.
            heightMeasureSpec
        }

        super.onMeasure(widthMeasureSpec, heightSpec)
    }
}