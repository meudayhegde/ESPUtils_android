package com.irware.remote.ui.adapters

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.listeners.ButtonDragListener
import com.irware.remote.ui.buttons.RemoteButton

class ButtonLayoutAdapter(layoutList: ArrayList<LinearLayout>) : BaseAdapter(){

    var layoutList: ArrayList<LinearLayout>
    init{
        this.layoutList=layoutList
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        return layoutList[position]
    }

    override fun getItem(position: Int): LinearLayout {
        return layoutList[position]
    }

    fun getChildLayout(position:Int):LinearLayout{
        return getItem(position/(MainActivity.size.x/(RemoteButton.BTN_WIDTH))).getChildAt(position%(MainActivity.size.x/(RemoteButton.BTN_WIDTH))) as LinearLayout
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun getCount(): Int {
        return layoutList.size
    }
}