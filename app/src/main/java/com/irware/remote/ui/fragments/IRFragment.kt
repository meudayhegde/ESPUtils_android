package com.irware.remote.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.android.material.textfield.TextInputEditText
import com.irware.ThreadHandler
import com.irware.remote.ESPUtilsApp
import com.irware.remote.R
import com.irware.remote.Strings
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.listeners.OnFragmentInteractionListener
import com.irware.remote.ui.adapters.RemoteListAdapter
import com.irware.remote.ui.dialogs.RemoteDialog
import java.util.*


class IRFragment : androidx.fragment.app.Fragment(), View.OnClickListener {
    private var listener: OnFragmentInteractionListener? = null
    private var recyclerView: RecyclerView? = null
    private var viewAdapter: RecyclerView.Adapter<*>? = null
    private var viewManager: RecyclerView.LayoutManager? = null
    private var rootView:RelativeLayout? = null
    private val configChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == Activity.RESULT_OK) {
            val uri = result?.data?.data
            try {
                val mIntent = Intent(Intent.ACTION_VIEW)
                mIntent.setDataAndType(uri, Strings.intentTypeJson)
                mIntent.setPackage(context?.packageName)
                startActivity(Intent.createChooser(mIntent, "Import Config File"))
            } catch (ex: Exception) {
                Log.e(javaClass.simpleName, "${ ex.message }")
            }
        }
    }

    @SuppressLint("DefaultLocale", "InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(rootView == null){
            rootView = inflater.inflate(R.layout.fragment_manage_remote, container, false) as RelativeLayout
            viewManager = LinearLayoutManager(context)
            viewAdapter = RemoteListAdapter(ESPUtilsApp.remotePropList,0)
            recyclerView = rootView!!.findViewById<RecyclerView>(R.id.refresh_layout_recycler_view).apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
            rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_remotes).setClosedOnTouchOutside(true)
            rootView!!.findViewById<FloatingActionButton>(R.id.fab_import_remote).setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = Strings.intentTypeJson
                intent.addCategory(Intent.CATEGORY_OPENABLE)

                try {
                    configChooser.launch(Intent.createChooser(intent, "Select a remote controller configuration file"))
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(
                        context, "Please install a File Manager.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_remotes).close(true)
            }

            val refreshLayout = rootView!!.findViewById<SwipeRefreshLayout>(R.id.refresh_layout)
            refreshLayout.setOnRefreshListener {
                refreshLayout.isRefreshing = true
                ThreadHandler.runOnFreeThread{
                    ESPUtilsApp.remotePropList.clear()
                    val files = ESPUtilsApp.getPrivateFile(Strings.nameDirRemoteConfig).listFiles { pathname ->
                        pathname!!.isFile and (pathname.name.endsWith(Strings.extensionJson, true)) and pathname.canWrite()
                    }
                    files!!.forEach {
                        ESPUtilsApp.remotePropList.add(RemoteProperties(it, null))
                    }
                    Handler(Looper.getMainLooper()).post{
                        viewAdapter?.notifyDataSetChanged()
                        refreshLayout.isRefreshing = false
                    }
                }
            }

            rootView!!.findViewById<FloatingActionButton>(R.id.fab_new_remote).setOnClickListener(this)
        }
        val manageMenu = rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_remotes)
        if(!manageMenu.isOpened)
            manageMenu.hideMenuButton(false)

        Handler(Looper.getMainLooper()).postDelayed({
            if(manageMenu.isMenuButtonHidden)
                manageMenu.showMenuButton(true)
            if(ESPUtilsApp.remotePropList.isEmpty())
                Handler(Looper.getMainLooper()).postDelayed({manageMenu.showMenu(true)},400)
        },400)
        return rootView
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

    fun notifyDataChanged(){
        viewAdapter?.notifyDataSetChanged()
    }

    override fun onClick(v: View?) {
        val newRemoteDialog = AlertDialog.Builder(requireContext(), R.style.AppTheme_AlertDialog)
            .setTitle(R.string.enter_remote_details)
            .setView(R.layout.new_remote_confirm)
            .setIcon(R.drawable.icon_ir_remote)
            .setPositiveButton(R.string.done){ _, _ ->

            }.setNegativeButton(R.string.cancel){ _, _ ->

            }
            .create()

        newRemoteDialog.setOnShowListener {
            val vendor = newRemoteDialog.findViewById<TextInputEditText>(R.id.vendor_name)
            val name = newRemoteDialog.findViewById<TextInputEditText>(R.id.model_name)
            val desc = newRemoteDialog.findViewById<TextInputEditText>(R.id.remote_desc)

            val spinner = newRemoteDialog.findViewById<Spinner>(R.id.select_device)
            val devicePropList = arrayListOf<Any>(getString(R.string.select_device))
            devicePropList.addAll(ESPUtilsApp.devicePropList)
            spinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, devicePropList)

            val btnDone = newRemoteDialog.getButton(DialogInterface.BUTTON_POSITIVE)

            btnDone.setOnClickListener {
                if((spinner?.selectedItemPosition?: 0) == 0){
                    Toast.makeText(requireContext(), getString(R.string.message_device_not_selected_note), Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val selectedDevice = ESPUtilsApp.devicePropList[(spinner?.selectedItemPosition?: 0) - 1]

                var id = ("${vendor?.text.toString()} ${name?.text.toString()}").lowercase(Locale.getDefault())
                    .replace(" ", "_").replace("\n", "").replace("/","_")

                var configFile = ESPUtilsApp.getPrivateFile(Strings.nameDirRemoteConfig, id + Strings.extensionJson)
                var incr = 1
                while(configFile.exists()) {
                    configFile = ESPUtilsApp.getPrivateFile(Strings.nameDirRemoteConfig, id + "_" + incr + Strings.extensionJson)
                    incr++
                }
                if(incr > 1) id += "_" + (incr - 1)
                if (!configFile.exists()) configFile.createNewFile()
                val remoteProperties = RemoteProperties(configFile, null)

                remoteProperties.fileName = configFile.name
                remoteProperties.remoteVendor = vendor?.text.toString()
                remoteProperties.remoteName = name?.text.toString()
                remoteProperties.remoteID = id
                remoteProperties.description = desc?.text.toString()
                remoteProperties.deviceConfigFileName = selectedDevice.deviceConfigFile.name
                ESPUtilsApp.remotePropList.add(remoteProperties)
                viewAdapter?.notifyItemInserted(ESPUtilsApp.remotePropList.size - 1)
                RemoteDialog(requireContext(), remoteProperties,RemoteDialog.MODE_VIEW_EDIT).show()
                newRemoteDialog.dismiss()
            }
        }
        newRemoteDialog.show()
        rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_remotes).close(true)
    }
}

