package com.irware.remote.ui.adapters

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.irware.remote.MainActivity
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
        holder.cardView.setOnClickListener{
            list[position].dialog.setTitle(list[position].title)
            list[position].dialog.show()
            list[position].dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            list[position].dialog.window?.setLayout((MainActivity.size.x*0.9).toInt(),WindowManager.LayoutParams.WRAP_CONTENT)

        }
    }

    override fun getItemCount() = list.size
}