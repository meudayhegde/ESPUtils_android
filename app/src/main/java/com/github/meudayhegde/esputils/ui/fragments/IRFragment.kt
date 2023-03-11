package com.github.meudayhegde.esputils.ui.fragments

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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.meudayhegde.ThreadHandler
import com.github.meudayhegde.esputils.ESPUtilsApp
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.databinding.FragmentManageRemoteBinding
import com.github.meudayhegde.esputils.databinding.NewRemoteConfirmBinding
import com.github.meudayhegde.esputils.holders.RemoteProperties
import com.github.meudayhegde.esputils.listeners.OnFragmentInteractionListener
import com.github.meudayhegde.esputils.ui.adapters.RemoteListAdapter
import com.github.meudayhegde.esputils.ui.dialogs.RemoteDialog
import java.util.*

class IRFragment : androidx.fragment.app.Fragment(), View.OnClickListener {
    private var listener: OnFragmentInteractionListener? = null
    private var viewAdapter: RecyclerView.Adapter<*>? = null
    private var viewManager: RecyclerView.LayoutManager? = null

    private var _binding: FragmentManageRemoteBinding? = null
    private lateinit var fragmentBinding: FragmentManageRemoteBinding

    private val configChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == Activity.RESULT_OK) {
            val uri = result?.data?.data
            try {
                val mIntent = Intent(Intent.ACTION_VIEW)
                mIntent.setDataAndType(uri, Strings.intentTypeJson)
                mIntent.setPackage(context?.packageName)
                startActivity(Intent.createChooser(mIntent, getString(R.string.title_file_chooser_remote_conf)))
            } catch (ex: Exception) {
                Log.e(javaClass.simpleName, "${ ex.message }")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if(_binding == null){
            _binding = FragmentManageRemoteBinding.inflate(inflater, container, false)
            fragmentBinding = _binding!!
            viewManager = LinearLayoutManager(context)
            viewAdapter = RemoteListAdapter(ESPUtilsApp.remotePropList,0)
            fragmentBinding.refreshLayout.refreshLayoutRecyclerView.apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
            fragmentBinding.famManageRemotes.setClosedOnTouchOutside(true)
            fragmentBinding.fabImportRemote.setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = Strings.intentTypeJson
                intent.addCategory(Intent.CATEGORY_OPENABLE)

                try {
                    configChooser.launch(Intent.createChooser(intent, getString(R.string.title_file_chooser_remote_conf)))
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.message_install_file_manager, Toast.LENGTH_SHORT).show()
                }
                fragmentBinding.famManageRemotes.close(true)
            }

            fragmentBinding.refreshLayout.refreshLayout.setOnRefreshListener {
                fragmentBinding.refreshLayout.refreshLayout.isRefreshing = true
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
                        fragmentBinding.refreshLayout.refreshLayout.isRefreshing = false
                    }
                }
            }

            fragmentBinding.famManageRemotes.setOnClickListener(this)
        }
        if(!fragmentBinding.famManageRemotes.isOpened)
            fragmentBinding.famManageRemotes.hideMenuButton(false)

        Handler(Looper.getMainLooper()).postDelayed({
            if(fragmentBinding.famManageRemotes.isMenuButtonHidden)
                fragmentBinding.famManageRemotes.showMenuButton(true)
            if(ESPUtilsApp.remotePropList.isEmpty())
                Handler(Looper.getMainLooper()).postDelayed({fragmentBinding.famManageRemotes.showMenu(true)}, 400)
        }, 400)
        return fragmentBinding.root
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
        val contentBinding = NewRemoteConfirmBinding.inflate(layoutInflater)
        val newRemoteDialog = AlertDialog.Builder(requireContext(), R.style.AppTheme_AlertDialog)
            .setTitle(R.string.enter_remote_details)
            .setView(contentBinding.root)
            .setIcon(R.drawable.icon_ir_remote)
            .setPositiveButton(R.string.done){ _, _ -> }
            .setNegativeButton(R.string.cancel){ _, _ -> }
            .create()

        newRemoteDialog.setOnShowListener {
            val devicePropList = arrayListOf<Any>(getString(R.string.select_device))
            devicePropList.addAll(ESPUtilsApp.devicePropList)
            contentBinding.deviceSelector.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, devicePropList)

            val btnDone = newRemoteDialog.getButton(DialogInterface.BUTTON_POSITIVE)

            btnDone.setOnClickListener {
                if(contentBinding.deviceSelector.selectedItemPosition == 0){
                    Toast.makeText(requireContext(), getString(R.string.message_device_not_selected_note), Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val selectedDevice = ESPUtilsApp.devicePropList[contentBinding.deviceSelector.selectedItemPosition - 1]

                var id = ("${contentBinding.vendorName.text.toString()} ${contentBinding.modelName.text.toString()}").lowercase(Locale.getDefault())
                    .replace(" ", "_").replace("\n", "").replace("/", "_")

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
                remoteProperties.remoteVendor = contentBinding.vendorName.text.toString()
                remoteProperties.remoteName = contentBinding.modelName.text.toString()
                remoteProperties.remoteID = id
                remoteProperties.description = contentBinding.remoteDesc.text.toString()
                remoteProperties.deviceConfigFileName = selectedDevice.deviceConfigFile.name
                ESPUtilsApp.remotePropList.add(remoteProperties)
                viewAdapter?.notifyItemInserted(ESPUtilsApp.remotePropList.size - 1)
                RemoteDialog(requireContext(), remoteProperties, RemoteDialog.MODE_VIEW_EDIT).show()
                newRemoteDialog.dismiss()
            }
        }
        newRemoteDialog.show()
        fragmentBinding.famManageRemotes.close(true)
    }
}

