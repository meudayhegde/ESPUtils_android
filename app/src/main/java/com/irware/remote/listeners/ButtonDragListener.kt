package com.irware.remote.listeners

import android.view.DragEvent
import android.widget.LinearLayout
import android.view.ViewGroup
import android.view.View
import android.view.View.OnDragListener

internal class ButtonDragListener : OnDragListener {
    override fun onDrag(v: View, event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DROP -> {
                val view = event.localState as View
                val owner = view.parent as ViewGroup
                val container = v as LinearLayout
                if(container.childCount==0) {
                    owner.removeView(view)
                    container.addView(view)
                }
                view.visibility = View.VISIBLE
            }
            DragEvent.ACTION_DRAG_ENDED ->{
                (event.localState as View).visibility=View.VISIBLE
            }
        }
        return true
    }
}