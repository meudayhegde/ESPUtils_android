package com.irware.remote.ui.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import com.irware.remote.R
import com.irware.remote.ui.buttons.CheckableImageView

class ButtonStyleAdapter(private var paramList: ArrayList<AbsListView.LayoutParams>,
                         private var iconRes: Int): BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = CheckableImageView(parent?.context)
        view.layoutParams = paramList[position]
        view.setPadding(5, 5, 5, 5)
        view.setImageResource(iconRes)
        view.setBackgroundResource(R.drawable.checkable_bg)
        return view
    }

    override fun getItem(position: Int): AbsListView.LayoutParams {
        return paramList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return paramList.size
    }

    fun setIconres(icRes:Int){
        iconRes=icRes
    }
}