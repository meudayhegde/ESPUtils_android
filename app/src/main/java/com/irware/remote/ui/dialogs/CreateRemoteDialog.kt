package com.irware.remote.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import com.github.clans.fab.FloatingActionButton
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.ButtonProperties
import com.irware.remote.listeners.ButtonDragListener
import com.irware.remote.listeners.ButtonLongClickListener
import com.irware.remote.ui.adapters.ButtonLayoutAdapter
import com.irware.remote.ui.buttons.RemoteButton

class CreateRemoteDialog(context: Context?) : Dialog(context, R.style.AppTheme) {

    init {

        var layoutList= ArrayList<LinearLayout>()
        setContentView(R.layout.create_remote_layout)
        var lv=findViewById<ListView>(R.id.btn_layout_listview)
        var fab=findViewById<FloatingActionButton>(R.id.fab_new_button)
        var layoutCount=MainActivity.size.y/(RemoteButton.MIN_HIGHT+10)
        for (i in 1..layoutCount){
            var layout=LinearLayout(context)
            layout.layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)
            layout.gravity= Gravity.CENTER
            layout.orientation=LinearLayout.HORIZONTAL
            for(i in 1..MainActivity.size.x/(RemoteButton.BTN_WIDTH)){
                var child=LinearLayout(context)
                child.gravity=Gravity.CENTER
                child.layoutParams=LinearLayout.LayoutParams(RemoteButton.BTN_WIDTH,LinearLayout.LayoutParams.MATCH_PARENT)
                child.minimumHeight=RemoteButton.MIN_HIGHT
                child.setBackgroundResource(R.drawable.layout_with_border_bg)
                child.setOnDragListener(ButtonDragListener())
                layout.addView(child)
            }
            layoutList.add(layout)
        }
        var adapter=ButtonLayoutAdapter(layoutList)
        lv.adapter=adapter
        var btn=RemoteButton(context, ButtonProperties("",RemoteButton.TYPE_RECT_VER,RemoteButton.COLOR_GREEN,10,""))
        btn.setOnClickListener({})

        var btn2=RemoteButton(context, ButtonProperties("",RemoteButton.TYPE_RECT_HOR,RemoteButton.COLOR_GREY,8,"hello"))
        btn2.setOnClickListener({})
        btn2.setIcon(R.drawable.ic_home_drawer)

        btn.setOnLongClickListener(ButtonLongClickListener())
        btn2.setOnLongClickListener(ButtonLongClickListener())
        adapter.getChildLayout(btn.getProperties().getPosition()).addView(btn)
        adapter.getChildLayout(btn2.getProperties().getPosition()).addView(btn2)

        fab.setOnClickListener {
            ButtonPropertiesDilog(context!!).show()
        }
    }
}
