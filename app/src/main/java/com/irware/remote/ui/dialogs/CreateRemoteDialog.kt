package com.irware.remote.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.widget.AbsListView
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
import org.json.JSONObject

class CreateRemoteDialog(context: Context?) : Dialog(context!!, R.style.AppTheme),OnSelectedListener {

    private var lv:ListView? = null
    init {

        val layoutList = ArrayList<LinearLayout>()
        setContentView(R.layout.create_remote_layout)
        lv = findViewById<ListView>(R.id.btn_layout_listview)
        val fab = findViewById<FloatingActionButton>(R.id.fab_new_button)
        val layoutCount = MainActivity.size.y / (RemoteButton.MIN_HIGHT + 10)
        for (i in 1..layoutCount) {
            val layout = LinearLayout(context)
            layout.layoutParams =
                AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT)
            layout.gravity = Gravity.CENTER
            layout.orientation = LinearLayout.HORIZONTAL
            for (j in 1..MainActivity.size.x / (RemoteButton.BTN_WIDTH)) {
                val child = LinearLayout(context)
                child.gravity = Gravity.CENTER
                child.layoutParams =
                    LinearLayout.LayoutParams(RemoteButton.BTN_WIDTH, LinearLayout.LayoutParams.MATCH_PARENT)
                child.minimumHeight = RemoteButton.MIN_HIGHT
                child.setBackgroundResource(R.drawable.layout_with_border_bg)
                child.setOnDragListener(ButtonDragListener())
                layout.addView(child)
            }
            layoutList.add(layout)
        }
        val adapter = ButtonLayoutAdapter(layoutList)
        lv!!.adapter = adapter

        fab.setOnClickListener {
            ButtonPropertiesDialog(context!!, this).show()
        }
    }

    override fun onSelected(prop: JSONObject) {
        val pos=(lv?.adapter as ButtonLayoutAdapter).getGetEmptyPosition()
        prop.put("position",pos)
        val btn= RemoteButton(lv?.context,ButtonProperties(prop))
        btn.setOnLongClickListener(ButtonLongClickListener())
        (lv?.adapter as ButtonLayoutAdapter).getChildLayout(btn.getProperties().getPosition()).addView(btn)
    }
}
