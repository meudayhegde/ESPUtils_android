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
import com.irware.remote.holders.DeviceProperties
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.fragments.DevicesFragment
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import kotlin.math.min

class DeviceListAdapter(private val propList: ArrayList<DeviceProperties>, private val devicesFragment: DevicesFragment) : RecyclerView.Adapter<DeviceListAdapter.MyViewHolder>(){
    
    class MyViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView)

    val context = devicesFragment.context
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        val cardView = LayoutInflater.from(parent.context).inflate(R.layout.device_list_item, parent, false) as CardView
        return MyViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val prop = propList[position]
        setViewProps(holder.cardView, prop)
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables", "InflateParams")
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
        status.text = context?.getString(R.string.connecting)
        ipText.text = ""
        cardView.setOnClickListener {  }
        Thread{
            var connected = false
            for(i: Int in 0..prop.ipAddr!!.length()){
                try {
                    val addr = prop.ipAddr!!.get(i) as String
                    if(!InetAddress.getByName(addr.split(":")[0]).isReachable(100)) continue
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
            (context as Activity).runOnUiThread{
                refresh.visibility = View.GONE
                icOnline.visibility = if(connected) View.VISIBLE else View.GONE
                icOffline.visibility = if(connected) View.GONE else View.VISIBLE
                status.text = context.getString(if(connected) R.string.online else R.string.offline)
                ipText.text = prop.ipAddr!!.get(0) as String
                val settingsIcon = context.getDrawable(R.drawable.ic_settings)
                settingsIcon?.setTint(MainActivity.colorOnBackground)
                if (connected){
                    cardView.setOnClickListener {
                        val settingsDialog = AlertDialog.Builder(context)
                            .setPositiveButton("Done") { p0, _ -> p0.dismiss() }
                            .setTitle("Settings for ${prop.nickName}")
                            .setView(R.layout.activity_settings)
                            .setIcon(settingsIcon)
                            .create()
                        settingsDialog.show()
                        settingsDialog.window?.setBackgroundDrawableResource(R.drawable.layout_border_round_corner)
                        settingsDialog.window?.setLayout((MainActivity.size.x*0.9).toInt(), (MainActivity.size.y*0.9).toInt())


                        val viewManager = LinearLayoutManager(context)
                        val viewAdapter = SettingsAdapter(arrayListOf(
                            SettingsItem("Wireless Settings","Wi-Fi/Hotspot SSID and passwords",
                                wirelessSettingsDialog(context, prop.ipAddr!!.get(0) as String, prop.userName, prop.password), R.drawable.icon_wifi),
                            SettingsItem("User Settings","User credentials (username and password)",
                                userSettingsDialog(context, prop.ipAddr!!.get(0) as String), R.drawable.ic_user)
                        ))
                        settingsDialog.findViewById<RecyclerView>(R.id.settings_list)?.apply {
                            setHasFixedSize(true)
                            layoutManager = viewManager
                            adapter = viewAdapter
                        }
                    }

                    cardView.setOnLongClickListener {
                        val content = LayoutInflater.from(context).inflate(R.layout.add_new_device,null) as ScrollView
                        val btnCancel = content.findViewById<Button>(R.id.cancel)

                        val dialog = Dialog(context)
                        dialog.setContentView(content)
                        btnCancel.setOnClickListener { dialog.dismiss() }
                        dialog.show()
                        dialog.findViewById<TextView>(R.id.title_add_new_device).text = context.getString(
                                                    R.string.set_dev_props)
                        val width = min(MainActivity.size.x,MainActivity.size.y)
                        dialog.window?.setLayout(width*7/8, WindowManager.LayoutParams.WRAP_CONTENT)
                        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        devicesFragment.onAddressVerified(dialog, prop.ipAddr?.get(0) as String, prop.macAddr!!)
                        true
                    }
                }else {
                    cardView.setOnClickListener{
                        Toast.makeText(context, "Device Offline!", Toast.LENGTH_SHORT).show()
                    }
                    cardView.setOnLongClickListener {
                        Toast.makeText(context, "Device Offline!", Toast.LENGTH_SHORT).show()
                        true
                    }
                }
            }

        }.start()
    }

    override fun getItemCount() = propList.size

    @SuppressLint("InflateParams")
    private fun userSettingsDialog(context: Context, address: String):AlertDialog{
        val content = LayoutInflater.from(context).inflate(R.layout.user_settings,null) as LinearLayout
        val cUname = content.findViewById<TextInputEditText>(R.id.cur_user_name)
        val cPass = content.findViewById<TextInputEditText>(R.id.cur_user_passwd)
        val nUname = content.findViewById<TextInputEditText>(R.id.til_user_name)
        val nPass = content.findViewById<TextInputEditText>(R.id.til_user_passwd)
        val nPassCon = content.findViewById<TextInputEditText>(R.id.til_user_confirm_passwd)

        val dialog = AlertDialog.Builder(context)
            .setTitle("User Settings")
            .setView(content)
            .setIcon(R.drawable.ic_user)
            .setNegativeButton("Cancel"){dialog,_ -> dialog.dismiss()}
            .setPositiveButton(context.getString(R.string.apply)){_,_->}
            .create()

        for(item in listOf<TextInputEditText>(cUname,cPass,nUname,nPass,nPassCon)){
            item.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    (item.parent.parent as TextInputLayout).error = null
                }
            })
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.layout_border_round_corner)
            dialog.window?.setLayout((MainActivity.size.x*0.8).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                var hasError = false
                for( tiet in listOf<TextInputEditText>(cUname,cPass,nUname,nPass,nPassCon)){
                    if (tiet.text.isNullOrEmpty()) {
                        (tiet.parent.parent as TextInputLayout).error = context.getString(R.string.empty_field)
                        hasError = true
                    }
                }
                if(nPassCon.text != nPass.text){
                    (nPassCon.parent.parent as TextInputLayout).error = context.getString(R.string.passwd_mismatch)
                    hasError = true
                }
                if(!hasError){
                    AlertDialog.Builder(context)
                        .setTitle("Confirm")
                        .setMessage("Wrong settings may result in inaccessibility of iRWaRE device (full reset will be required to recover))."
                                +"Make Sure UserName and password are correct")
                        .setNegativeButton("Cancel"){dg,_->dg.dismiss()}
                        .setPositiveButton("Confirm"){ _, _->
                            dialog.dismiss()
                            Thread {
                                try {
                                    val connector = SocketClient.Connector(address)
                                    connector.sendLine("{\"request\":\"set_user\",\"username\":\""
                                            + cUname.text.toString() + "\",\"password\":\""
                                            + cPass.text.toString() + "\",\"new_username\":\""
                                            + nUname.text.toString()+"\",\"new_password\":\""
                                            + nPass.text.toString() + "\"}"
                                    )
                                    val result = connector.readLine()
                                    val resultObj = JSONObject(result)
                                    (context as Activity).runOnUiThread{
                                        AlertDialog.Builder(context)
                                            .setTitle(if(resultObj.getString("response").contains("success",true)) "Success" else "Failed")
                                            .setMessage(resultObj.getString("response"))
                                            .setPositiveButton("Done"){dg,_->dg.dismiss()}
                                            .show()
                                    }
                                    connector.close()
                                }catch(ex:Exception){
                                    (context as Activity).runOnUiThread{
                                        AlertDialog.Builder(context)
                                            .setTitle("Failed")
                                            .setMessage("Failed to apply user settings\n$ex")
                                            .setPositiveButton("Close"){dg,_->dg.dismiss()}
                                            .show()
                                    }
                                }
                            }.start()
                        }
                        .show()
                }
            }
        }

        return dialog
    }

    @SuppressLint("InflateParams")
    private fun wirelessSettingsDialog(context: Context, address: String, userName: String?, password: String?):AlertDialog{
        val content = LayoutInflater.from(context).inflate(R.layout.wireless_settings,null) as LinearLayout
        val ssid = content.findViewById<TextInputEditText>(R.id.til_wifi_name)
        val pass = content.findViewById<TextInputEditText>(R.id.til_wifi_passwd)
        val progressBar = content.findViewById<ProgressBar>(R.id.get_wireless_loading)
        val spinner = content.findViewById<Spinner>(R.id.spinner_wireless_mode)
        var wirelessData: JSONObject? = null
        spinner.adapter = ArrayAdapter(context,android.R.layout.simple_list_item_1, arrayListOf("Station (WiFi)","Access Point (Hotspot)"))
        var mode = if(spinner.selectedItemPosition == 0) "WIFI" else "AP"
        spinner.onItemSelectedListener = object:AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                mode = if(position ==0 ) "WIFI" else "AP"
                ssid.setText(wirelessData?.optString(when(mode){"WIFI" -> "station" else -> "ap"} + "_ssid"))
                pass.setText(wirelessData?.optString(when(mode){"WIFI" -> "station" else -> "ap"} + "_psk"))
                (ssid.parent.parent as TextInputLayout).hint = "$mode Name"
                (pass.parent.parent as TextInputLayout).hint = "$mode Password"
            }
        }

        for(item in listOf<TextInputEditText>(ssid,pass)){
            item.addTextChangedListener(object:TextWatcher{
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    (item.parent.parent as TextInputLayout).error = null
                    wirelessData?.put(when(mode){"WIFI"->"station" else->"ap"} + "_" + when(item.id){R.id.til_wifi_name -> "ssid" else -> "psk"},s)
                }
            })
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("Wireless Settings")
            .setView(content)
            .setNegativeButton("Cancel"){dialog,_ -> dialog.dismiss()}
            .setPositiveButton(context.getString(R.string.apply)){_,_->}
            .setIcon(R.drawable.icon_wifi)
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.layout_border_round_corner)
            dialog.window?.setLayout((MainActivity.size.x*0.8).toInt(),WindowManager.LayoutParams.WRAP_CONTENT)
            progressBar.visibility = View.VISIBLE
            getWirelessSettings(context, address, userName, password, object:OnSocketReadListener{
                override fun onSocketRead(data: JSONObject) {
                    progressBar.visibility = View.GONE
                    val wirelessMode = data.optString("wireless_mode")
                    if(!wirelessMode.isNullOrEmpty()) {
                        wirelessData = data
                        mode = wirelessMode
                        spinner.setSelection(when(mode){"WIFI"->0 else->1 },true)
                        ssid.setText(wirelessData?.optString(when(mode){"WIFI"->"station" else->"ap" } + "_ssid"))
                        pass.setText(wirelessData?.optString(when(mode){"WIFI"->"station" else->"ap" } + "_psk"))
                    }
                }
            })
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

            positiveButton.setOnClickListener {
                if(ssid.text.isNullOrEmpty())
                    (ssid.parent.parent as TextInputLayout).error = context.getString(R.string.empty_ssid)
                if(pass.text?.length?:8 < 8)
                    (pass.parent.parent as TextInputLayout).error = context.getString(R.string.empty_password)
                if(ssid.text!!.isNotEmpty() and (pass.text?.length?:0 >= 8)){
                    AlertDialog.Builder(context)
                        .setTitle("Confirm")
                        .setMessage("Wrong settings may result in inaccessibility of iRWaRE device (full reset will be required to recover))."
                                +"\nMake Sure All SSID and password are correct")
                        .setNegativeButton("Cancel"){dg,_->dg.dismiss()}
                        .setPositiveButton("Confirm"){ _, _->
                            dialog.dismiss()
                            Thread {
                                try {
                                    val connector = SocketClient.Connector(address)
                                    connector.sendLine("{\"request\":\"set_wireless\",\"username\":\""
                                            + MainActivity.USERNAME + "\",\"password\":\""
                                            + MainActivity.PASSWORD + "\",\"wireless_mode\":\""+mode+"\",\"new_ssid\":\""
                                            + ssid.text.toString()+"\",\"new_pass\":\""
                                            +pass.text.toString() + "\"}"
                                    )
                                    val result = connector.readLine()
                                    val resultObj = JSONObject(result)
                                    (context as Activity).runOnUiThread{
                                        AlertDialog.Builder(context)
                                            .setTitle(if(resultObj.getString("response").contains("success",true)) "Success" else "Failed")
                                            .setMessage(resultObj.getString("response"))
                                            .setPositiveButton("Done"){dg,_->dg.dismiss()}
                                            .show()
                                    }
                                    connector.close()
                                }catch(ex:Exception){
                                    (context as Activity).runOnUiThread{
                                        AlertDialog.Builder(context)
                                            .setTitle("Failed")
                                            .setMessage("Failed to apply wireless settings\n$ex")
                                            .setPositiveButton("Close"){dg,_->dg.dismiss()}
                                            .show()
                                    }
                                }
                            }.start()

                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }

        return dialog
    }

    private fun getWirelessSettings(context: Context, address: String, userName: String?, password: String?, onReadWiFiConfig: OnSocketReadListener){
        Thread{
            try {
                val connector = SocketClient.Connector(address)
                connector.sendLine("{\"request\":\"get_wireless\",\"username\":\""
                        + userName + "\",\"password\":\""
                        + password + "\"}"
                )
                val result = connector.readLine()
                val resultObj = JSONObject(result)
                (context as Activity).runOnUiThread{
                    onReadWiFiConfig.onSocketRead(resultObj)
                }
                connector.close()
            }catch(ex:Exception){
                (context as Activity).runOnUiThread{
                    onReadWiFiConfig.onSocketRead(JSONObject())
                }
            }
        }.start()
    }

}

private fun JSONArray.insert(position: Int, value: Any){
    for (i in length() downTo position + 1) {
        put(i, get(i - 1))
    }
    put(position, value)
}