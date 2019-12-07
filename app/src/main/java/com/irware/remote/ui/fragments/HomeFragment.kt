package com.irware.remote.ui.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.clans.fab.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.ui.dialogs.RemoteDialog
import java.io.File
import java.io.OutputStreamWriter


class HomeFragment : androidx.fragment.app.Fragment() {
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

            rootView!!.findViewById<FloatingActionButton>(R.id.fab_import_remote).setOnClickListener {
                MainActivity.activity?.startConfigChooser()
            }

            rootView!!.findViewById<FloatingActionButton>(R.id.fab_new_remote).setOnClickListener {
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
                val lWindowParams = WindowManager.LayoutParams()
                lWindowParams.copyFrom(dialog.window?.attributes)
                lWindowParams.width = MainActivity.size.x - MainActivity.size.x/8
                lWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                dialog.show()
                dialog.window?.attributes = lWindowParams
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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

    fun notifyDataChanged(){
        viewAdapter.notifyDataSetChanged()
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
        val context = holder.cardView.context

        val share = holder.cardView.findViewById<ImageView>(R.id.icon_share)
        share.setOnClickListener {

            val parent = context.externalCacheDir
            if(parent!!.exists() and parent.isDirectory) parent.delete()
            if(!parent.exists()) parent.mkdirs()
            val fileToShare =  File(parent.absolutePath,prop.fileName)
            if(fileToShare.exists())
                fileToShare.delete()
            fileToShare.createNewFile()
            val writer = OutputStreamWriter(fileToShare.outputStream())
            writer.write(prop.toString())
            writer.flush()
            writer.close()

            val uri = FileProvider.getUriForFile(context,  context.applicationContext.packageName + ".provider", fileToShare)

            val intent = ShareCompat.IntentBuilder.from(MainActivity.activity)
                .setType("application/json")
                .setSubject("Share File")
                .setStream(uri)
                .setChooserTitle("Share Remote Controller")
                .createChooserIntent()
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(holder.cardView.context,Intent.createChooser(intent, "Share File"),null)

        }

        holder.cardView.setOnLongClickListener {
            val inputLayout = LayoutInflater.from(holder.cardView.context).inflate(R.layout.new_remote_confirm, null) as ScrollView
            val vendor = inputLayout.findViewById<TextInputEditText>(R.id.vendor_name)
            vendor.setText(prop.remoteVendor)
            val name = inputLayout.findViewById<TextInputEditText>(R.id.model_name)
            name.setText(prop.remoteName)
            val desc =inputLayout.findViewById<TextInputEditText>(R.id.remote_desc)
            desc.setText(prop.description)

            val btnEdit = inputLayout.findViewById<Button>(R.id._edit_buttons)
            val btnDelete = inputLayout.findViewById<Button>(R.id.delete_remote)
            val btnCancel = inputLayout.findViewById<Button>(R.id.cancel)
            val btnFinish = inputLayout.findViewById<Button>(R.id.button_done)
            inputLayout.findViewById<TextView>(R.id.title_new_remote_confirm).text = holder.cardView.context.getString(
                            R.string.edit_remote_informtion)
            val dialog = Dialog(holder.cardView.context)
            dialog.setContentView(inputLayout)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            btnCancel.setOnClickListener { dialog.dismiss() }
            btnFinish.setOnClickListener {
                prop.remoteVendor = vendor.text.toString()
                prop.remoteName = name.text.toString()
                prop.description = desc.text.toString()
                setViewProps(holder.cardView,prop)
                dialog.dismiss()
            }
            btnEdit.setOnClickListener {
                prop.remoteVendor = vendor.text.toString()
                prop.remoteName = name.text.toString()
                prop.description = desc.text.toString()
                setViewProps(holder.cardView,prop)
                RemoteDialog(holder.cardView.context, prop,RemoteDialog.MODE_EDIT).show()
                dialog.dismiss()
            }

            btnDelete.setOnClickListener {
                val icon = dialog.context.resources.getDrawable(R.drawable.icon_delete)
                DrawableCompat.setTint(icon,Color.RED)
                AlertDialog.Builder(dialog.context).setNegativeButton("No,Quit"){ dialog, _ -> dialog.dismiss()}
                    .setPositiveButton("Yes,Delete"){_,_ ->
                        File(MainActivity.configPath+File.separator+prop.fileName).delete()
                        propList.remove(prop)
                        notifyDataSetChanged()
                        dialog.dismiss()
                    }.setMessage("This action cannot be unDone\nAre you sure you want to delete "+prop.remoteVendor+" "+prop.remoteName+" ?")
                    .setTitle("Confirm Deletion")
                    .setIcon(icon)
                    .show()
            }

            val lWindowParams = WindowManager.LayoutParams()
            lWindowParams.copyFrom(dialog.window?.attributes)
            lWindowParams.width = MainActivity.size.x - MainActivity.size.x/8
            lWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            dialog.show()
            dialog.window?.attributes = lWindowParams

            true
        }

        holder.cardView.setOnClickListener {
            RemoteDialog(holder.cardView.context,prop,RemoteDialog.MODE_VIEW_ONLY).show()
        }

    }

    private fun setViewProps(cardView:CardView,prop:RemoteProperties){
        cardView.findViewById<TextView>(R.id.vendor_name_text).text = prop.remoteVendor
        cardView.findViewById<TextView>(R.id.model_name_text).text = prop.remoteName
        cardView.findViewById<TextView>(R.id.remote_desc).text = prop.description
    }

    override fun getItemCount() = propList.size

}
