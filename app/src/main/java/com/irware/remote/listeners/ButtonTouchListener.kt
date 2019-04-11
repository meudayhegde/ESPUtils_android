package com.irware.remote.listeners

import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.View.DragShadowBuilder
import android.content.ClipData



class ButtonTouchListener: OnTouchListener {
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        if (event?.action === MotionEvent.ACTION_DOWN) {
            val data = ClipData.newPlainText("", "")
            val shadowBuilder = View.DragShadowBuilder(
                view
            )
            view?.startDrag(data, shadowBuilder, view, 0)
            view?.visibility = View.INVISIBLE
            return true
        } else return false
    }
}