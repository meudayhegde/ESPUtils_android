package com.irware.remote.ui.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.irware.remote.ESPUtils
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.ui.dialogs.RemoteDialog
import java.io.File
import java.io.OutputStreamWriter
import kotlin.math.min

class RemoteListAdapter(private val propList: ArrayList<RemoteProperties>, private val mode:Int) : RecyclerView.Adapter<RemoteListAdapter.RemoteListViewHolder>(){

    class RemoteListViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): RemoteListViewHolder {
        val cardView = LayoutInflater.from(parent.context).inflate(R.layout.manage_remote_list_item, parent, false) as CardView
        if(mode == RemoteDialog.MODE_SELECT_BUTTON){
            cardView.findViewById<ImageView>(R.id.icon_share).visibility =View.GONE
        }
        when (cardView.context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> { }
            Configuration.UI_MODE_NIGHT_NO -> { }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> { }
        }
        return RemoteListViewHolder(cardView)
    }

    @SuppressLint("InflateParams")
    override fun onBindViewHolder(holder: RemoteListViewHolder, position: Int) {
        val prop = propList[position]
        setViewProps(holder.cardView, prop)
        holder.cardView.findViewById<TextView>(R.id.btn_count).text =
            prop.getButtons().length().toString()
        val context = holder.cardView.context
        holder.cardView.visibility = View.GONE
        Handler(Looper.getMainLooper()).postDelayed({
            holder.cardView.visibility = View.VISIBLE
            holder.cardView.startAnimation(
                AnimationUtils.loadAnimation(
                    context,
                    R.anim.anim_button_show
                )
            )
        }, position * 20L)

        val share = holder.cardView.findViewById<ImageView>(R.id.icon_share)
        if (mode != RemoteDialog.MODE_SELECT_BUTTON) {
            share.setOnClickListener {
                onShareClick(context,prop)
            }

            holder.cardView.setOnLongClickListener {
                onCardLongClick(holder.cardView,prop)
            }

            holder.cardView.setOnClickListener {
                RemoteDialog(holder.cardView.context, prop, RemoteDialog.MODE_VIEW_EDIT).show()
            }
        }else{
            holder.cardView.setOnClickListener {
                RemoteDialog(holder.cardView.context, prop, RemoteDialog.MODE_SELECT_BUTTON).show()
            }
        }
    }

    private fun setViewProps(cardView: CardView, prop: RemoteProperties){
        cardView.findViewById<TextView>(R.id.mac_addr).text = prop.remoteVendor
        cardView.findViewById<TextView>(R.id.model_name_text).text = prop.remoteName
        cardView.findViewById<TextView>(R.id.remote_desc).text = prop.description
        cardView.getChildAt(0).background = ContextCompat.getDrawable(cardView.context, if(prop.deviceProperties.isConnected) R.drawable.round_corner_success else R.drawable.round_corner_error)
    }

    override fun getItemCount() = propList.size

    private fun onShareClick(context: Context,prop:RemoteProperties) {
        val parent = context.filesDir
        if (parent!!.exists() and parent.isFile) parent.delete()
        if (!parent.exists()) parent.mkdirs()
        val fileToShare = File(parent.absolutePath, prop.remoteConfigFile.name)
        if (fileToShare.exists())
            fileToShare.delete()
        fileToShare.createNewFile()
        val writer = OutputStreamWriter(fileToShare.outputStream())
        writer.write(prop.toString())
        writer.flush()
        writer.close()

        val uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            fileToShare
        )

        val intent = ShareCompat.IntentBuilder.from(MainActivity.activity as Activity)
            .setType("application/json")
            .setSubject("Share File")
            .setStream(uri)
            .setChooserTitle("Share Remote Controller")
            .createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ContextCompat.startActivity(
            context,
            Intent.createChooser(intent, "Share File"),
            null
        )
    }

    @SuppressLint("InflateParams")
    private fun onCardLongClick(card:CardView, prop:RemoteProperties):Boolean{
        val inputLayout = LayoutInflater.from(card.context).inflate(R.layout.new_remote_confirm, null) as ScrollView
        val vendor = inputLayout.findViewById<TextInputEditText>(R.id.vendor_name)
        vendor.setText(prop.remoteVendor)
        val name = inputLayout.findViewById<TextInputEditText>(R.id.model_name)
        name.setText(prop.remoteName)
        val desc = inputLayout.findViewById<TextInputEditText>(R.id.remote_desc)
        desc.setText(prop.description)

        val btnDelete = inputLayout.findViewById<Button>(R.id.delete_remote)
        val btnCancel = inputLayout.findViewById<Button>(R.id.cancel)
        val btnFinish = inputLayout.findViewById<Button>(R.id.button_done)
        inputLayout.findViewById<TextView>(R.id.title_new_remote_confirm).text =
            card.context.getString(
                R.string.edit_remote_informtion
            )
        val spinner = inputLayout.findViewById<Spinner>(R.id.select_device)

        val devicePropList = arrayListOf<Any>(card.context.getString(R.string.select_device))
        devicePropList.addAll(ESPUtils.devicePropList)
        spinner.adapter = ArrayAdapter(card.context, android.R.layout.simple_list_item_1, devicePropList)

        spinner.setSelection(ESPUtils.devicePropList.indexOf(prop.deviceProperties) + 1)

        val dialog = Dialog(card.context)
        dialog.setContentView(inputLayout)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnFinish.setOnClickListener {
            if(spinner.selectedItemPosition == 0){
                Toast.makeText(card.context, card.context.getString(R.string.device_not_selected_note), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val selectedDevice = ESPUtils.devicePropList[spinner.selectedItemPosition - 1]

            prop.remoteVendor = vendor.text.toString()
            prop.remoteName = name.text.toString()
            prop.description = desc.text.toString()
            prop.deviceConfigFileName = selectedDevice.deviceConfigFile.name
            setViewProps(card, prop)
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            val icon = ContextCompat.getDrawable(dialog.context, R.drawable.icon_delete)
            DrawableCompat.setTint(icon!!, Color.RED)
            AlertDialog.Builder(dialog.context)
                .setNegativeButton("No,Quit") { dialog, _ -> dialog.dismiss() }
                .setPositiveButton("Yes,Delete") { _, _ ->
                    prop.remoteConfigFile.delete()
                    val index = propList.indexOf(prop)
                    propList.remove(prop)
                    notifyItemRemoved(index)
                    dialog.dismiss()
                }
                .setMessage("This action cannot be unDone\nAre you sure you want to delete " + prop.remoteVendor + " " + prop.remoteName + " ?")
                .setTitle("Confirm Deletion")
                .setIcon(icon)
                .show()
        }

        dialog.show()

        val width = min(MainActivity.layoutParams.width, MainActivity.layoutParams.height)
        dialog.window?.setLayout(width - width / 8, WindowManager.LayoutParams.WRAP_CONTENT)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return true
    }
}