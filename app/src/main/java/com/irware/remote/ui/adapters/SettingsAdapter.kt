package com.irware.remote.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.irware.remote.R
import com.irware.remote.SettingsItem

class SettingsAdapter(private val list: ArrayList<SettingsItem>) : RecyclerView.Adapter<SettingsAdapter.MyViewHolder>(){

    class MyViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView){
        val titleView: TextView = cardView.findViewById(R.id.setting_title)
        val subTitleView: TextView = cardView.findViewById(R.id.setting_subtitle)
        val iconView: ImageView = cardView.findViewById(R.id.ic_settings)
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.settings_list_item, parent, false) as CardView

        return MyViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.titleView.text = list[position].title
        holder.subTitleView.text = list[position].subtitle
        if(list[position].iconRes != 0) holder.iconView.setImageResource(list[position].iconRes)
        val background = if(list[position].prop == null) R.drawable.layout_border_round_corner else
            if(list[position].prop!!.isConnected) R.drawable.round_corner_success else R.drawable.round_corner_error
        holder.cardView.getChildAt(0).setBackgroundResource(background)
        holder.cardView.setOnClickListener{
            list[position].dialog?.setTitle(list[position].title)
            list[position].dialog?.show()
            list[position].clickAction?.run()
        }
    }

    override fun getItemCount() = list.size
}