package com.irware.remote.ui.dialogs

import android.app.ActionBar
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.GridView
import android.widget.LinearLayout
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.listeners.ButtonDragListener
import com.irware.remote.listeners.ButtonTouchListener
import com.irware.remote.ui.adapters.ButtonLayoutAdapter
import com.irware.remote.ui.buttons.RemoteButton

class CreateRemoteDialog(context: Context?) : Dialog(context, R.style.AppTheme) {

    var columns=3

    init {
        var layout_width=MainActivity.size.x/(columns+1)
        var layout_height=layout_width/3
        var layoutList:ArrayList<LinearLayout> =ArrayList()
        for(i in 1..(MainActivity.size.y/(layout_height+10))*columns){
            var layout=LinearLayout(context)
            layout.layoutParams= ViewGroup.LayoutParams(layout_width,layout_width)
            layout.minimumWidth=layout_width
            layout.minimumHeight==layout_height
            layout.gravity=Gravity.CENTER
            layout.setBackgroundResource(R.drawable.layout_with_border_bg)
            layout.setOnDragListener(ButtonDragListener())
            layoutList.add(layout)
        }
        var btn=RemoteButton(context, ButtonProperties("",RemoteButton.TYPE_RECT_VER,RemoteButton.COLOR_GREEN,10,""))
        btn.setOnClickListener({})

        var btn2=RemoteButton(context, ButtonProperties("",RemoteButton.TYPE_RECT_HOR,RemoteButton.COLOR_GREY,8,"hello"))
        btn2.setOnClickListener({})
        btn2.setIcon(R.drawable.ic_home_drawer)

        btn.setOnTouchListener(ButtonTouchListener())
        btn2.setOnTouchListener(ButtonTouchListener())

        layoutList.get(btn.getProperties().getPosition()).addView(btn)
        layoutList.get(btn2.getProperties().getPosition()).addView(btn2)

        setContentView(R.layout.create_remote_layout)
        val gridLayout= this.findViewById<GridView>(R.id.gridview_remote)
        gridLayout.adapter=ButtonLayoutAdapter(layoutList)
        gridLayout.setOnItemClickListener(null)
        gridLayout.columnWidth=layout_width
    }
}
