package com.github.meudayhegde.esputils.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.meudayhegde.ThreadHandler
import com.github.meudayhegde.esputils.ESPUtilsApp
import com.github.meudayhegde.esputils.MainActivity
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.databinding.AddNewDeviceBinding
import com.github.meudayhegde.esputils.databinding.DevicePropertiesBinding
import com.github.meudayhegde.esputils.databinding.FragmentDevicesBinding
import com.github.meudayhegde.esputils.holders.ARPItem
import com.github.meudayhegde.esputils.holders.DeviceProperties
import com.github.meudayhegde.esputils.listeners.OnFragmentInteractionListener
import com.github.meudayhegde.esputils.net.ESPTable
import com.github.meudayhegde.esputils.net.SocketClient
import com.github.meudayhegde.esputils.ui.adapters.DeviceListAdapter
import com.github.meudayhegde.esputils.ui.adapters.ScanDeviceListAdapter
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.io.File

class DevicesFragment : androidx.fragment.app.Fragment()  {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private lateinit var fragmentBinding: FragmentDevicesBinding
    private var _binding: FragmentDevicesBinding? = null

    var updateSelectedListener: ((File) -> Unit)? = null
    val espOtaChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == Activity.RESULT_OK) {
            val isr = requireContext().contentResolver.openInputStream(result?.data?.data?: return@registerForActivityResult)?: return@registerForActivityResult
            val update = File((requireContext().externalCacheDir?:requireContext().filesDir).absolutePath + File.separator + "Update.zip")
            if(update.exists()) update.delete()
            ThreadHandler.runOnFreeThread{
                update.createNewFile()

                update.outputStream().use {
                    it.write(isr.readBytes())
                    it.flush()
                }
                isr.close()

                Handler(Looper.getMainLooper()).post{
                    updateSelectedListener?.invoke(update)
                    updateSelectedListener = null
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if(_binding == null ){
            fragmentBinding = FragmentDevicesBinding.inflate(inflater, container, false)
            _binding = fragmentBinding
            viewManager = LinearLayoutManager(context)
            viewAdapter = DeviceListAdapter(ESPUtilsApp.devicePropList, this)
            fragmentBinding.refreshLayout.refreshLayoutRecyclerView.apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }

            fragmentBinding.famManageGpio.setClosedOnTouchOutside(true)

            fragmentBinding.refreshLayout.refreshLayout.setOnRefreshListener {
                fragmentBinding.refreshLayout.refreshLayout.isRefreshing = true

                ESPTable.getInstance(context).refreshDevicesStatus(mutableMapOf<String, DeviceProperties>().apply {
                    for(prop in ESPUtilsApp.devicePropList) this[prop.macAddress] = prop
                })

                ThreadHandler.runOnFreeThread{
                    Thread.sleep(100)
                    ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
                        fragmentBinding.refreshLayout.refreshLayout.isRefreshing = false
                    }
                }
            }

            fragmentBinding.fabScanDevice.setOnClickListener {
                val deviceDialog = AlertDialog.Builder(requireContext(), R.style.AppTheme_AlertDialog)
                    .setIcon(R.drawable.ic_refresh)
                    .setNegativeButton(R.string.cancel){ _, _ -> }
                    .setTitle(R.string.add_new_device)
                    .setView(R.layout.scanner_layout)
                    .create()

                deviceDialog.setOnShowListener {
                    val recyclerViewScanner = deviceDialog.findViewById<RecyclerView>(R.id.scan_device_recycler_view)
                    val arpItemList = ArrayList<ARPItem>()
                    val viewAdapterScanner = ScanDeviceListAdapter(arpItemList)
                    recyclerViewScanner?.apply {
                        setHasFixedSize(true)
                        layoutManager = LinearLayoutManager(context)
                        adapter = viewAdapterScanner
                    }

                    viewAdapterScanner.setOnARPItemSelectedListener { arpItem ->
                        onAddressVerified(requireContext(), arpItem.ipAddress, arpItem.macAddress)
                    }

                    ESPTable.getInstance().scanForARPItems { arpItem ->
                        Handler(Looper.getMainLooper()).post{
                            arpItemList.add(arpItem)
                            ESPUtilsApp.devicePropList.forEach {
                                if(it.macAddress == arpItem.macAddress){
                                    arpItem.devNickName = it.nickName
                                    return@forEach
                                }
                            }
                            viewAdapterScanner.notifyDataSetChanged()
                        }
                    }
                    ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
                        Handler(Looper.getMainLooper()).post {
                            deviceDialog.findViewById<ProgressBar>(R.id.device_scanner_progress_bar)?.visibility = View.GONE
                        }
                    }
                }

                deviceDialog.show()

                fragmentBinding.famManageGpio.close(true)
            }
            fragmentBinding.fabEnterAddress.setOnClickListener { newDeviceEnterAddress() }
        }
        if(!fragmentBinding.famManageGpio.isOpened)
            fragmentBinding.famManageGpio.hideMenuButton(false)
        Handler(Looper.getMainLooper()).postDelayed({
            if(fragmentBinding.famManageGpio.isMenuButtonHidden)
                fragmentBinding.famManageGpio.showMenuButton(true)
            if(ESPUtilsApp.remotePropList.isEmpty())
                Handler(Looper.getMainLooper()).postDelayed({fragmentBinding.famManageGpio.showMenu(true)},400)
        },400)
        return fragmentBinding.root
    }

    private fun newDeviceEnterAddress(){
        val contentBinding = AddNewDeviceBinding.inflate(layoutInflater)
        val newDevDialog = AlertDialog.Builder(requireContext(), R.style.AppTheme_AlertDialog)
            .setView(contentBinding.root)
            .setTitle(R.string.add_new_device)
            .setIcon(R.drawable.ic_refresh)
            .setNegativeButton(R.string.cancel){_, _ ->}
            .setPositiveButton(R.string.next){_, _ ->}
            .create()
        newDevDialog.setOnShowListener {
            val btnNext = newDevDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            btnNext.setOnClickListener {
                val address = contentBinding.deviceAddress.text.toString()
                ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE){
                    try {
                        val connector = SocketClient.Connector(address)
                        connector.sendLine(Strings.espCommandPing)
                        val response = connector.readLine()
                        val macAddress = JSONObject(response).getString(Strings.espResponseMac)
                        Handler(Looper.getMainLooper()).post{
                            newDevDialog.dismiss()
                            onAddressVerified(requireContext(), address, macAddress)
                        }
                    }catch(ex: Exception){
                        Handler(Looper.getMainLooper()).post{
                            Toast.makeText(context, context?.getString(R.string.message_err_failed_to_contact_device, address), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        newDevDialog.show()
        fragmentBinding.famManageGpio.close(true)
    }

    fun onAddressVerified(context: Context, address: String, macAddress: String){
        val contentBinding = DevicePropertiesBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
            .setView(contentBinding.root)
            .setIcon(R.drawable.icon_edit)
            .setTitle(context.resources.getString(R.string.set_dev_props))
            .setNegativeButton(context.resources.getString(R.string.cancel)){ dialog, _ -> dialog.dismiss()}
            .setPositiveButton(context.resources.getString(R.string.apply)){ dialog, _ -> dialog.dismiss() }
            .create()
        dialog.setOnShowListener {
            val btnAdd = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

            val devName = dialog.findViewById<TextInputEditText>(R.id.device_name)!!
            val devDescription = dialog.findViewById<TextInputEditText>(R.id.device_desc)!!

            var devProp: DeviceProperties? = null
            var devExist = false
            for(prop: DeviceProperties in ESPUtilsApp.devicePropList){
                if(prop.macAddress == macAddress){ devProp = prop; devExist = true; break }
            }

            val pref = context.getSharedPreferences(Strings.sharedPrefNameLogin, 0)
            contentBinding.deviceUserName.setText(devProp?.userName?: pref?.getString(Strings.sharedPrefItemUsername, ""))
            contentBinding.devicePassword.setText(devProp?.password?: pref?.getString(Strings.sharedPrefItemPassword, ""))

            devName.setText(devProp?.nickName?: macAddress.replace(":", "_"))
            devDescription.setText(devProp?.description?: "")

            btnAdd.setOnClickListener{
                ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE){
                    try{
                        val connector = SocketClient.Connector(address)
                        connector.sendLine(Strings.espCommandAuth(
                            contentBinding.deviceUserName.text.toString(),
                            contentBinding.devicePassword.text.toString()
                        ))
                        val response = connector.readLine()

                        if(JSONObject(response)[Strings.espResponse] != Strings.espResponseAuthSuccess) throw Exception()

                        if (!devExist) {
                            val devConfigFile = ESPUtilsApp.getPrivateFile(
                                Strings.nameDirDeviceConfig,
                                devName.text.toString().replace(" ", "_") + Strings.extensionJson
                            )
                            devConfigFile.createNewFile()
                            devProp = DeviceProperties(devConfigFile)
                        }

                        devProp!!.nickName = devName.text.toString()
                        devProp!!.macAddress = macAddress
                        devProp!!.description = devDescription.text.toString()
                        devProp!!.userName = contentBinding.deviceUserName.text.toString()
                        devProp!!.password = contentBinding.devicePassword.text.toString()

                        Handler(Looper.getMainLooper()).post{
                            if(devExist){
                                viewAdapter.notifyItemChanged(ESPUtilsApp.devicePropList.indexOf(devProp!!))
                                Toast.makeText(context, R.string.message_dev_prop_updated, Toast.LENGTH_LONG).show()
                            }else{
                                ESPUtilsApp.devicePropList.add(devProp!!)
                                viewAdapter.notifyItemInserted(ESPUtilsApp.devicePropList.indexOf(devProp!!))
                                Toast.makeText(context, R.string.message_dev_prop_added, Toast.LENGTH_LONG).show()
                            }
                            dialog.dismiss()
                            ESPUtilsApp.showAd(context as MainActivity)
                        }
                    }catch(ex: Exception){
                        Handler(Looper.getMainLooper()).post{
                            Toast.makeText(context, R.string.message_auth_failed, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}