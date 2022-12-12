package com.irware.remote.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.irware.remote.R
import com.irware.remote.holders.ListItemCommon

class ListAdapterCommon(private val itemList: ArrayList<ListItemCommon>) : RecyclerView.Adapter<ListAdapterCommon.CommonListViewHolder>(){

    class CommonListViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView){
        val titleView: TextView = cardView.findViewById(R.id.setting_title)
        val subTitleView: TextView = cardView.findViewById(R.id.setting_subtitle)
        val iconView: ImageView = cardView.findViewById(R.id.ic_settings)
    }

    private var onItemClickListener: ((viewHolder: CommonListViewHolder, item: ListItemCommon) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): CommonListViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.common_list_item, parent, false) as CardView

        return CommonListViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: CommonListViewHolder, position: Int) {
        val listItem = itemList[position]
        holder.titleView.text = listItem.title
        holder.subTitleView.text = listItem.subTitle
        holder.iconView.setImageResource(listItem.iconRes)
        onItemClickListener?.let { listener ->
            holder.cardView.setOnClickListener {
                listener.invoke(holder, listItem)
            }
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    fun setOnItemClickListener(listener: ((viewHolder: CommonListViewHolder, item: ListItemCommon) -> Unit)): ListAdapterCommon{
        onItemClickListener = listener
        return this
    }
}