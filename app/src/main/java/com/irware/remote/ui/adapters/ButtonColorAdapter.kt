package com.irware.remote.ui.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import com.irware.remote.R
import com.irware.remote.ui.buttons.CheckableImageView


class ButtonColorAdapter(icons:IntArray): BaseAdapter() {
    private val iconList= icons

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view=convertView as CheckableImageView?
        if(view==null) {
            view = CheckableImageView(parent?.context)
            view.layoutParams = AbsListView.LayoutParams(50,50)
            view.setPadding(5, 5, 5, 5)
            if(iconList[position]!=0) view.setImageResource(iconList[position])
            view.setBackgroundResource(R.drawable.checkable_bg)
        }
        return view
    }

    override fun getItem(position: Int): Int {
        return iconList[position]
    }

    override fun getItemId(position: Int): Long {
        return iconList[position].toLong()
    }

    override fun getCount(): Int {
        return iconList.size
    }
}