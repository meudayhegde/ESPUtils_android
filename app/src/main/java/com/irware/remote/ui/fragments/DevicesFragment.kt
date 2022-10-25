package com.irware.remote.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.android.material.textfield.TextInputEditText
import com.irware.ThreadHandler
import com.irware.remote.ESPUtils
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.DeviceProperties
import com.irware.remote.net.ARPItem
import com.irware.remote.net.ARPTable
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.adapters.DeviceListAdapter
import com.irware.remote.ui.adapters.OnARPItemSelectedListener
import com.irware.remote.ui.adapters.ScanDeviceListAdapter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.min

class DevicesFragment : androidx.fragment.app.Fragment()  {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var rootView: RelativeLayout? = null
    private lateinit var manageMenu: FloatingActionMenu

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

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(rootView == null ){
            rootView = inflater.inflate(R.layout.fragment_devices, container, false) as RelativeLayout
            manageMenu = rootView!!.findViewById(R.id.fam_manage_gpio)
            viewManager = LinearLayoutManager(context)
            viewAdapter = DeviceListAdapter(ESPUtils.devicePropList, this)
            recyclerView = rootView!!.findViewById<RecyclerView>(R.id.manage_remotes_recycler_view).apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }

            manageMenu.setClosedOnTouchOutside(true)

