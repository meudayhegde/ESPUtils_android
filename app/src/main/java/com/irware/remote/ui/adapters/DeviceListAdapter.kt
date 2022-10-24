package com.irware.remote.ui.adapters

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.irware.ThreadHandler
import com.irware.md5
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.SettingsItem
import com.irware.remote.holders.DeviceProperties
import com.irware.remote.net.ARPTable
import com.irware.remote.net.EspOta
import com.irware.remote.net.OnUpdateIntermediateListener
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.fragments.DevicesFragment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*
import java.util.zip.ZipFile


class DeviceListAdapter(private val propList: ArrayList<DeviceProperties>,
                        private val devicesFragment: DevicesFragment): RecyclerView.Adapter<DeviceListAdapter.MyViewHolder>(){
    
    class MyViewHolder(listItem: RelativeLayout) : RecyclerView.ViewHolder(listItem){
        val cardView: CardView = listItem.findViewById(R.id.device_list_item_foreground)
        val deviceNameView: TextView = cardView.findViewById(R.id.name_device)
        val deviceMacAddrView: TextView = cardView.findViewById(R.id.mac_addr)
        val deviceDescView: TextView = cardView.findViewById(R.id.device_desc)
        val icOnline: ImageView = cardView.findViewById(R.id.img_online)
        val icOffline: ImageView = cardView.findViewById(R.id.img_offline)
        val refresh: ProgressBar = cardView.findViewById(R.id.progress_status)
        val status: TextView = cardView.findViewById(R.id.status_text)
        val ipText: TextView = cardView.findViewById(R.id.ip_addr)
    }

    val context = devicesFragment.requireContext()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val listItem = LayoutInflater.from(parent.context).inflate(R.layout.device_list_item, parent, false) as RelativeLayout
        return MyViewHolder(listItem)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val prop = propList[position]
        setViewProps(holder, prop)
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables", "InflateParams")
    private fun setViewProps(holder: MyViewHolder, prop: DeviceProperties){
        holder.deviceNameView.text = prop.nickName
        holder.deviceMacAddrView.text = "(${prop.macAddress})"
        holder.deviceDescView.text = prop.description

        holder.refresh.visibility = View.VISIBLE
        holder.icOnline.visibility = View.GONE
        holder.icOffline.visibility = View.GONE
        holder.status.text = context.getString(R.string.connecting)
        holder.ipText.text = ""
        prop.getIpAddress{
            Handler(Looper.getMainLooper()).post{
                holder.refresh.visibility = View.GONE
                holder.icOnline.visibility = if (prop.isConnected) View.VISIBLE else View.GONE
                holder.icOffline.visibility = if (prop.isConnected) View.GONE else View.VISIBLE
                holder.status.text =
                    context.resources.getString(if(prop.isConnected) R.string.online else R.string.offline)
                holder.ipText.text = it?: ARPTable().getIpFromMac(prop.macAddress)

                holder.cardView.getChildAt(0).background =
                    if(prop.isConnected) context.getDrawable(R.drawable.round_corner_success)
                    else context.getDrawable(R.drawable.round_corner_error)
                MainActivity.activity?.irFragment?.notifyDataChanged()
            }
        }
        holder.cardView.setOnClickListener {
            val settingsDialog = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                .setPositiveButton(context.resources.getString(R.string.done)) { p0, _ -> p0.dismiss() }
                .setTitle("Settings for ${prop.nickName}")
                .setView(R.layout.activity_settings)
                .setIcon(R.drawable.ic_settings)
                .create()
            settingsDialog.setOnShowListener {
                val addr = prop.ipAddress
                val settingsList = arrayListOf(
                    SettingsItem("Wireless Settings", "Wi-Fi/Hotspot SSID and passwords",
                        null, R.drawable.icon_wifi, wirelessSettingsClickAction(context, prop), prop
                    ),
                    SettingsItem(
                        "User Settings", "User credentials (username and password)",
                        null, R.drawable.ic_user, userSettingsClickAction(context, prop), prop
                    ),
                    SettingsItem("Reboot", "Restart the micro controller", null,
                        R.drawable.icon_power, restartConfirmClickAction(context, prop), prop
                    ),
                    SettingsItem("Install Update", "install update on esp device",
                        null, R.drawable.ic_system_update, updateClickAction(context, prop), prop
                    ),
                    SettingsItem("Remove Device", "Remove ESP device from device list",
                        null, R.drawable.icon_delete, deleteClickAction(context, holder.adapterPosition, settingsDialog)
                    ),
                    SettingsItem("Edit Properties", "Edit device properties",
                        null, R.drawable.icon_edit, editClickAction(context, prop)
                    )
                )
                val viewManager = LinearLayoutManager(context)
                val viewAdapter = SettingsAdapter(settingsList)
                val refreshLayout = settingsDialog.findViewById<SwipeRefreshLayout>(R.id.settings_refresh_layout)
                refreshLayout?.setOnRefreshListener {
                    prop.getIpAddress{
                        Handler(Looper.getMainLooper()).post{
                            for(ind in 0 until settingsList.size){
                                if(settingsList[ind].prop != null){
                                    viewAdapter.notifyItemChanged(ind)
                                }
                            }
                            ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE){
                                Thread.sleep(100)
                                Handler(Looper.getMainLooper()).post{
                                    refreshLayout.isRefreshing = false
                                }
                            }
                        }
                    }
                }
                settingsDialog.findViewById<RecyclerView>(R.id.settings_list)?.apply {
                    setHasFixedSize(true)
                    layoutManager = viewManager
                    adapter = viewAdapter
                }
            }
            settingsDialog.show()
        }
    }

    override fun getItemCount() = propList.size

    private fun restartConfirmClickAction(context: Context, prop: DeviceProperties): Runnable{
        return Runnable{
            if(prop.isConnected) {
                AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                    .setTitle(context.resources.getString(R.string.confirm_restart))
                    .setIcon(R.drawable.icon_power)
                    .setMessage(context.resources.getString(R.string.confirm_restart_note))
                    .setNegativeButton(context.resources.getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(context.resources.getString(R.string.restart)) { dialog, _ ->
                        dialog.dismiss()
                        ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
                            try {
                                val connector = SocketClient.Connector(prop.ipAddress)
                                connector.sendLine("{\"request\":\"restart\",\"username\":\"${prop.userName}\",\"password\":\"${prop.password}\"}")
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, "Restart command successfully sent.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (ex: Exception) {
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, "Failed to send Restart command!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .show()
            }
            else Toast.makeText(context, context.resources.getString(R.string.device_offline), Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteClickAction(context: Context, position: Int, settingsDialog: AlertDialog): Runnable{
        return Runnable{
            AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                .setIcon(R.drawable.icon_delete)
                .setTitle(context.resources.getString(R.string.confirm_delete))
                .setMessage(context.resources.getString(R.string.confirm_delete_note))
                .setNegativeButton(context.resources.getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(context.resources.getString(R.string.remove)) { dialog, _ ->
                    dialog.dismiss()
                    propList[position].delete()
                    propList.removeAt(position)
                    notifyItemRemoved(position)
                    settingsDialog.dismiss()
                    Toast.makeText(context, "Device successfully removed.", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun editClickAction(context: Context, prop: DeviceProperties): Runnable{
        return Runnable{
            devicesFragment.onAddressVerified(context, prop.ipAddress, prop.macAddress)
        }
    }

    private fun extractUpdate(file: File, updateIntermediateListener: OnUpdateIntermediateListener?): File?{
        updateIntermediateListener?.onStatusUpdate("Extracting update file...", true)
        if(file.exists() and file.isFile){
            if(!file.absolutePath.endsWith(".zip")){
                updateIntermediateListener?.onError("Err: Update file is not valid")
                return null
            }

            val extractDir = File(file.parentFile?.absolutePath + File.separator + file.name.split(".")[0])
            if(extractDir.exists()) {
                if(extractDir.isFile) extractDir.delete() else extractDir.deleteRecursively()
            }
            extractDir.mkdirs()
            val path = extractDir.absolutePath

            val zip = ZipFile(file)
            val entries = zip.entries()
            while(entries.hasMoreElements()){
                val entry = entries.nextElement()
                if(entry.isDirectory) {
                    File(path + File.separator + entry.name).mkdirs()
                    continue
                }
                val outFile = File(path + File.separator + entry.name)
                outFile.createNewFile()

                outFile.outputStream().use{
                    it.write(zip.getInputStream(entry).readBytes())
                    it.flush()
                }
            }
            file.delete()
            return extractDir
        }
        updateIntermediateListener?.onError("Err: Update file does not exist.")
        return null
    }

    @SuppressLint("InflateParams")
    private fun updateClickAction(context: Context, prop: DeviceProperties): Runnable{
        return Runnable{
            if(prop.isConnected) {
                devicesFragment.updateSelectedListener = { updateFile ->
                    val updateDialog = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                        .setTitle("Esp Ota Update")
                        .setView(R.layout.esp_ota_layout)
                        .setIcon(R.drawable.ic_system_update)
                        .setNegativeButton("Cancel") { _, _ -> }
                        .setPositiveButton("Update") { _, _ -> }
                        .create()
                    updateDialog.setCancelable(false)
                    updateDialog.setOnShowListener {

                        val positiveButton = updateDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        val negativeButton = updateDialog.getButton(DialogInterface.BUTTON_NEGATIVE)

                        val messageView = updateDialog.findViewById<TextView>(R.id.progress_title)
                        val progressBar =
                            updateDialog.findViewById<ProgressBar>(R.id.update_progress_bar)

                        val errorLayout = updateDialog.findViewById<LinearLayout>(R.id.error_layout)
                        val errorView = updateDialog.findViewById<TextView>(R.id.error_content)

                        val updateIntermediateListener = object : OnUpdateIntermediateListener {
                            override fun onStatusUpdate(status: String, progress: Boolean) {
                                Handler(Looper.getMainLooper()).post {
                                    if (progress) {
                                        progressBar?.visibility = View.VISIBLE
                                        progressBar?.isIndeterminate = true
                                        negativeButton?.isEnabled = false
                                        positiveButton?.isEnabled = false
                                    } else progressBar?.visibility = View.GONE
                                    messageView?.text = status
                                    if (status.lowercase(Locale.ROOT).contains("success")) {
                                        positiveButton?.isEnabled = true
                                        positiveButton?.setOnClickListener {
                                            updateDialog.dismiss()
                                        }
                                    }
                                }
                            }

                            override fun onProgressUpdate(progress: Float) {
                                Handler(Looper.getMainLooper()).post  {
                                    negativeButton?.isEnabled = false
                                    positiveButton?.isEnabled = false
                                    errorLayout?.visibility = View.GONE

                                    if (progressBar?.visibility != View.VISIBLE) progressBar?.visibility =
                                        View.VISIBLE
                                    progressBar?.isIndeterminate = false
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                        progressBar?.setProgress((progress * 100).toInt(), true)
                                    } else {
                                        progressBar?.progress = (progress * 100).toInt()

                                    }
                                }
                            }

                            override fun onError(message: String) {
                                Handler(Looper.getMainLooper()).post  {
                                    negativeButton?.isEnabled = true
                                    negativeButton?.text = context.getString(R.string.close)
                                    positiveButton?.isEnabled = false
                                    errorLayout?.visibility = View.VISIBLE

                                    progressBar?.visibility = View.GONE
                                    errorView?.text = message
                                }
                            }
                        }
                        positiveButton?.setOnClickListener {
                            positiveButton.isEnabled = false
                            positiveButton.text = context.resources.getString(R.string.done)
                            negativeButton?.isEnabled = false
                            ThreadHandler.runOnFreeThread {
                                val updateDir =
                                    extractUpdate(updateFile, updateIntermediateListener)
                                if (updateDir != null) {
                                    if (!verifyUpdate(updateDir, updateIntermediateListener)) {
                                        updateDir.deleteRecursively()
                                    }

                                    val espOta = EspOta(prop)
                                    val system =
                                        updateDir.listFiles { _, name -> name.endsWith(".bin") }
                                            ?.find {
                                                it.name.lowercase(Locale.ROOT).startsWith("system")
                                            }
                                    system?.let {
                                        espOta.installUpdate(
                                            it,
                                            EspOta.SYSTEM,
                                            updateIntermediateListener
                                        )
                                        return@runOnFreeThread
                                    }

                                    val spiffs =
                                        updateDir.listFiles { _, name -> name.endsWith(".bin") }
                                            ?.find {
                                                it.name.lowercase(Locale.ROOT).startsWith("spiffs")
                                            }
                                    spiffs?.let {
                                        espOta.installUpdate(
                                            it,
                                            EspOta.SPIFFS,
                                            updateIntermediateListener
                                        )
                                        return@runOnFreeThread
                                    }

                                }
                            }
                        }
                    }
                    updateDialog.show()
                }
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "application/zip"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                try {
                    devicesFragment.espOtaChooser.launch(
                        Intent.createChooser(
                            intent,
                            "Select Update zip file containing firmware files."
                        )
                    )
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(
                        context, "Please install a File Manager.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else Toast.makeText(context, context.resources.getString(R.string.device_offline), Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyUpdate(updateDir: File, updateIntermediateListener: OnUpdateIntermediateListener?): Boolean{
        updateIntermediateListener?.onStatusUpdate("Verifying update file.", true)
        val hashFile = File(updateDir.absolutePath, ".hash")
        if(!hashFile.exists()){
            updateIntermediateListener?.onError("Hash does not exist, Update aborted.")
            return false
        }

        val hashOrigin = hashFile.inputStream().readBytes().toString(Charsets.UTF_8).replace("\n","")
        var hash = ""
        updateDir.listFiles{ _, name -> name.endsWith(".bin")}?.sortedArray()?.forEach {
            hash += md5(it) + md5(it.name)
        }
        val verified = md5(hash) == hashOrigin
        if(!verified) updateIntermediateListener?.onError("Verification failed, Please select a valid update for the selected device")
        Log.d(javaClass.simpleName, " Verified")
        return verified
    }

    @SuppressLint("InflateParams")
    private fun userSettingsClickAction(context: Context, prop: DeviceProperties): Runnable{
        return Runnable{
            if(prop.isConnected) {
                val userDialog = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                    .setTitle("User Settings")
                    .setView(R.layout.user_settings)
                    .setIcon(R.drawable.ic_user)
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(context.getString(R.string.apply)) { _, _ -> }
                    .create()

                userDialog.setOnShowListener {
                    val cUname = userDialog.findViewById<TextInputEditText>(R.id.cur_user_name)!!
                    val cPass = userDialog.findViewById<TextInputEditText>(R.id.cur_user_passwd)!!
                    val nUname = userDialog.findViewById<TextInputEditText>(R.id.til_user_name)!!
                    val nPass = userDialog.findViewById<TextInputEditText>(R.id.til_user_passwd)!!
                    val nPassCon =
                        userDialog.findViewById<TextInputEditText>(R.id.til_user_confirm_passwd)!!

                    for (item in listOf(cUname, cPass, nUname, nPass, nPassCon)) {
                        item.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable?) {}
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                                (item.parent.parent as TextInputLayout).error = null
                            }
                        })
                    }

                    val positiveButton = userDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    positiveButton.setOnClickListener {
                        var hasError = false
                        for (tiet in listOf(cUname, cPass, nUname, nPass, nPassCon)) {
                            if (tiet.text.isNullOrEmpty()) {
                                (tiet.parent.parent as TextInputLayout).error =
                                    context.getString(R.string.empty_field)
                                hasError = true
                            }
                        }
                        if (nPassCon.text != nPass.text) {
                            (nPassCon.parent.parent as TextInputLayout).error =
                                context.getString(R.string.passwd_mismatch)
                            hasError = true
                        }
                        if (!hasError) {
                            AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                                .setTitle("Confirm")
                                .setMessage(
                                    "Wrong settings may result in inaccessibility of iRWaRE device (full reset will be required to recover))."
                                            + "Make Sure UserName and password are correct"
                                )
                                .setNegativeButton("Cancel") { dg, _ -> dg.dismiss() }
                                .setPositiveButton("Confirm") { _, _ ->
                                    userDialog.dismiss()
                                    ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
                                        try {
                                            val connector = SocketClient.Connector(prop.ipAddress)
                                            connector.sendLine(
                                                "{\"request\":\"set_user\",\"username\":\""
                                                        + cUname.text.toString() + "\",\"password\":\""
                                                        + cPass.text.toString() + "\",\"new_username\":\""
                                                        + nUname.text.toString() + "\",\"new_password\":\""
                                                        + nPass.text.toString() + "\"}"
                                            )
                                            val result = connector.readLine()
                                            val resultObj = JSONObject(result)
                                            Handler(Looper.getMainLooper()).post {
                                                AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                                                    .setTitle(
                                                        if (resultObj.getString("response")
                                                                .contains(
                                                                    "success",
                                                                    true
                                                                )
                                                        ) "Success" else "Failed"
                                                    )
                                                    .setMessage(resultObj.getString("response"))
                                                    .setPositiveButton("Done") { dg, _ -> dg.dismiss() }
                                                    .show()
                                            }
                                            connector.close()
                                        } catch (ex: Exception) {
                                            Handler(Looper.getMainLooper()).post {
                                                AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                                                    .setTitle("Failed")
                                                    .setMessage("Failed to apply user settings\n$ex")
                                                    .setPositiveButton("Close") { dg, _ -> dg.dismiss() }
                                                    .show()
                                            }
                                        }
                                    }
                                }
                                .show()
                        }
                    }
                }
                userDialog.show()
            }
            else Toast.makeText(context, context.resources.getString(R.string.device_offline), Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("InflateParams")
    private fun wirelessSettingsClickAction(context: Context, prop: DeviceProperties): Runnable{
        return Runnable {
            if(prop.isConnected) {
                val dialog = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                    .setTitle("Wireless Settings")
                    .setView(R.layout.wireless_settings)
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(context.getString(R.string.apply)) { _, _ -> }
                    .setIcon(R.drawable.icon_wifi)
                    .create()
                dialog.setOnShowListener {
                    val ssid = dialog.findViewById<TextInputEditText>(R.id.til_wifi_name)!!
                    val pass = dialog.findViewById<TextInputEditText>(R.id.til_wifi_passwd)!!
                    val progressBar = dialog.findViewById<ProgressBar>(R.id.get_wireless_loading)!!
                    val spinner = dialog.findViewById<Spinner>(R.id.spinner_wireless_mode)!!
                    var wirelessData: JSONObject? = null
                    spinner.adapter = ArrayAdapter(
                        context, android.R.layout.simple_list_item_1, arrayListOf(
                            "Station (WiFi)",
                            "Access Point (Hotspot)"
                        )
                    )
                    var mode = if (spinner.selectedItemPosition == 0) "WIFI" else "AP"
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            mode = if (position == 0) "WIFI" else "AP"
                            ssid.setText(
                                wirelessData?.optString(
                                    when (mode) {
                                        "WIFI" -> "station"
                                        else -> "ap"
                                    } + "_ssid"
                                )
                            )
                            pass.setText(
                                wirelessData?.optString(
                                    when (mode) {
                                        "WIFI" -> "station"
                                        else -> "ap"
                                    } + "_psk"
                                )
                            )
                            (ssid.parent?.parent as TextInputLayout).hint = "$mode Name"
                            (pass.parent?.parent as TextInputLayout).hint = "$mode Password"
                        }
                    }

                    for (item in listOf(ssid, pass)) {
                        item.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable?) {}
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                                (item.parent.parent as TextInputLayout).error = null
                                wirelessData?.put(
                                    when (mode) {
                                        "WIFI" -> "station"
                                        else -> "ap"
                                    } + "_" + when (item.id) {
                                        R.id.til_wifi_name -> "ssid"
                                        else -> "psk"
                                    }, s
                                )
                            }
                        })
                    }

                    progressBar.visibility = View.VISIBLE
                    getWirelessSettings(prop.ipAddress, prop.userName, prop.password) { data ->
                        progressBar.visibility = View.GONE
                        val wirelessMode = data.optString("wireless_mode")
                        if (!wirelessMode.isNullOrEmpty()) {
                            wirelessData = data
                            mode = wirelessMode
                            spinner.setSelection(
                                when (mode) {
                                    "WIFI" -> 0
                                    else -> 1
                                }, true
                            )
                            ssid.setText(
                                wirelessData?.optString(
                                    when (mode) {
                                        "WIFI" -> "station"
                                        else -> "ap"
                                    } + "_ssid"
                                )
                            )
                            pass.setText(
                                wirelessData?.optString(
                                    when (mode) {
                                        "WIFI" -> "station"
                                        else -> "ap"
                                    } + "_psk"
                                )
                            )
                        }
                    }
                    val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

                    positiveButton.setOnClickListener {
                        if (ssid.text.isNullOrEmpty())
                            (ssid.parent.parent as TextInputLayout).error =
                                context.getString(R.string.empty_ssid)
                        if ((pass.text?.length ?: 8) < 8)
                            (pass.parent.parent as TextInputLayout).error =
                                context.getString(R.string.empty_password)
                        if (ssid.text!!.isNotEmpty() and ((pass.text?.length ?: 0) >= 8)) {
                            AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                                .setTitle("Confirm")
                                .setMessage(
                                    "Wrong settings may result in inaccessibility of iRWaRE device (full reset will be required to recover))."
                                            + "\nMake Sure SSID and password are correct"
                                )
                                .setNegativeButton("Cancel") { dg, _ -> dg.dismiss() }
                                .setPositiveButton("Confirm") { _, _ ->
                                    dialog.dismiss()
                                    ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
                                        try {
                                            val connector = SocketClient.Connector(prop.ipAddress)
                                            connector.sendLine(
                                                "{\"request\":\"set_wireless\",\"username\":\"${prop.userName}\",\"password\":\""
                                                        + "${prop.password}\",\"wireless_mode\":\"" + mode + "\",\"new_ssid\":\""
                                                        + "${ssid.text.toString()}\",\"new_pass\":\"${pass.text.toString()}\"}"
                                            )
                                            val result = connector.readLine()
                                            val resultObj = JSONObject(result)
                                            Handler(Looper.getMainLooper()).post {
                                                AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                                                    .setTitle(
                                                        if (resultObj.getString("response")
                                                                .contains(
                                                                    "success",
                                                                    true
                                                                )
                                                        ) "Success" else "Failed"
                                                    )
                                                    .setMessage(resultObj.getString("response"))
                                                    .setPositiveButton("Done") { dg, _ -> dg.dismiss() }
                                                    .show()
                                            }
                                            connector.close()
                                        } catch (ex: Exception) {
                                            Handler(Looper.getMainLooper()).post {
                                                AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                                                    .setTitle("Failed")
                                                    .setMessage("Failed to apply wireless settings\n$ex")
                                                    .setPositiveButton("Close") { dg, _ -> dg.dismiss() }
                                                    .show()
                                            }
                                        }
                                    }

                                    dialog.dismiss()
                                }
                                .show()
                        }
                    }
                }
                dialog.show()
            }
            else Toast.makeText(context, context.resources.getString(R.string.device_offline), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getWirelessSettings(address: String, userName: String?, password: String?, onReadWiFiConfig: ((data: JSONObject) -> Unit)){
        ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE){
            try {
                val connector = SocketClient.Connector(address)
                connector.sendLine(
                    "{\"request\":\"get_wireless\",\"username\":\""
                            + userName + "\",\"password\":\""
                            + password + "\"}"
                )
                val result = connector.readLine()
                val resultObj = JSONObject(result)
                Handler(Looper.getMainLooper()).post{
                    onReadWiFiConfig.invoke(resultObj)
                }
                connector.close()
            }catch (ex: Exception){
                Handler(Looper.getMainLooper()).post{
                    onReadWiFiConfig.invoke(JSONObject())
                }
            }
        }
    }
}

fun JSONArray.insert(position: Int, value: Any){
    if(length() > 0) for (i in length() downTo position + 1) {
        put(i, get(i - 1))
    }
    put(position, value)
}