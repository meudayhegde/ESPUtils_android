package com.irware.remote.ui.adapters

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.LinearLayout
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.listeners.ButtonDragListener
import com.irware.remote.ui.buttons.RemoteButton

class ButtonLayoutAdapter(var layoutList: ArrayList<LinearLayout>) : BaseAdapter(){

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

    fun getGetEmptyPosition():Int{
        for(i in 0 until layoutList.size){
            for(j in 0 until (MainActivity.size.x/(RemoteButton.BTN_WIDTH))){
                if((layoutList[i].getChildAt(j)as LinearLayout).childCount==0)
                    return (i*(MainActivity.size.x/(RemoteButton.BTN_WIDTH)))+j
            }
        }
        val layout=LinearLayout(layoutList[0].context)
        layout.layoutParams =
            AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT)
        layout.gravity = Gravity.CENTER
        layout.orientation = LinearLayout.HORIZONTAL
        for (i in 1..MainActivity.size.x / (RemoteButton.BTN_WIDTH)) {
            val child = LinearLayout(layout.context)
            child.gravity = Gravity.CENTER
            child.layoutParams =
                LinearLayout.LayoutParams(RemoteButton.BTN_WIDTH, LinearLayout.LayoutParams.MATCH_PARENT)
            child.minimumHeight = RemoteButton.MIN_HIGHT
            child.setBackgroundResource(R.drawable.layout_with_border_bg)
            child.setOnDragListener(ButtonDragListener())
            layout.addView(child)
        }
        layoutList.add(layout)
        return ((layoutList.size-1)*(MainActivity.size.x/(RemoteButton.BTN_WIDTH)))
    }
}