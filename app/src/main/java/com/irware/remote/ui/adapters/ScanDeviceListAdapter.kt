package com.irware.remote.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.irware.remote.R
import com.irware.remote.holders.ARPItem

class ScanDeviceListAdapter(private val arpItemList: ArrayList<ARPItem>) : RecyclerView.Adapter<ScanDeviceListAdapter.DeviceScanListViewHolder>() {

    class DeviceScanListViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView){
        val ipAddressView: TextView = cardView.findViewById(R.id.device_ip_address)
        val macAddressView: TextView = cardView.findViewById(R.id.device_mac_address)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceScanListViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_list_scan_item, parent, false) as CardView
        return DeviceScanListViewHolder(cardView)
    }

    private var onARPItemSelectedListener: ((arpItem: ARPItem) -> Unit)? = null
    fun setOnARPItemSelectedListener(onARPItemSelectedListener: (arpItem: ARPItem) -> Unit){
        this.onARPItemSelectedListener = onARPItemSelectedListener
    }

    override fun onBindViewHolder(holder: DeviceScanListViewHolder, position: Int) {
        val item = arpItemList[position]
        holder.ipAddressView.text = item.ipAddress
        item.devNickName?.let {
            holder.ipAddressView.text = holder.cardView.context.getString(R.string.scan_list_item_ip_text, item.ipAddress, it)
        }
        holder.macAddressView.text = item.macAddress
        holder.cardView.setOnClickListener { onARPItemSelectedListener?.invoke(item) }
    }

    override fun getItemCount() = arpItemList.size
}