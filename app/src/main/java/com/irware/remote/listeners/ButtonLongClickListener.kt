package com.irware.remote.listeners

import android.view.View
import android.view.View.OnLongClickListener
import android.content.ClipData

class ButtonLongClickListener: OnLongClickListener {
    override fun onLongClick(view: View?): Boolean {
        val data = ClipData.newPlainText("", "")
        val shadowBuilder = View.DragShadowBuilder(view)
        view?.startDrag(data, shadowBuilder, view, 0)
        view?.visibility = View.INVISIBLE
        return true
    }
}