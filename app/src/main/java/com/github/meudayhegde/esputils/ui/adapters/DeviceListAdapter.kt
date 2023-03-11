package com.github.meudayhegde.esputils.ui.adapters

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.meudayhegde.ThreadHandler
import com.github.meudayhegde.Utils
import com.github.meudayhegde.esputils.ESPUtilsApp
import com.github.meudayhegde.esputils.MainActivity
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.databinding.DeviceListItemBinding
import com.github.meudayhegde.esputils.databinding.EspOtaLayoutBinding
import com.github.meudayhegde.esputils.databinding.RecyclerRefreshLayoutBinding
import com.github.meudayhegde.esputils.databinding.UserSettingsBinding
import com.github.meudayhegde.esputils.databinding.WirelessSettingsBinding
import com.github.meudayhegde.esputils.holders.DeviceProperties
import com.github.meudayhegde.esputils.holders.SettingsItem
import com.github.meudayhegde.esputils.listeners.OnDeviceStatusListener
import com.github.meudayhegde.esputils.listeners.OnOTAIntermediateListener
import com.github.meudayhegde.esputils.net.EspOta
import com.github.meudayhegde.esputils.net.SocketClient
import com.github.meudayhegde.esputils.ui.fragments.DevicesFragment
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject
import java.io.File
import java.util.*
import java.util.zip.ZipException
import java.util.zip.ZipFile


