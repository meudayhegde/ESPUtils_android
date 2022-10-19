package com.irware.remote.ui.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.irware.ThreadHandler
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.GPIOObject
import com.irware.remote.holders.OnStatusUpdateListener
import com.irware.remote.net.ARPTable
import com.irware.remote.net.SocketClient
import org.json.JSONObject

class GPIOListAdapter(private val propList: ArrayList<GPIOObject>) : RecyclerView.Adapter<GPIOListAdapter.MyViewHolder>(){

    class MyViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView){
        val title: TextView = cardView.findViewById(R.id.gpio_name)
        val subTitle: TextView = cardView.findViewById(R.id.gpio_description)
        val gpioSwitch: SwitchCompat = cardView.findViewById(R.id.gpio_switch)
        val statusLayout: LinearLayout = cardView.findViewById(R.id.gpio_intermediate)
        val progressBar: ProgressBar = cardView.findViewById(R.id.progress_status)
        val progressImg: ImageView = cardView.findViewById(R.id.img_offline)
        val progressText: TextView = cardView.findViewById(R.id.status_text)
        val iconDrawable = ContextCompat.getDrawable(cardView.context, R.drawable.icon_lamp)

        init{
            cardView.findViewById<ImageView>(R.id.ic_gpio_list_item).setImageDrawable(iconDrawable)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val cardView = LayoutInflater.from(parent.context).inflate(R.layout.gpio_list_item, parent, false) as CardView
        return MyViewHolder(cardView)
    }

    @SuppressLint("InflateParams")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val prop = propList[position]

        holder.title.text = prop.title
        holder.subTitle.text = "Device: ${prop.deviceProperties?.nickName?: "Unknown"}\n${prop.subTitle}"
        holder.iconDrawable?.setTint(MainActivity.colorOnBackground)

        holder.cardView.isEnabled = false
        holder.gpioSwitch.visibility = View.GONE

        holder.statusLayout.visibility = View.VISIBLE
        holder.progressBar.visibility = View.VISIBLE
        holder.progressImg.visibility = View.GONE
        holder.progressText.text = holder.cardView.context.getString(R.string.loading)

        holder.cardView.getChildAt(0).background = ContextCompat.getDrawable(holder.cardView.context, R.drawable.round_corner)
        prop.deviceProperties?.addOnStatusUpdateListener(object: OnStatusUpdateListener{
            override var listenerParent: Any? = prop

            override fun onStatusUpdate(connected: Boolean) {
                updateItemStatus(connected, prop, holder)
            }
        })
        prop.deviceProperties?.updateStatus()
    }

    fun updateItemStatus(connected: Boolean, prop: GPIOObject, holder: MyViewHolder){
        if(connected){
            holder.cardView.getChildAt(0).background = ContextCompat.getDrawable(holder.cardView.context, R.drawable.round_corner_success)
            holder.cardView.isEnabled = true

            holder.gpioSwitch.visibility = View.VISIBLE
            holder.statusLayout.visibility = View.GONE

            if(prop.pinValue == 1){
                holder.gpioSwitch.isChecked = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.iconDrawable?.setTint(Color.YELLOW)
                }
            }else{
                holder.gpioSwitch.isChecked = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.iconDrawable?.setTint(MainActivity.colorOnBackground)
                }
            }

            holder.cardView.setOnClickListener { holder.gpioSwitch.toggle() }
            holder.gpioSwitch.setOnCheckedChangeListener (object: CompoundButton.OnCheckedChangeListener{
                override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                    gpioSwitchCheckedCHangedListener(prop, p0!!, holder.iconDrawable, this)
                }
            })
        }else{
            holder.cardView.getChildAt(0).background = ContextCompat.getDrawable(holder.cardView.context, R.drawable.round_corner_error)
            holder.progressBar.visibility = View.GONE
            holder.progressImg.visibility = View.VISIBLE
            holder.progressText.text = holder.cardView.context.getString(R.string.offline)
        }
    }

    private fun gpioSwitchCheckedCHangedListener(prop: GPIOObject, compoundButton: CompoundButton, iconDrawable: Drawable?,
                                                 checkedChangedListener: CompoundButton.OnCheckedChangeListener){
        ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE){
            val success = try{
                val connector = SocketClient.Connector((MainActivity.arpTable ?: ARPTable(1)).getIpFromMac(prop.macAddr) ?: "")
                connector.sendLine("{\"request\":\"gpio_set\",\"username\":\"${prop.deviceProperties!!.userName}\", " +
                        "\"password\": \"${prop.deviceProperties!!.password}\", \"pinMode\": \"OUTPUT\", \"pinNumber\":" +
                        " ${prop.gpioNumber}, \"pinValue\": ${if(compoundButton.isChecked) 1 else 0}}")
                JSONObject(connector.readLine()).getString("response") == "success"
            }catch(ex: Exception){
                false
            }
            (compoundButton.context as Activity).runOnUiThread {
                if(!success){
                    compoundButton.setOnCheckedChangeListener { _, _ ->  }
                    compoundButton.isChecked = !compoundButton.isChecked
                    compoundButton.setOnCheckedChangeListener(checkedChangedListener)
                }
                iconDrawable?.setTint(if(compoundButton.isChecked) Color.YELLOW else MainActivity.colorOnBackground)
            }
        }
    }

    override fun getItemCount() = propList.size
}
