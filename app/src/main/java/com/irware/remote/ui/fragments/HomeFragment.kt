package com.irware.remote.ui.fragments

import android.annotation.SuppressLint
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
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.android.material.textfield.TextInputEditText
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.ui.adapters.RemoteListAdapter
import com.irware.remote.ui.dialogs.RemoteDialog
import java.io.File
import kotlin.math.min


class HomeFragment : androidx.fragment.app.Fragment(), View.OnClickListener {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var rootView:RelativeLayout? = null

    @SuppressLint("DefaultLocale", "InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(rootView == null){
            rootView = inflater.inflate(R.layout.fragment_manage_remote, container, false) as RelativeLayout
            viewManager = LinearLayoutManager(context)
            viewAdapter = RemoteListAdapter(MainActivity.remotePropList,0)
            recyclerView = rootView!!.findViewById<RecyclerView>(R.id.manage_remotes_recycler_view).apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
            rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_remotes).setClosedOnTouchOutside(true)
            rootView!!.findViewById<FloatingActionButton>(R.id.fab_import_remote).setOnClickListener {
                MainActivity.activity?.startConfigChooser()
                rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_remotes).close(true)
            }

            val refreshLayout = rootView!!.findViewById<SwipeRefreshLayout>(R.id.refresh_layout)
            refreshLayout.setOnRefreshListener {
                refreshLayout.isRefreshing = true
                Thread{
                    MainActivity.remotePropList.clear()
                    val files = File(MainActivity.configPath).listFiles { pathname ->
                        pathname!!.isFile and (pathname.name.endsWith(
                            ".json",
                            true
                        )) and pathname.canWrite()
                    }
                    files.forEach {
                        MainActivity.remotePropList.add(RemoteProperties(it, null))
                    }
                    MainActivity.activity?.runOnUiThread{
                        viewAdapter.notifyDataSetChanged()
                        refreshLayout.isRefreshing = false
                    }
                }.start()
            }

            rootView!!.findViewById<FloatingActionButton>(R.id.fab_new_remote).setOnClickListener(this)
        }
        val manageMenu = rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_remotes)
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
        viewAdapter.notifyDataSetChanged()
    }

    @SuppressLint("InflateParams", "DefaultLocale")
    override fun onClick(v: View?) {
        val inputLayout = layoutInflater.inflate(R.layout.new_remote_confirm, null) as ScrollView
        val btnEdit = inputLayout.findViewById<Button>(R.id._edit_buttons)
        val btnDelete = inputLayout.findViewById<Button>(R.id.delete_remote)
        val btnCancel = inputLayout.findViewById<Button>(R.id.cancel)
        val btnFinish = inputLayout.findViewById<Button>(R.id.button_done)
        btnDelete.visibility = View.GONE
        btnEdit.visibility = View.GONE
        inputLayout.findViewById<TextView>(R.id.title_new_remote_confirm).text = getString(R.string.enter_remote_details)

        val dialog = Dialog(context!!)
        dialog.setContentView(inputLayout)
        btnCancel.setOnClickListener { dialog.cancel() }
        btnFinish.setOnClickListener {
            val vendor = inputLayout.findViewById<TextInputEditText>(R.id.vendor_name).text.toString()
            val model = inputLayout.findViewById<TextInputEditText>(R.id.model_name).text.toString()
            var id = ("$vendor $model").toLowerCase().replace(" ", "_").replace("\n", "").replace("/","_")

            val desc = inputLayout.findViewById<TextInputEditText>(R.id.remote_desc)
            var configFile = File(MainActivity.configPath + File.separator + id + ".json")
            var incr = 1
            while(configFile.exists()) {
                configFile = File(MainActivity.configPath + File.separator + id + "_" + incr + ".json")
                incr++
            }
            if(incr>1) id+="_"+(incr-1)
            if (!configFile.exists()) configFile.createNewFile()
            val remoteProperties = RemoteProperties(configFile, null)


            remoteProperties.fileName = configFile.name
            remoteProperties.remoteVendor = vendor
            remoteProperties.remoteName = model
            remoteProperties.remoteID = id
            remoteProperties.description = desc.text.toString()
            MainActivity.remotePropList.add(remoteProperties)
            viewAdapter.notifyDataSetChanged()
            RemoteDialog(context!!, remoteProperties,RemoteDialog.MODE_EDIT).show()
            dialog.dismiss()
        }
        dialog.show()
        val width = min(MainActivity.size.x,MainActivity.size.y)
        dialog.window?.setLayout(width - width/8,WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_remotes).close(true)
    }
}

