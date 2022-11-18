package com.irware.remote.ui.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.irware.ThreadHandler
import com.irware.remote.ESPUtilsApp
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.GPIOObject
import com.irware.remote.listeners.OnGPIORefreshListener
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.fragments.GPIOControllerFragment
import org.json.JSONObject

class GPIOListAdapter(private val gpioList: ArrayList<GPIOObject>,
                      private val fragment: GPIOControllerFragment) : RecyclerView.Adapter<GPIOListAdapter.GPIOListViewHolder>(){

    class GPIOListViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView){
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GPIOListViewHolder {
        val cardView = LayoutInflater.from(parent.context).inflate(R.layout.gpio_list_item, parent, false) as CardView
        return GPIOListViewHolder(cardView)
    }

    @SuppressLint("InflateParams")
    override fun onBindViewHolder(holder: GPIOListViewHolder, position: Int) {
        val gpioObject = gpioList[position]
        setViews(holder, gpioObject)
    }

    private fun setViews(holder: GPIOListViewHolder, gpioObject: GPIOObject){
        gpioObject.onGPIORefreshListener = object: OnGPIORefreshListener {
            override fun onRefreshBegin() {
                itemStatusRefreshing(holder, gpioObject)
            }

            override fun onRefresh(pinValue: Int) {
                if(pinValue == -1) itemStatusOffline(holder, gpioObject)
                else itemStatusOnline(holder, gpioObject)
            }
        }
        if(gpioObject.deviceProperties.isConnected) itemStatusOnline(holder, gpioObject)
        else itemStatusOffline(holder, gpioObject)

        holder.gpioSwitch.setOnCheckedChangeListener (object: CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                gpioSwitchCheckedCHangedListener(gpioObject, p0!!, holder.iconDrawable, this)
            }
        })

        holder.cardView.setOnLongClickListener {
            fragment.gpioDialog(gpioObject)
            true
        }
    }

    private fun itemStatusOnline(holder: GPIOListViewHolder, gpioObject: GPIOObject){
        itemStatusAll(holder, gpioObject)
        holder.cardView.setOnClickListener { holder.gpioSwitch.toggle() }
        holder.cardView.getChildAt(0).background =
            ContextCompat.getDrawable(holder.cardView.context, R.drawable.round_corner_success)
        holder.gpioSwitch.visibility = View.VISIBLE
        holder.statusLayout.visibility = View.GONE
        if(gpioObject.pinValue == 1){
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
    }

    private fun itemStatusOffline(holder: GPIOListViewHolder, gpioObject: GPIOObject){
        itemStatusAll(holder, gpioObject)
        holder.statusLayout.visibility = View.VISIBLE
        holder.gpioSwitch.visibility = View.GONE

        holder.cardView.getChildAt(0).background = ContextCompat.getDrawable(holder.cardView.context, R.drawable.round_corner_error)
        holder.progressBar.visibility = View.GONE
        holder.progressImg.visibility = View.VISIBLE
        holder.progressText.text = holder.cardView.context.getString(R.string.offline)
        holder.cardView.setOnClickListener {
            Toast.makeText(holder.cardView.context, holder.cardView.context.getString(R.string.device_offline), Toast.LENGTH_SHORT).show()
        }
    }

    private fun itemStatusRefreshing(holder: GPIOListViewHolder, gpioObject: GPIOObject){
        itemStatusAll(holder, gpioObject)
        holder.gpioSwitch.visibility = View.GONE
        holder.statusLayout.visibility = View.VISIBLE
        holder.progressBar.visibility = View.VISIBLE
        holder.progressImg.visibility = View.GONE
        holder.progressText.text = holder.cardView.context.getString(R.string.loading)
        holder.cardView.getChildAt(0).background =
            ContextCompat.getDrawable(holder.cardView.context, R.drawable.layout_border_round_corner)
        holder.cardView.setOnClickListener {
            Toast.makeText(holder.cardView.context, holder.cardView.context.getString(R.string.scanning), Toast.LENGTH_SHORT).show()
        }
    }
    private fun itemStatusAll(holder: GPIOListViewHolder, gpioObject: GPIOObject){
        holder.title.text = gpioObject.title
        holder.subTitle.text = ESPUtilsApp.getString(
            R.string.gpio_list_item_subtitle,
            gpioObject.deviceProperties.nickName,
            gpioObject.subTitle
        )
        holder.iconDrawable?.setTint(MainActivity.colorOnBackground)
    }

    private fun gpioSwitchCheckedCHangedListener(prop: GPIOObject, compoundButton: CompoundButton, iconDrawable: Drawable?,
                                                 checkedChangedListener: CompoundButton.OnCheckedChangeListener){
        ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE){
            val success = try{
                val connector = SocketClient.Connector(prop.deviceProperties.ipAddress)
                connector.sendLine(ESPUtilsApp.getString(
                    R.string.esp_command_set_gpio,
                    prop.deviceProperties.userName,
                    prop.deviceProperties.password,
                    prop.gpioNumber,
                    if(compoundButton.isChecked) 1 else 0
                ))
                JSONObject(connector.readLine()).getString("response") == "success"
            }catch(ex: Exception){
                false
            }
            Handler(Looper.getMainLooper()).post {
                if(!success){
                    compoundButton.setOnCheckedChangeListener { _, _ ->  }
                    compoundButton.isChecked = !compoundButton.isChecked
                    compoundButton.setOnCheckedChangeListener(checkedChangedListener)
                }
                iconDrawable?.setTint(if(compoundButton.isChecked) Color.YELLOW else MainActivity.colorOnBackground)
            }
        }
    }

    override fun getItemCount() = gpioList.size
}
