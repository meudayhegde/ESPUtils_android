package com.irware.remote.ui.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.net.ARPTable
import com.irware.remote.holders.GPIOObject
import com.irware.remote.holders.OnStatusUpdateListener
import com.irware.remote.net.SocketClient
import org.json.JSONObject

class GPIOListAdapter(private val propList: ArrayList<GPIOObject>) : RecyclerView.Adapter<GPIOListAdapter.MyViewHolder>(){

    class MyViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        val cardView = LayoutInflater.from(parent.context).inflate(R.layout.gpio_list_item, parent, false) as CardView
        return MyViewHolder(cardView)
    }

    @SuppressLint("InflateParams")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val prop = propList[position]
        setViewProps(holder.cardView, prop)
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun setViewProps(cardView: CardView, prop: GPIOObject){
        val title = cardView.findViewById<TextView>(R.id.gpio_name)
        val subTitle = cardView.findViewById<TextView>(R.id.gpio_description)
        val gpioSwitch = cardView.findViewById<SwitchCompat>(R.id.gpio_switch)
        val gpioIcon = cardView.findViewById<ImageView>(R.id.ic_gpio_list_item)
        val statusLayout = cardView.findViewById<LinearLayout>(R.id.gpio_intermediate)
        val progressBar = cardView.findViewById<ProgressBar>(R.id.progress_status)
        val progressImg = cardView.findViewById<ImageView>(R.id.img_offline)
        val progressText = cardView.findViewById<TextView>(R.id.status_text)

        title.text = prop.title
        subTitle.text = "Device: " + (prop.deviceProperties?.nickName?: "Unknown") + "\n" + prop.subTitle
        val iconDrawable = cardView.context.getDrawable(R.drawable.icon_lamp)
        iconDrawable?.setTint(MainActivity.colorOnBackground)
        gpioIcon.setImageDrawable(iconDrawable)

        cardView.isEnabled = false
        gpioSwitch.visibility = View.GONE

        statusLayout.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        progressImg.visibility = View.GONE
        progressText.text = cardView.context.getString(R.string.loading)

        Thread{
            try{
                (cardView.context as Activity).runOnUiThread {
                    cardView.isEnabled = true

                    gpioSwitch.visibility = View.VISIBLE
                    statusLayout.visibility = View.GONE


                    if(prop.pinValue == 1){
                        gpioSwitch.isChecked = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            iconDrawable?.setTint(Color.YELLOW)
                        }
                    }else{
                        gpioSwitch.isChecked = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            iconDrawable?.setTint(MainActivity.colorOnBackground)
                        }
                    }

                    cardView.setOnClickListener { gpioSwitch.toggle() }
                    gpioSwitch.setOnCheckedChangeListener (object: CompoundButton.OnCheckedChangeListener{
                        override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                            gpioSwitchCheckedCHangedListener(prop, p0!!, iconDrawable, this)
                        }
                    })

                }
            }catch(ex: Exception){
                (cardView.context as Activity).runOnUiThread {
                    progressBar.visibility = View.GONE
                    progressImg.visibility = View.VISIBLE
                    progressText.text = cardView.context.getString(R.string.offline)
                }

                prop.deviceProperties?.addOnStatusUpdateListener(object: OnStatusUpdateListener{
                    override var listenerParent: Any? = this@GPIOListAdapter.javaClass

                    override fun onStatusUpdate(connected: Boolean) {
                        notifyItemChanged(propList.indexOf(prop))
                    }
                })
                prop.deviceProperties?.updateStatus(cardView.context)
            }
        }.start()
    }

    private fun gpioSwitchCheckedCHangedListener(prop: GPIOObject, compoundButton: CompoundButton, iconDrawable: Drawable?,
                                                 checkedChangedListener: CompoundButton.OnCheckedChangeListener){
        Thread{
            val success = try{
                val connector = SocketClient.Connector((MainActivity.arpTable ?: ARPTable(compoundButton.context, 1)).getIpFromMac(prop.macAddr) ?: "")
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
        }.start()
    }

    override fun getItemCount() = propList.size
}