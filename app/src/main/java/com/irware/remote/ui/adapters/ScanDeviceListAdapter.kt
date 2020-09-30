package com.irware.remote.ui.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.irware.remote.MainActivity
import com.irware.remote.OnSocketReadListener
import com.irware.remote.R
import com.irware.remote.SettingsItem
import com.irware.remote.net.ARPTable
import com.irware.remote.holders.DeviceProperties
import com.irware.remote.holders.OnStatusUpdateListener
import com.irware.remote.net.ARPItem
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.fragments.DevicesFragment
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min

class ScanDeviceListAdapter(private val arpItemList: ArrayList<ARPItem>) : RecyclerView.Adapter<ScanDeviceListAdapter.MyViewHolder>() {

    class MyViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView){
        val ipAddrView: TextView = cardView.findViewById(R.id.device_ip_address)
        val macAddrView: TextView = cardView.findViewById(R.id.device_mac_address)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_list_scan_item, parent, false) as CardView
        return MyViewHolder(cardView)
    }

    private var onARPItemSelectedListener: OnARPItemSelectedListener? = null
    fun setOnARPItemSelectedListener(onARPItemSelectedListener: OnARPItemSelectedListener){
        this.onARPItemSelectedListener = onARPItemSelectedListener
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = arpItemList[position]
        holder.ipAddrView.text = item.ipAddress
        holder.macAddrView.text = item.macAddress
        holder.cardView.setOnClickListener { onARPItemSelectedListener?.onARPItemSelected(item) }
    }

    override fun getItemCount() = arpItemList.size
}

interface OnARPItemSelectedListener{
    fun onARPItemSelected(arpItem: ARPItem)
}