class DeviceListAdapter(private val propList: ArrayList<DeviceProperties>,
                        private val devicesFragment: DevicesFragment): RecyclerView.Adapter<DeviceListAdapter.DeviceListViewHolder>(){
    
    class DeviceListViewHolder(val binding: DeviceListItemBinding) : RecyclerView.ViewHolder(binding.root){
        val context: Context = binding.root.context
        val deviceNameView = binding.nameDevice
        val macAddressView = binding.macAddr
        val deviceDescView = binding.deviceDesc
        val icOnline = binding.imgOnline
        val icOffline = binding.imgOffline
        val refresh = binding.progressStatus
        val status = binding.statusText
        val ipText = binding.ipAddr
    }

    val context = devicesFragment.requireContext()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceListViewHolder {
        return DeviceListViewHolder(
            DeviceListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: DeviceListViewHolder, position: Int) {
        val prop = propList[position]
        setViewProps(holder, prop)
    }

    private fun setStateAll(holder: DeviceListViewHolder, prop: DeviceProperties){
        holder.deviceNameView.text = prop.nickName
        holder.macAddressView.text = context.getString(R.string.parentheses, prop.macAddress)
        holder.deviceDescView.text = prop.description
    }

    private fun setStateRefreshing(holder: DeviceListViewHolder, prop: DeviceProperties){
        setStateAll(holder, prop)
        holder.refresh.visibility = View.VISIBLE
        holder.icOnline.visibility = View.GONE
        holder.icOffline.visibility = View.GONE
        holder.status.text = context.getString(R.string.connecting)
        holder.binding.container.background =
            AppCompatResources.getDrawable(context, R.drawable.layout_border_round_corner)
        holder.ipText.text = ""
    }

    private fun setStateOnline(holder: DeviceListViewHolder, prop: DeviceProperties){
        setStateAll(holder, prop)
        holder.refresh.visibility = View.GONE
        holder.icOnline.visibility = View.VISIBLE
        holder.icOffline.visibility = View.GONE
        holder.status.text = context.resources.getString(R.string.online)
        holder.ipText.text = prop.ipAddress
        holder.binding.container.background =
            AppCompatResources.getDrawable(context, R.drawable.round_corner_success)
    }

    private fun setStateOffline(holder: DeviceListViewHolder, prop: DeviceProperties){
        setStateAll(holder, prop)
        holder.refresh.visibility = View.GONE
        holder.icOnline.visibility = View.GONE
        holder.icOffline.visibility = View.VISIBLE
        holder.status.text = context.resources.getString(R.string.offline)
        holder.ipText.text = prop.ipAddress
        holder.binding.container.background =
            AppCompatResources.getDrawable(context, R.drawable.round_corner_error)
    }

    private fun setViewProps(holder: DeviceListViewHolder, prop: DeviceProperties){
        prop.onDeviceStatusListener = object: OnDeviceStatusListener {
            override fun onBeginRefresh() {
                setStateRefreshing(holder, prop)
            }

            override fun onStatusUpdate(connected: Boolean) {
                if(connected) setStateOnline(holder, prop) else setStateOffline(holder, prop)
                MainActivity.activity?.irFragment?.notifyDataChanged()
            }
        }
        if(prop.isConnected) setStateOnline(holder, prop) else setStateOffline(holder, prop)

        holder.binding.root.setOnClickListener {
            val dialogBinding = RecyclerRefreshLayoutBinding.inflate(LayoutInflater.from(holder.context))
            val settingsDialog = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                .setPositiveButton(context.resources.getString(R.string.done)) { p0, _ -> p0.dismiss() }
                .setTitle(context.getString(R.string.settings_dialog_subtitle, prop.nickName))
                .setView(dialogBinding.root)
                .setIcon(R.drawable.ic_settings)
                .create()
            settingsDialog.setOnShowListener {
                val settingsList = arrayListOf(
                    SettingsItem(context.getString(R.string.title_wireless_settings), context.getString(R.string.title_sub_wireless_settings),
                        null, R.drawable.icon_wifi, wirelessSettingsClickAction(context, prop), prop
                    ),
                    SettingsItem(
                        context.getString(R.string.title_user_settings), context.getString(R.string.title_sub_user_settings),
                        null, R.drawable.ic_user, userSettingsClickAction(context, prop), prop
                    ),
                    SettingsItem(context.getString(R.string.reboot), context.getString(R.string.title_sub_reboot), null,
                        R.drawable.icon_power, restartConfirmClickAction(context, prop), prop
                    ),
                    SettingsItem(context.getString(R.string.title_install_update), context.getString(R.string.title_sub_install_update),
                        null, R.drawable.ic_system_update, updateClickAction(context, prop), prop
                    ),
                    SettingsItem(context.getString(R.string.title_edit_properties), context.getString(R.string.title_sub_edit_properties),
                        null, R.drawable.icon_edit, editClickAction(context, prop)
                    ),
                    SettingsItem(context.getString(R.string.title_remove_device), context.getString(R.string.title_sub_remove_device),
                        null, R.drawable.icon_delete, deleteClickAction(context, holder.adapterPosition, settingsDialog)
                    ),
                    SettingsItem(context.getString(R.string.title_reset_device), context.getString(R.string.title_sub_reset_device),
                        null, R.drawable.ic_reset, resetClickAction(context, prop), prop
                    )
                )
                val viewManager = LinearLayoutManager(context)
                val viewAdapter = SettingsAdapter(settingsList)
                dialogBinding.refreshLayout.setOnRefreshListener {
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
                                    dialogBinding.refreshLayout.isRefreshing = false
                                }
                            }
                        }
                    }
                }
                dialogBinding.refreshLayoutRecyclerView.apply {
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
                    .setMessage(context.resources.getString(R.string.message_confirm_restart_note))
                    .setNegativeButton(context.resources.getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(context.resources.getString(R.string.restart)) { dialog, _ ->
                        dialog.dismiss()
                        ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
                            try {
                                val connector = SocketClient.Connector(prop.ipAddress)
                                connector.sendLine(Strings.espCommandRestart(prop.userName, prop.password))
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, context.getString(R.string.message_esp_restart_success), Toast.LENGTH_SHORT).show()
                                    ESPUtilsApp.showAd(context as MainActivity)
                                }
                            } catch (ex: Exception) {
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, context.getString(R.string.message_esp_restart_failure), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .show()
            }
            else Toast.makeText(context, context.resources.getString(R.string.message_device_offline), Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteClickAction(context: Context, position: Int, settingsDialog: AlertDialog): Runnable{
        return Runnable{
            AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                .setIcon(R.drawable.icon_delete)
                .setTitle(context.resources.getString(R.string.confirm_delete))
                .setMessage(context.resources.getString(R.string.message_confirm_delete_note))
                .setNegativeButton(context.resources.getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(context.resources.getString(R.string.remove)) { dialog, _ ->
                    dialog.dismiss()
                    propList[position].delete()
                    propList.removeAt(position)
                    notifyItemRemoved(position)
                    settingsDialog.dismiss()
                    Toast.makeText(context, context.getString(R.string.message_device_delete), Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun resetClickAction(context: Context, prop: DeviceProperties): Runnable{
        return Runnable{
            if(prop.isConnected) {
                AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                    .setTitle(context.resources.getString(R.string.confirm_reset))
                    .setIcon(R.drawable.ic_reset)
                    .setMessage(context.resources.getString(R.string.message_confirm_reset_note))
                    .setNegativeButton(context.resources.getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(context.resources.getString(R.string.reset)) { dialog, _ ->
                        dialog.dismiss()
                        ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
                            try {
                                val connector = SocketClient.Connector(prop.ipAddress)
                                connector.sendLine(Strings.espCommandReset(prop.userName, prop.password))
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, context.getString(R.string.message_esp_reset_success), Toast.LENGTH_SHORT).show()
                                    ESPUtilsApp.showAd(context as MainActivity)
                                }
                            } catch (ex: Exception) {
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, context.getString(R.string.message_esp_restart_failure), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .show()
            }
            else Toast.makeText(context, context.resources.getString(R.string.message_device_offline), Toast.LENGTH_SHORT).show()
        }
    }

    private fun editClickAction(context: Context, prop: DeviceProperties): Runnable{
        return Runnable{
            devicesFragment.onAddressVerified(context, prop.ipAddress, prop.macAddress)
        }
    }

    private fun extractUpdate(file: File, updateIntermediateListener: OnOTAIntermediateListener?): File?{
        updateIntermediateListener?.onStatusUpdate(context.getString(R.string.message_extracting_update_file), true)
        if(file.exists() and file.isFile){
            if(!file.absolutePath.endsWith(Strings.extensionZip)){
                updateIntermediateListener?.onError(context.getString(R.string.message_err_update_file_is_not_valid))
                return null
            }

            val extractDir = File(file.parentFile?.absolutePath + File.separator + file.name.split(".")[0])
            if(extractDir.exists()) {
                if(extractDir.isFile) extractDir.delete() else extractDir.deleteRecursively()
            }
            extractDir.mkdirs()
            val path = extractDir.absolutePath

            val zip: ZipFile
            try{
                zip = ZipFile(file)
            }catch(_: ZipException){
                updateIntermediateListener?.onError(context.getString(R.string.message_err_update_file_corrupt))
                return null
            }

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
        updateIntermediateListener?.onError(context.getString(R.string.message_err_update_file_does_not_exist))
        return null
    }

    private fun updateClickAction(context: Context, prop: DeviceProperties): Runnable{
        return Runnable{
            if(prop.isConnected) {
                val dialogBiding = EspOtaLayoutBinding.inflate(LayoutInflater.from(context))
                devicesFragment.updateSelectedListener = { updateFile ->
                    val updateDialog = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                        .setTitle(R.string.esp_ota_update)
                        .setView(dialogBiding.root)
                        .setIcon(R.drawable.ic_system_update)
                        .setNegativeButton(R.string.cancel) { _, _ -> }
                        .setPositiveButton(R.string.update) { _, _ -> }
                        .create()
                    updateDialog.setCancelable(false)
                    updateDialog.setOnDismissListener {
                        ESPUtilsApp.showAd(context as MainActivity)
                    }
                    updateDialog.setOnShowListener {
                        val positiveButton = updateDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        val negativeButton = updateDialog.getButton(DialogInterface.BUTTON_NEGATIVE)

                        val messageView = dialogBiding.progressTitle
                        val progressBar = dialogBiding.updateProgressBar
                        val progressLayout = dialogBiding.otaProgressLayout
                        val percentText = dialogBiding.otaPercentage
                        val progressText = dialogBiding.otaContentProgress
                        val errorView = dialogBiding.errorContent

                        val updateIntermediateListener = object : OnOTAIntermediateListener {
                            override fun onStatusUpdate(status: String, progress: Boolean) {
                                Handler(Looper.getMainLooper()).post {
                                    if (progress) {
                                        progressLayout.visibility = View.VISIBLE
                                        progressBar.isIndeterminate = true
                                        negativeButton?.isEnabled = false
                                        positiveButton?.isEnabled = false
                                    } else progressLayout.visibility = View.GONE
                                    messageView.text = status
                                    if (status.lowercase(Locale.ROOT).contains(Strings.espResponseSuccess)) {
                                        positiveButton?.isEnabled = true
                                        positiveButton?.setOnClickListener {
                                            updateDialog.dismiss()
                                        }
                                    }
                                }
                            }

                            override fun onProgressUpdate(fileLength: Long, offset: Long) {
                                Handler(Looper.getMainLooper()).post  {
                                    negativeButton?.isEnabled = false
                                    positiveButton?.isEnabled = false

                                    errorView.visibility = View.GONE
                                    progressLayout.visibility = View.VISIBLE

                                    progressBar.isIndeterminate = false

                                    percentText.text = context.getString(
                                        R.string.ota_percent_progress, (offset * 100 / fileLength).toInt()
                                    )

                                    if(offset < fileLength)
                                        progressText.text = context.getString(
                                            R.string.ota_content_progress,
                                            Utils.getConventionalSize(offset),
                                            Utils.getConventionalSize(fileLength)
                                        )
                                    else progressText.text = Utils.getConventionalSize(fileLength)

                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                        progressBar.setProgress((offset * 100 / fileLength).toInt(), true)
                                    } else {
                                        progressBar.progress = (offset * 100 / fileLength).toInt()
                                    }
                                }
                            }

                            override fun onError(message: String) {
                                Handler(Looper.getMainLooper()).post  {
                                    negativeButton?.isEnabled = true
                                    positiveButton?.isEnabled = false

                                    negativeButton?.text = context.getString(R.string.close)
                                    errorView.text = message

                                    errorView.visibility = View.VISIBLE
                                    messageView.visibility = View.GONE
                                    progressLayout.visibility = View.GONE
                                }
                            }
                        }
                        positiveButton?.setOnClickListener {
                            positiveButton.isEnabled = false
                            negativeButton?.isEnabled = false

                            positiveButton.text = context.resources.getString(R.string.done)
                            ThreadHandler.runOnFreeThread {
                                val updateDir =
                                    extractUpdate(updateFile, updateIntermediateListener)
                                if (updateDir != null) {
                                    if (!verifyUpdate(updateDir, updateIntermediateListener)) {
                                        updateDir.deleteRecursively()
                                    }

                                    val espOta = EspOta(prop)
                                    val system =
                                        updateDir.listFiles { _, name -> name.endsWith(Strings.extensionBin) }
                                            ?.find {
                                                it.name.lowercase(Locale.ROOT).startsWith(context.getString(R.string.update_file_system))
                                            }
                                    system?.let {
                                        espOta.installUpdate(it, EspOta.SYSTEM, updateIntermediateListener)
                                        return@runOnFreeThread
                                    }

                                    val spiffs = updateDir.listFiles { _, name ->
                                        name.endsWith(Strings.extensionBin)
                                    }?.find {
                                        it.name.lowercase(Locale.ROOT).startsWith(context.getString(R.string.update_file_spiffs))
                                    }
                                    spiffs?.let {
                                        espOta.installUpdate(it, EspOta.SPIFFS, updateIntermediateListener)
                                        return@runOnFreeThread
                                    }
                                }
                            }
                        }
                    }
                    updateDialog.show()
                }
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = Strings.intentTypeZip
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                try {
                    devicesFragment.espOtaChooser.launch(
                        Intent.createChooser(
                            intent,
                            context.getString(R.string.message_ota_chooser)
                        )
                    )
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.message_install_file_manager, Toast.LENGTH_SHORT).show()
                }
            }
            else Toast.makeText(context, R.string.message_device_offline, Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyUpdate(updateDir: File, updateIntermediateListener: OnOTAIntermediateListener?): Boolean{
        updateIntermediateListener?.onStatusUpdate(context.getString(R.string.message_verify_update), true)
        val hashFile = File(updateDir.absolutePath, Strings.extensionHash)
        if(!hashFile.exists()){
            updateIntermediateListener?.onError(context.getString(R.string.message_err_update_hash))
            return false
        }

        val hashOrigin = hashFile.inputStream().readBytes().toString(Charsets.UTF_8).replace("\n","")
        var hash = ""
        updateDir.listFiles{ _, name -> name.endsWith(Strings.extensionBin)}?.sortedArray()?.forEach {
            hash += Utils.md5(it) + Utils.md5(it.name)
        }

        if(Utils.md5(hash) != hashOrigin) updateIntermediateListener?.onError(context.getString(R.string.message_err_verify_update_failed))
        return Utils.md5(hash) == hashOrigin
    }

    private fun userSettingsClickAction(context: Context, prop: DeviceProperties): Runnable{
        return Runnable{
            if(prop.isConnected) {
                val dialogBinding = UserSettingsBinding.inflate(LayoutInflater.from(context))
                val userDialog = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                    .setTitle(R.string.title_user_settings)
                    .setView(dialogBinding.root)
                    .setIcon(R.drawable.ic_user)
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(context.getString(R.string.apply)) { _, _ -> }
                    .create()

                userDialog.setOnShowListener {
                    val cUname = dialogBinding.curUserName
                    val cPass = dialogBinding.curUserPasswd
                    val nUname = dialogBinding.tilUserName
                    val nPass = dialogBinding.tilUserPasswd
                    val nPassCon = dialogBinding.tilUserConfirmPasswd

                    for (item in listOf(cUname, cPass, nUname, nPass, nPassCon)) {
                        item.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable?) {}
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                (item.parent.parent as TextInputLayout).error = null
                            }
                        })
                    }

                    val positiveButton = userDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    positiveButton.setOnClickListener {
                        var hasError = false
                        for (txtInputEdtTxt in listOf(cUname, cPass, nUname, nPass, nPassCon)) {
                            if (txtInputEdtTxt.text.isNullOrEmpty()) {
                                (txtInputEdtTxt.parent.parent as TextInputLayout).error =
                                    context.getString(R.string.message_empty_field)
                                hasError = true
                            }
                        }
                        if (nPassCon.text != nPass.text) {
                            (nPassCon.parent.parent as TextInputLayout).error =
                                context.getString(R.string.message_passwd_mismatch)
                            hasError = true
                        }
                        if (!hasError) {
                            AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                                .setTitle(R.string.confirm)
                                .setMessage(R.string.message_err_user_settings)
                                .setNegativeButton(R.string.cancel) { dg, _ -> dg.dismiss() }
                                .setPositiveButton(R.string.confirm) { _, _ ->
                                    userDialog.dismiss()
                                    ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
                                        try {
                                            val connector = SocketClient.Connector(prop.ipAddress)
                                            connector.sendLine(
                                                Strings.espCommandChangeUser(
                                                    cUname.text.toString(),
                                                    cPass.text.toString(),
                                                    nUname.text.toString(),
                                                    nPass.text.toString()
                                                )
                                            )
                                            val result = connector.readLine()
                                            val resultObj = JSONObject(result)
                                            Handler(Looper.getMainLooper()).post {
                                                val nDialog = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                                                    .setTitle(
                                                        if (resultObj.getString(Strings.espResponse)
                                                                .contains(
                                                                    Strings.espResponseSuccess,
                                                                    true
                                                                )
                                                        ) R.string.title_success else R.string.title_failure
                                                    )
                                                    .setMessage(resultObj.getString(Strings.espResponse))
                                                    .setPositiveButton(R.string.done) { dg, _ -> dg.dismiss() }
                                                    .show()
                                                nDialog.setOnDismissListener {
                                                    ESPUtilsApp.showAd(context as MainActivity)
                                                }
                                            }
                                            connector.close()
                                        } catch (ex: Exception) {
                                            Handler(Looper.getMainLooper()).post {
                                                AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                                                    .setTitle(R.string.title_failure)
                                                    .setMessage(R.string.message_fail_user_settings)
                                                    .setPositiveButton(R.string.close) { dg, _ -> dg.dismiss() }
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
            else Toast.makeText(context, R.string.message_device_offline, Toast.LENGTH_SHORT).show()
        }
    }

    private fun wirelessSettingsClickAction(context: Context, prop: DeviceProperties): Runnable{
        return Runnable {
            if(prop.isConnected) {
                val dialogBinding = WirelessSettingsBinding.inflate(LayoutInflater.from(context))
                val dialog = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                    .setTitle(R.string.title_wireless_settings)
                    .setView(dialogBinding.root)
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(context.getString(R.string.apply)) { _, _ -> }
                    .setIcon(R.drawable.icon_wifi)
                    .create()
                dialog.setOnShowListener {
                    val ssid = dialogBinding.tilWifiName
                    val pass = dialogBinding.tilWifiPasswd
                    val progressBar = dialogBinding.getWirelessLoading
                    val spinner = dialogBinding.spinnerWirelessMode

                    var wirelessData: JSONObject? = null

                    spinner.adapter = ArrayAdapter(
                        context, android.R.layout.simple_list_item_1,
                        context.resources.getStringArray(R.array.esp_wireless_mode)
                    )
                    var mode = if (spinner.selectedItemPosition == 0) Strings.espWirelessStation else Strings.espWirelessAp
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?,
                            position: Int, id: Long) {
                            mode = if (position == 0) Strings.espWirelessStation else Strings.espWirelessAp
                            ssid.setText(
                                wirelessData?.optString(
                                    when (mode) {
                                        Strings.espWirelessStation -> "station"
                                        else -> "ap"
                                    } + "_ssid"
                                )
                            )
                            pass.setText(
                                wirelessData?.optString(
                                    when (mode) {
                                        Strings.espWirelessStation -> "station"
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
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(charSeq: CharSequence?, start: Int, before: Int, count: Int) {
                                (item.parent.parent as TextInputLayout).error = null
                                wirelessData?.put(
                                    when (mode) {
                                        Strings.espWirelessStation -> "station"
                                        else -> "ap"
                                    } + "_" + when (item.id) {
                                        R.id.til_wifi_name -> "ssid"
                                        else -> "psk"
                                    }, charSeq
                                )
                            }
                        })
                    }

                    progressBar.visibility = View.VISIBLE
                    getWirelessSettings(prop.ipAddress, prop.userName, prop.password) { data ->
                        progressBar.visibility = View.GONE
                        val wirelessMode = data.optString(Strings.espWirelessMode)
                        if (!wirelessMode.isNullOrEmpty()) {
                            wirelessData = data
                            mode = wirelessMode
                            spinner.setSelection(
                                when (mode) {
                                    Strings.espWirelessStation -> 0
                                    else -> 1
                                }, true
                            )
                            ssid.setText(
                                wirelessData?.optString(
                                    when (mode) {
                                        Strings.espWirelessStation -> "station"
                                        else -> "ap"
                                    } + "_ssid"
                                )
                            )
                            pass.setText(
                                wirelessData?.optString(
                                    when (mode) {
                                        Strings.espWirelessStation -> "station"
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
                                context.getString(R.string.message_empty_ssid)
                        if ((pass.text?.length ?: 8) < 8)
                            (pass.parent.parent as TextInputLayout).error =
                                context.getString(R.string.message_empty_password)
                        if (ssid.text!!.isNotEmpty() and ((pass.text?.length ?: 0) >= 8)) {
                            AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                                .setTitle(R.string.confirm)
                                .setMessage(R.string.message_err_user_settings)
                                .setNegativeButton(R.string.cancel) { dg, _ -> dg.dismiss() }
                                .setPositiveButton(R.string.confirm) { _, _ ->
                                    dialog.dismiss()
                                    ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
                                        try {
                                            val connector = SocketClient.Connector(prop.ipAddress)
                                            connector.sendLine(
                                                Strings.espCommandChangeWireless(
                                                    prop.userName, prop.password, mode, ssid.text.toString(), pass.text.toString()
                                                )
                                            )
                                            val result = connector.readLine()
                                            val resultObj = JSONObject(result)
                                            Handler(Looper.getMainLooper()).post {
                                                val nDialog = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                                                    .setTitle(
                                                        if (resultObj.getString(Strings.espResponse)
                                                                .contains(
                                                                    Strings.espResponseSuccess,
                                                                    true
                                                                )
                                                        ) R.string.title_success else R.string.title_failure
                                                    )
                                                    .setMessage(resultObj.getString(Strings.espResponse))
                                                    .setPositiveButton(R.string.done) { dg, _ -> dg.dismiss() }
                                                    .show()
                                                nDialog.setOnDismissListener {
                                                    ESPUtilsApp.showAd(context as MainActivity)
                                                }
                                            }
                                            connector.close()
                                        } catch (ex: Exception) {
                                            Handler(Looper.getMainLooper()).post {
                                                AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
                                                    .setTitle(R.string.title_failure)
                                                    .setMessage(R.string.message_fail_wireless_settings)
                                                    .setPositiveButton(R.string.close) { dg, _ -> dg.dismiss() }
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
            else Toast.makeText(context, R.string.message_device_offline, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getWirelessSettings(address: String, userName: String, password: String, onReadWiFiConfig: ((data: JSONObject) -> Unit)){
        ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE){
            try {
                val connector = SocketClient.Connector(address)
                connector.sendLine(Strings.espCommandGetWireless(userName, password))
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