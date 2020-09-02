package com.irware.remote.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.android.material.textfield.TextInputEditText
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.DeviceProperties
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.adapters.DeviceListAdapter
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(rootView == null ){
            rootView = inflater.inflate(R.layout.fragment_devices, container, false) as RelativeLayout
            viewManager = LinearLayoutManager(context)
            viewAdapter = DeviceListAdapter(MainActivity.devicePropList)
            recyclerView = rootView!!.findViewById<RecyclerView>(R.id.manage_remotes_recycler_view).apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
            rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_gpio).setClosedOnTouchOutside(true)

            val refreshLayout = rootView!!.findViewById<SwipeRefreshLayout>(R.id.refresh_layout)
            refreshLayout.setOnRefreshListener {
                refreshLayout.isRefreshing = true
                viewAdapter.notifyDataSetChanged()
                refreshLayout.isRefreshing = false
            }

            val fabScan = rootView!!.findViewById<FloatingActionButton>(R.id.fab_scan_device)
            val fabEnterAddress = rootView!!.findViewById<FloatingActionButton>(R.id.fab_enter_address)

            fabScan.setOnClickListener {  }
            fabEnterAddress.setOnClickListener { newDeviceEnterAddress() }

        }
        val manageMenu = rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_gpio)
        if(!manageMenu.isOpened)
            manageMenu.hideMenuButton(false)
        Handler().postDelayed({
            if(manageMenu.isMenuButtonHidden)
                manageMenu.showMenuButton(true)
            if(MainActivity.remotePropList.isEmpty())
                Handler().postDelayed({manageMenu.showMenu(true)},400)
        },400)
        return rootView
    }

    @SuppressLint("InflateParams")
    fun newDeviceEnterAddress(){
        val content = LayoutInflater.from(context).inflate(R.layout.add_new_device,null) as ScrollView
        val btnCancel = content.findViewById<Button>(R.id.cancel)
        val btnNext = content.findViewById<Button>(R.id.button_done)
        val devAddr = content.findViewById<TextInputEditText>(R.id.device_address)

        val dialog = Dialog(context!!)
        dialog.setContentView(content)
        btnCancel.setOnClickListener { dialog.cancel() }
        btnNext.setOnClickListener {
            val address = devAddr.text.toString()
            Thread{
                try {
                    val connector = SocketClient.Connector(address)
                    connector.sendLine("{\"request\":\"ping\"}")
                    val response = connector.readLine()
                    val macAddr = JSONObject(response).getString("MAC")
                    (context as Activity).runOnUiThread{
                        onAddressVerified(dialog, address, macAddr)
                    }
                }catch(ex: Exception){
                    (context as Activity).runOnUiThread{
                        Toast.makeText(context, "Err: Failed to contact $address", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }

        dialog.show()
        val width = min(MainActivity.size.x,MainActivity.size.y)
        dialog.window?.setLayout(width - width/8, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_gpio).close(true)
    }

    fun onAddressVerified(dialog: Dialog, address: String, macAddr: String){
        val btnAdd = dialog.findViewById<Button>(R.id.button_done)
        val devAddr = dialog.findViewById<TextInputEditText>(R.id.device_address)
        val devPropertiesLayout = dialog.findViewById<LinearLayout>(R.id.new_device_properties_layout)
        val userName = dialog.findViewById<TextInputEditText>(R.id.device_user_name)
        val password = dialog.findViewById<TextInputEditText>(R.id.device_password)
        val devName = dialog.findViewById<TextInputEditText>(R.id.device_name)
        val devDescription = dialog.findViewById<TextInputEditText>(R.id.device_desc)

        btnAdd.text = MainActivity.activity?.getString(R.string.add_device)

        var devProp: DeviceProperties? = null
        var devExist: Boolean = false
        for(prop: DeviceProperties in MainActivity.devicePropList){
            if(prop.macAddr == macAddr){ devProp = prop; devExist = true; break }
        }

        val pref = context?.getSharedPreferences("login",0)
        userName.setText(devProp?.userName?: pref?.getString("username", ""))
        password.setText(devProp?.password?: pref?.getString("password", ""))

        (devAddr.parent as FrameLayout).visibility = View.GONE
        devPropertiesLayout.visibility = View.VISIBLE

        var addresses = devProp?.ipAddr
        if(addresses == null){
            addresses = JSONArray(arrayOf(address))
        }else{
            addresses.insert(0, address)
        }
        devName.setText(devProp?.nickName?: macAddr.replace(":", "_"))

        btnAdd.setOnClickListener{
            Thread{
                try{
                    val connector = SocketClient.Connector(address)
                    connector.sendLine("{\"request\":\"authenticate\",\"username\":\"${userName.text.toString()}\",\"password\":\"${password.text.toString()}\",\"data\":\"__\",\"length\":\"0\"}")
                    val response=connector.readLine()
                    if(JSONObject(response)["response"] != "authenticated") throw Exception()

                    if (!devExist) {
                        val filePath = MainActivity.deviceConfigPath + File.separator + devName.text.toString().replace(" ", "_") + ".json"
                        File(filePath).createNewFile()
                        devProp = DeviceProperties(File(filePath))
                    }
                    devProp!!.nickName = devName.text.toString()
                    devProp!!.macAddr = macAddr
                    devProp!!.ipAddr = addresses
                    devProp!!.description = devDescription.text.toString()

                    (context as Activity).runOnUiThread{
                        if(devExist){
                            viewAdapter.notifyItemChanged(MainActivity.devicePropList.indexOf(devProp!!))
                            Toast.makeText(context, "Device Preferences Updated", Toast.LENGTH_LONG).show()
                        }else{
                            MainActivity.devicePropList.add(devProp!!)
                            viewAdapter.notifyDataSetChanged()
                            Toast.makeText(context, "Device successfully Added", Toast.LENGTH_LONG).show()
                        }
                        dialog.cancel()
                    }
                }catch(ex:Exception){
                    (context as Activity).runOnUiThread{
                        Toast.makeText(context, "Err: Authentication Failed", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }
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