            val refreshLayout = rootView!!.findViewById<SwipeRefreshLayout>(R.id.refresh_layout)
            refreshLayout.setOnRefreshListener {
                refreshLayout.isRefreshing = true
                viewAdapter.notifyDataSetChanged()
                ThreadHandler.runOnFreeThread{
                    Thread.sleep(100)
                    ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
                        refreshLayout.isRefreshing = false
                    }
                }
            }

            val fabScan = rootView!!.findViewById<FloatingActionButton>(R.id.fab_scan_device)
            val fabEnterAddress = rootView!!.findViewById<FloatingActionButton>(R.id.fab_enter_address)

            fabScan.setOnClickListener {
                val content = LayoutInflater.from(context).inflate(R.layout.add_new_device,null) as LinearLayout
                val scrollView = content.findViewById<ScrollView>(R.id.add_new_device_scroll)
                val recyclerView = content.findViewById<RecyclerView>(R.id.device_list_add_new_device)
                val btnCancel = content.findViewById<Button>(R.id.cancel)
                val btnNext = content.findViewById<Button>(R.id.button_done)

                scrollView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                val viewAdapter = ScanDeviceListAdapter(ESPUtils.arpTable.getARPItemList())
                recyclerView.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    adapter = viewAdapter
                }

                val dialog = Dialog(requireContext())
                dialog.setContentView(content)
                btnCancel.setOnClickListener { dialog.cancel() }
                btnNext.visibility = View.GONE

                dialog.show()
                val width = min(MainActivity.layoutParams.width, MainActivity.layoutParams.height)
                dialog.window?.setLayout(width - width/8, WindowManager.LayoutParams.WRAP_CONTENT)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                manageMenu.close(true)

                viewAdapter.setOnARPItemSelectedListener(object: OnARPItemSelectedListener{
                    override fun onARPItemSelected(arpItem: ARPItem) {
                        recyclerView.visibility = View.GONE
                        scrollView.visibility = View.VISIBLE
                        btnNext.visibility = View.VISIBLE
                        onAddressVerified(requireContext(), arpItem.ipAddress, arpItem.macAddress)
                    }

                })
            }
            fabEnterAddress.setOnClickListener { newDeviceEnterAddress() }
        }
        if(!manageMenu.isOpened)
            manageMenu.hideMenuButton(false)
        Handler(Looper.getMainLooper()).postDelayed({
            if(manageMenu.isMenuButtonHidden)
                manageMenu.showMenuButton(true)
            if(ESPUtils.remotePropList.isEmpty())
                Handler(Looper.getMainLooper()).postDelayed({manageMenu.showMenu(true)},400)
        },400)
        return rootView
    }

    @SuppressLint("InflateParams")
    fun newDeviceEnterAddress(){
        val content = LayoutInflater.from(context).inflate(R.layout.add_new_device,null) as LinearLayout
        val btnCancel = content.findViewById<Button>(R.id.cancel)
        val btnNext = content.findViewById<Button>(R.id.button_done)
        val devAddr = content.findViewById<TextInputEditText>(R.id.device_address)

        val dialog = Dialog(requireContext())
        dialog.setContentView(content)
        btnCancel.setOnClickListener { dialog.cancel() }
        btnNext.setOnClickListener {
            val address = devAddr.text.toString()
            ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE){
                try {
                    val connector = SocketClient.Connector(address)
                    connector.sendLine("{\"request\":\"ping\"}")
                    val response = connector.readLine()
                    val macAddr = JSONObject(response).getString("MAC")
                    Handler(Looper.getMainLooper()).post{
                        onAddressVerified(requireContext(), address, macAddr)
                    }
                }catch(ex: Exception){
                    Handler(Looper.getMainLooper()).post{
                        Toast.makeText(context, "Err: Failed to contact $address", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        dialog.show()
        val width = min(MainActivity.layoutParams.width, MainActivity.layoutParams.height)
        dialog.window?.setLayout(width - width/8, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        manageMenu.close(true)
    }

    fun onAddressVerified(context: Context, address: String, macAddr: String){
        val dialog = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
            .setView(R.layout.device_properties)
            .setIcon(R.drawable.icon_edit)
            .setTitle(context.resources.getString(R.string.set_dev_props))
            .setNegativeButton(context.resources.getString(R.string.cancel)){ dialog, _ -> dialog.dismiss()}
            .setPositiveButton(context.resources.getString(R.string.apply)){ dialog, _ -> dialog.dismiss() }
            .create()
        dialog.setOnShowListener {
            val btnAdd = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

            val userName = dialog.findViewById<TextInputEditText>(R.id.device_user_name)!!
            val password = dialog.findViewById<TextInputEditText>(R.id.device_password)!!
            val devName = dialog.findViewById<TextInputEditText>(R.id.device_name)!!
            val devDescription = dialog.findViewById<TextInputEditText>(R.id.device_desc)!!

            var devProp: DeviceProperties? = null
            var devExist = false
            for(prop: DeviceProperties in ESPUtils.devicePropList){
                if(prop.macAddress == macAddr){ devProp = prop; devExist = true; break }
            }

            val pref = context.getSharedPreferences("login",0)
            userName.setText(devProp?.userName?: pref?.getString("username", ""))
            password.setText(devProp?.password?: pref?.getString("password", ""))


            devName.setText(devProp?.nickName?: macAddr.replace(":", "_"))
            devDescription.setText(devProp?.description?: "")

            btnAdd.setOnClickListener{
                ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE){
                    try{
                        val connector = SocketClient.Connector(address)
                        connector.sendLine("{\"request\":\"authenticate\",\"username\":\"${userName.text.toString()}\",\"password\":\"${password.text.toString()}\",\"data\":\"__\",\"length\":\"0\"}")
                        val response=connector.readLine()
                        if(JSONObject(response)["response"] != "authenticated") throw Exception()

                        if (!devExist) {
                            val filePath = ESPUtils.deviceConfigPath + File.separator + devName.text.toString().replace(" ", "_") + ".json"
                            File(filePath).createNewFile()
                            devProp = DeviceProperties(File(filePath))
                        }

                        devProp!!.nickName = devName.text.toString()
                        devProp!!.macAddress = macAddr
                        devProp!!.description = devDescription.text.toString()
                        devProp!!.userName = userName.text.toString()
                        devProp!!.password = password.text.toString()

                        Handler(Looper.getMainLooper()).post{
                            if(devExist){
                                viewAdapter.notifyItemChanged(ESPUtils.devicePropList.indexOf(devProp!!))
                                Toast.makeText(context, "Device Preferences Updated", Toast.LENGTH_LONG).show()
                            }else{
                                ESPUtils.devicePropList.add(devProp!!)
                                viewAdapter.notifyItemInserted(ESPUtils.devicePropList.indexOf(devProp!!))
                                Toast.makeText(context, "Device successfully Added", Toast.LENGTH_LONG).show()
                            }
                            dialog.dismiss()
                        }
                    }catch(ex:Exception){
                        Handler(Looper.getMainLooper()).post{
                            Toast.makeText(context, "Err: Authentication Failed", Toast.LENGTH_LONG).show()
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

private fun JSONArray.insert(position: Int, value: Any){
    for (i in length() downTo position + 1) {
        put(i, get(i - 1))
    }
    put(position, value)
}
