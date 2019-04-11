package com.irware.remote.ui.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout

class ButtonLayoutAdapter(layoutLists:List<LinearLayout>) : BaseAdapter(){
    var layoutLists:List<LinearLayout>?=null
    init{
        this.layoutLists=layoutLists
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return layoutLists?.get(position) as View
    }

    override fun getItem(position: Int): LinearLayout? {
        return layoutLists?.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position as Long
    }

    override fun getCount(): Int {
        return layoutLists?.size!!
    }
}