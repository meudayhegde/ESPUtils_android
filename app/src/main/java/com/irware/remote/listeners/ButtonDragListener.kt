package com.irware.remote.listeners

import android.view.DragEvent
import android.widget.LinearLayout
import android.view.ViewGroup
import android.view.View
import android.view.View.OnDragListener



internal class ButtonDragListener : OnDragListener {

    override fun onDrag(v: View, event: DragEvent): Boolean {
        val action = event.action
        when (event.action) {
            DragEvent.ACTION_DROP -> {
                val view = event.localState as View
                val owner = view.parent as ViewGroup
                owner.removeView(view)
                val container = v as LinearLayout
                container.addView(view)
                view.visibility = View.VISIBLE
            }
        }
        return true
    }
}