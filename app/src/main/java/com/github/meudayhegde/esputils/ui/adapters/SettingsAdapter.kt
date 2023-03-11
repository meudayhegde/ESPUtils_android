package com.github.meudayhegde.esputils.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.databinding.CommonListItemBinding
import com.github.meudayhegde.esputils.holders.SettingsItem

class SettingsAdapter(private val list: ArrayList<SettingsItem>) :
    RecyclerView.Adapter<SettingsAdapter.SettingsListViewHolder>() {

    class SettingsListViewHolder(val viewBinding: CommonListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsListViewHolder {
        val viewBinding =
            CommonListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SettingsListViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: SettingsListViewHolder, position: Int) {
        holder.viewBinding.itemTitle.text = list[position].title
        holder.viewBinding.itemSubtitle.text = list[position].subtitle
        if (list[position].iconRes != 0) holder.viewBinding.itemIcon.setImageResource(list[position].iconRes)
        val background =
            if (list[position].prop == null) R.drawable.layout_border_round_corner else if (list[position].prop!!.isConnected) R.drawable.round_corner_success else R.drawable.round_corner_error
        holder.viewBinding.root.getChildAt(0).setBackgroundResource(background)
        holder.viewBinding.root.setOnClickListener {
            list[position].dialog?.setTitle(list[position].title)
            list[position].dialog?.show()
            list[position].clickAction?.run()
        }
    }

    override fun getItemCount() = list.size
}