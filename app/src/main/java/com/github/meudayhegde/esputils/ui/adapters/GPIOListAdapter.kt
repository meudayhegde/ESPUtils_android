package com.github.meudayhegde.esputils.ui.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.meudayhegde.ThreadHandler
import com.github.meudayhegde.esputils.ESPUtilsApp
import com.github.meudayhegde.esputils.MainActivity
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.databinding.GpioListItemBinding
import com.github.meudayhegde.esputils.holders.GPIOObject
import com.github.meudayhegde.esputils.listeners.OnGPIORefreshListener
import com.github.meudayhegde.esputils.net.SocketClient
import com.github.meudayhegde.esputils.ui.fragments.GPIOControllerFragment
import org.json.JSONObject

class GPIOListAdapter(
    private val gpioList: ArrayList<GPIOObject>, private val fragment: GPIOControllerFragment
) : RecyclerView.Adapter<GPIOListAdapter.GPIOListViewHolder>() {

    class GPIOListViewHolder(val viewBinding: GpioListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        val context: Context = viewBinding.root.context
        val title = viewBinding.gpioName
        val subTitle = viewBinding.gpioDescription
        val gpioSwitch = viewBinding.gpioSwitch
        val statusLayout = viewBinding.gpioIntermediate
        val progressBar = viewBinding.progressStatus
        val progressImg = viewBinding.imgOffline
        val progressText = viewBinding.statusText
        val iconDrawable = ContextCompat.getDrawable(context, R.drawable.icon_lamp)

        init {
            viewBinding.icGpioListItem.setImageDrawable(iconDrawable)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GPIOListViewHolder {
        return GPIOListViewHolder(
            GpioListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: GPIOListViewHolder, position: Int) {
        setViews(holder, gpioList[position])
    }

    private fun setViews(holder: GPIOListViewHolder, gpioObject: GPIOObject) {
        gpioObject.onGPIORefreshListener = object : OnGPIORefreshListener {
            override fun onRefreshBegin() {
                itemStatusRefreshing(holder, gpioObject)
            }

            override fun onRefresh(pinValue: Int) {
                if (pinValue == -1) itemStatusOffline(holder, gpioObject)
                else itemStatusOnline(holder, gpioObject)
            }
        }
        if (gpioObject.deviceProperties.isConnected) itemStatusOnline(holder, gpioObject)
        else itemStatusOffline(holder, gpioObject)

        holder.gpioSwitch.setOnCheckedChangeListener(object :
            CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                gpioSwitchCheckedCHangedListener(gpioObject, p0!!, holder.iconDrawable, this)
            }
        })

        holder.viewBinding.root.setOnLongClickListener {
            fragment.gpioDialog(gpioObject)
            true
        }
    }

    private fun itemStatusOnline(holder: GPIOListViewHolder, gpioObject: GPIOObject) {
        itemStatusAll(holder, gpioObject)
        holder.viewBinding.root.setOnClickListener { holder.gpioSwitch.toggle() }
        holder.viewBinding.container.background =
            ContextCompat.getDrawable(holder.context, R.drawable.round_corner_success)
        holder.gpioSwitch.visibility = View.VISIBLE
        holder.statusLayout.visibility = View.GONE
        if (gpioObject.pinValue == 1) {
            holder.gpioSwitch.isChecked = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.iconDrawable?.setTint(Color.YELLOW)
            }
        } else {
            holder.gpioSwitch.isChecked = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.iconDrawable?.setTint(MainActivity.colorOnBackground)
            }
        }
    }

    private fun itemStatusOffline(holder: GPIOListViewHolder, gpioObject: GPIOObject) {
        itemStatusAll(holder, gpioObject)
        holder.statusLayout.visibility = View.VISIBLE
        holder.gpioSwitch.visibility = View.GONE

        holder.viewBinding.container.background =
            ContextCompat.getDrawable(holder.context, R.drawable.round_corner_error)
        holder.progressBar.visibility = View.GONE
        holder.progressImg.visibility = View.VISIBLE
        holder.progressText.text = holder.context.getString(R.string.offline)
        holder.viewBinding.root.setOnClickListener {
            Toast.makeText(holder.context, R.string.message_device_offline, Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun itemStatusRefreshing(holder: GPIOListViewHolder, gpioObject: GPIOObject) {
        itemStatusAll(holder, gpioObject)
        holder.gpioSwitch.visibility = View.GONE
        holder.statusLayout.visibility = View.VISIBLE
        holder.progressBar.visibility = View.VISIBLE
        holder.progressImg.visibility = View.GONE
        holder.progressText.text = holder.context.getString(R.string.loading)
        holder.viewBinding.container.background =
            ContextCompat.getDrawable(holder.context, R.drawable.layout_border_round_corner)
        holder.viewBinding.root.setOnClickListener {
            Toast.makeText(holder.context, R.string.scanning, Toast.LENGTH_SHORT).show()
        }
    }

    private fun itemStatusAll(holder: GPIOListViewHolder, gpioObject: GPIOObject) {
        holder.title.text = gpioObject.title
        holder.subTitle.text = ESPUtilsApp.getString(
            R.string.gpio_list_item_subtitle,
            gpioObject.deviceProperties.nickName,
            gpioObject.subTitle
        )
        holder.iconDrawable?.setTint(MainActivity.colorOnBackground)
    }

    private fun gpioSwitchCheckedCHangedListener(
        prop: GPIOObject,
        compoundButton: CompoundButton,
        iconDrawable: Drawable?,
        checkedChangedListener: CompoundButton.OnCheckedChangeListener
    ) {
        ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
            val success = try {
                val connector = SocketClient.Connector(prop.deviceProperties.ipAddress)
                connector.sendLine(
                    Strings.espCommandSetGpio(
                        prop.deviceProperties.userName,
                        prop.deviceProperties.password,
                        prop.gpioNumber,
                        if (compoundButton.isChecked) 1 else 0
                    )
                )
                JSONObject(connector.readLine()).getString("response") == "success"
            } catch (ex: Exception) {
                false
            }
            Handler(Looper.getMainLooper()).post {
                if (!success) {
                    compoundButton.setOnCheckedChangeListener { _, _ -> }
                    compoundButton.isChecked = !compoundButton.isChecked
                    compoundButton.setOnCheckedChangeListener(checkedChangedListener)
                }
                iconDrawable?.setTint(if (compoundButton.isChecked) Color.YELLOW else MainActivity.colorOnBackground)
            }
        }
    }

    override fun getItemCount() = gpioList.size
}
