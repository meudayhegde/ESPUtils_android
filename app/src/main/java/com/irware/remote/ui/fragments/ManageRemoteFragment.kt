package com.irware.remote.ui.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.clans.fab.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.ui.dialogs.RemoteDialog
import java.io.File

class ManageRemoteFragment : androidx.fragment.app.Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var rootView:RelativeLayout? = null


    @SuppressLint("DefaultLocale")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(rootView == null){
            rootView = inflater.inflate(R.layout.fragment_manage_remote, container, false) as RelativeLayout

            viewManager = LinearLayoutManager(context)
            viewAdapter = RemoteListAdapter(MainActivity.remotePropList)

            recyclerView = rootView!!.findViewById<RecyclerView>(R.id.manage_remotes_recycler_view).apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }

            rootView!!.findViewById<FloatingActionButton>(R.id.fab_new_remote).setOnClickListener {
                val inputLayout = layoutInflater.inflate(R.layout.new_remote_confirm, null) as LinearLayout
                object:AlertDialog(context){
                    private val buttonPositive:Button
                    private val buttonNegative:Button
                    init {
                        setView(inputLayout)
                        setTitle("Enter Remote Details")
                        setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel") { dialog, _ -> dialog?.dismiss() }
                        setButton(DialogInterface.BUTTON_POSITIVE,"Create") { dialog, _ ->}
                        show()
                        buttonNegative = getButton(DialogInterface.BUTTON_NEGATIVE)
                        buttonPositive = getButton(DialogInterface.BUTTON_POSITIVE)

                        buttonPositive.setOnClickListener{
                            val vendor = inputLayout.findViewById<TextInputEditText>(R.id.vendor_name).text.toString()
                            val model = inputLayout.findViewById<TextInputEditText>(R.id.model_name).text.toString()
                            var id = ("$vendor $model").toLowerCase().replace(" ", "_").replace("\n", "")

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

                            remoteProperties.remoteVendor = vendor
                            remoteProperties.remoteName = model
                            remoteProperties.remoteID = id
                            remoteProperties.description = desc.text.toString()
                            MainActivity.remotePropList.add(remoteProperties)
                            RemoteDialog(context, remoteProperties,RemoteDialog.MODE_EDIT).show()
                            dismiss()
                        }
                    }
                }
            }
        }
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

}


class RemoteListAdapter(private val propList: ArrayList<RemoteProperties>) : RecyclerView.Adapter<RemoteListAdapter.MyViewHolder>(){

    class MyViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.manage_remote_list_item, parent, false) as CardView

        return MyViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val prop = propList[position]
        setViewProps(holder.cardView,prop)
        holder.cardView.findViewById<TextView>(R.id.btn_count).text = prop.getButtons().length().toString()
        holder.cardView.setOnClickListener {
            val inputLayout = LayoutInflater.from(holder.cardView.context).inflate(R.layout.new_remote_confirm, null) as LinearLayout
            val vendor = inputLayout.findViewById<TextInputEditText>(R.id.vendor_name)
            vendor.setText(prop.remoteVendor)
            val name = inputLayout.findViewById<TextInputEditText>(R.id.model_name)
            name.setText(prop.remoteName)
            val desc =inputLayout.findViewById<TextInputEditText>(R.id.remote_desc)
            desc.setText(prop.description)
            AlertDialog.Builder(holder.cardView.context).setTitle("Edit remote Information").setView(inputLayout)
                .setNegativeButton("Cancel"){ dialog, _ -> dialog.dismiss()}
                .setPositiveButton("Done"){ _, _ ->
                    prop.remoteVendor = vendor.text.toString()
                    prop.remoteName = name.text.toString()
                    prop.description = desc.text.toString()
                    setViewProps(holder.cardView,prop)
                }
                .setNeutralButton("Edit Buttons"){ _, _ ->
                    prop.remoteVendor = vendor.text.toString()
                    prop.remoteName = name.text.toString()
                    prop.description = desc.text.toString()
                    setViewProps(holder.cardView,prop)
                    RemoteDialog(holder.cardView.context, prop,RemoteDialog.MODE_EDIT).show()
                }.show()
        }

    }

    private fun setViewProps(cardView:CardView,prop:RemoteProperties){
        cardView.findViewById<TextView>(R.id.vendor_name_text).text = prop.remoteVendor
        cardView.findViewById<TextView>(R.id.model_name_text).text = prop.remoteName
        cardView.findViewById<TextView>(R.id.remote_desc).text = prop.description
    }

    override fun getItemCount() = propList.size

}
