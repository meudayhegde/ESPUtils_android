package com.github.meudayhegde.esputils.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.meudayhegde.esputils.databinding.CommonListItemBinding
import com.github.meudayhegde.esputils.holders.ListItemCommon

class ListAdapterCommon(private val itemList: ArrayList<ListItemCommon>) : RecyclerView.Adapter<ListAdapterCommon.CommonListViewHolder>(){

    class CommonListViewHolder(val viewBinding: CommonListItemBinding) : RecyclerView.ViewHolder(viewBinding.root)

    private var onItemClickListener: ((viewHolder: CommonListViewHolder, item: ListItemCommon) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): CommonListViewHolder {
        return CommonListViewHolder(
            CommonListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: CommonListViewHolder, position: Int) {
        val listItem = itemList[position]
        holder.viewBinding.itemTitle.text = listItem.title
        holder.viewBinding.itemSubtitle.text = listItem.subTitle
        holder.viewBinding.itemIcon.setImageResource(listItem.iconRes)
        onItemClickListener?.let { listener ->
            holder.viewBinding.root.setOnClickListener {
                listener.invoke(holder, listItem)
            }
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    /**
     * to carry onClick action for [RecyclerView] list item.
     * @param listener invokes this object with arguments [CommonListViewHolder] and [ListItemCommon]
     * with this view of the recycler view item can be completely handled
     */
    fun setOnItemClickListener(listener: ((viewHolder: CommonListViewHolder, item: ListItemCommon) -> Unit)): ListAdapterCommon{
        onItemClickListener = listener
        return this
    }
}