package com.github.meudayhegde.esputils.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.databinding.DeviceListScanItemBinding
import com.github.meudayhegde.esputils.holders.ARPItem

class ScanDeviceListAdapter(private val arpItemList: ArrayList<ARPItem>) :
    RecyclerView.Adapter<ScanDeviceListAdapter.DeviceScanListViewHolder>() {

    class DeviceScanListViewHolder(val viewBinding: DeviceListScanItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceScanListViewHolder {
        return DeviceScanListViewHolder(
            DeviceListScanItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    private var onARPItemSelectedListener: ((arpItem: ARPItem) -> Unit)? = null
    fun setOnARPItemSelectedListener(onARPItemSelectedListener: (arpItem: ARPItem) -> Unit) {
        this.onARPItemSelectedListener = onARPItemSelectedListener
    }

    override fun onBindViewHolder(holder: DeviceScanListViewHolder, position: Int) {
        val item = arpItemList[position]
        holder.viewBinding.deviceIpAddress.text = item.ipAddress
        item.devNickName?.let {
            holder.viewBinding.deviceIpAddress.text = holder.viewBinding.root.context.getString(
                R.string.scan_list_item_ip_text, item.ipAddress, it
            )
        }
        holder.viewBinding.deviceMacAddress.text = item.macAddress
        holder.viewBinding.root.setOnClickListener { onARPItemSelectedListener?.invoke(item) }
    }

    override fun getItemCount() = arpItemList.size
}