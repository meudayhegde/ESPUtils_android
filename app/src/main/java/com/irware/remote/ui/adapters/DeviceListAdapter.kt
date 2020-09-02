package com.irware.remote.ui.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.DeviceProperties
import com.irware.remote.net.SocketClient
import org.json.JSONArray
import org.json.JSONObject

class DeviceListAdapter(private val propList: ArrayList<DeviceProperties>) : RecyclerView.Adapter<DeviceListAdapter.MyViewHolder>(){

    class MyViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        val cardView = LayoutInflater.from(parent.context).inflate(R.layout.device_list_item, parent, false) as CardView
        return MyViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val prop = propList[position]
        setViewProps(holder.cardView, prop)
    }

    @SuppressLint("SetTextI18n")
    private fun setViewProps(cardView: CardView, prop: DeviceProperties){
        cardView.findViewById<TextView>(R.id.name_device).text = prop.nickName
        cardView.findViewById<TextView>(R.id.mac_addr).text = "(${prop.macAddr})"
        cardView.findViewById<TextView>(R.id.device_desc).text = prop.description

        val icOnline = cardView.findViewById<ImageView>(R.id.img_online)
        val icOffline = cardView.findViewById<ImageView>(R.id.img_offline)
        val refresh = cardView.findViewById<ProgressBar>(R.id.progress_status)
        val status = cardView.findViewById<TextView>(R.id.status_text)
        val ipText = cardView.findViewById<TextView>(R.id.ip_addr)

        refresh.visibility = View.VISIBLE
        icOnline.visibility = View.GONE
        icOffline.visibility = View.GONE
        status.text = cardView.context.getString(R.string.connecting)
        ipText.text = ""
        Thread{
            var connected = false
            for(i: Int in 0..prop.ipAddr!!.length()){
                try {
                    val addr = prop.ipAddr!!.get(i) as String
                    val connector = SocketClient.Connector(addr)
                    connector.sendLine("{\"request\":\"ping\"}")
                    val response = connector.readLine()
                    val macAddr = JSONObject(response).getString("MAC")
                    if(macAddr != prop.macAddr) throw Exception()
                    prop.ipAddr!!.remove(i)
                    prop.ipAddr!!.insert(0, addr)
                    prop.update()
                    connected = true; break
                }catch(ex: Exception){ }
            }
            (cardView.context as Activity).runOnUiThread{
                refresh.visibility = View.GONE
                icOnline.visibility = if(connected) View.VISIBLE else View.GONE
                icOffline.visibility = if(connected) View.GONE else View.VISIBLE
                status.text = cardView.context.getString(if(connected) R.string.online else R.string.offline)
                ipText.text = prop.ipAddr!!.get(0) as String
            }

        }.start()
    }

    override fun getItemCount() = propList.size
}

private fun JSONArray.insert(position: Int, value: Any){
    for (i in length() downTo position + 1) {
        put(i, get(i - 1))
    }
    put(position, value)
}