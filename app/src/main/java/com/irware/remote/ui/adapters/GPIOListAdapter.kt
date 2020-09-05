package com.irware.remote.ui.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.GPIOObject
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.dialogs.RemoteDialog
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import kotlin.math.min

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

        title.text = prop.title
        subTitle.text = "Device: " + (prop.deviceProperties?.nickName?: "Unknown") + "\n" + prop.subTitle
        val iconDrawable = cardView.context.getDrawable(R.drawable.icon_lamp)
        iconDrawable?.setTint(MainActivity.colorOnBackground)
        gpioIcon.setImageDrawable(iconDrawable)

        cardView.isEnabled = false
        Thread{
            try{
                val connector = SocketClient.Connector(prop.deviceProperties!!.ipAddr!!.getString(0))
                connector.sendLine("{\"request\":\"gpio_get\",\"username\":\"${prop.deviceProperties!!.userName}\", " +
                        "\"password\": \"${prop.deviceProperties!!.password}\", \"pinNumber\": ${prop.gpioNumber}}")
                val response = connector.readLine()
                val statusJson = JSONObject(response)
                (cardView.context as Activity).runOnUiThread {
                    cardView.isEnabled = true
                    if(statusJson.optString("pinMode","") == "OUTPUT"){
                        if(statusJson.getInt("pinValue") == 1){
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
                }
            }catch(ex: Exception){}
        }.start()
    }

    private fun gpioSwitchCheckedCHangedListener(prop: GPIOObject, compoundButton: CompoundButton, iconDrawable: Drawable?,
                                                 checkedChangedListener: CompoundButton.OnCheckedChangeListener){
        Thread{
            var success = false
            success = try{
                val connector = SocketClient.Connector(prop.deviceProperties!!.ipAddr!!.getString(0))
                connector.sendLine("{\"request\":\"gpio_set\",\"username\":\"${prop.deviceProperties!!.userName}\", " +
                        "\"password\": \"${prop.deviceProperties!!.password}\", \"pinMode\": \"OUTPUT\", \"pinNumber\":" +
                        " ${prop.gpioNumber}, \"pinValue\": ${if(compoundButton.isChecked) 1 else 0}}")
                val response = connector.readLine()
                val statusJson = JSONObject(response)
                statusJson.getString("response") == "success"
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