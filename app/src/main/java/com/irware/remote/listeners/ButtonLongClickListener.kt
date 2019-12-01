package com.irware.remote.listeners

import android.view.View
import android.view.View.OnLongClickListener
import android.content.ClipData
import android.view.animation.Animation
import android.widget.ImageView
import com.irware.remote.ui.buttons.RemoteButton
import com.irware.remote.ui.dialogs.RemoteDialog

class ButtonLongClickListener(private val remoteDialog: RemoteDialog): OnLongClickListener {
    override fun onLongClick(view: View?): Boolean {
        val data = ClipData.newPlainText("", "")
        val shadowBuilder = View.DragShadowBuilder(view)
        view?.startDrag(data, shadowBuilder, view, 0)
        view?.visibility = View.INVISIBLE
        remoteDialog.delView.visibility = View.VISIBLE
        remoteDialog.infoTextVew.visibility = View.INVISIBLE
        return true
    }
}