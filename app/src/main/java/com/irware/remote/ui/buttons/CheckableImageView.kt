package com.irware.remote.ui.buttons

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import android.widget.ImageView

class CheckableImageView : ImageView,Checkable{
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?,int:Int) : super(context, attrs,int)

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }

    private var isChecked=false
    override fun isChecked(): Boolean {
        return isChecked
    }

    override fun toggle() {
        setChecked(!isChecked)
    }

    override fun setChecked(checked: Boolean) {
        if(isChecked!=checked){
            isChecked=checked
            refreshDrawableState()
        }
    }

    companion object {
        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }
}