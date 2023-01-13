package com.github.meudayhegde.esputils.ui.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.github.meudayhegde.esputils.ESPUtilsApp
import com.github.meudayhegde.esputils.MainActivity
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.holders.RemoteProperties
import com.github.meudayhegde.esputils.ui.dialogs.RemoteDialog
import java.io.OutputStreamWriter

class RemoteListAdapter(private val propList: ArrayList<RemoteProperties>, private val mode:Int) : RecyclerView.Adapter<RemoteListAdapter.RemoteListViewHolder>(){

    class RemoteListViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): RemoteListViewHolder {
        val cardView = LayoutInflater.from(parent.context).inflate(R.layout.manage_remote_list_item, parent, false) as CardView
        if(mode == RemoteDialog.MODE_SELECT_BUTTON){
            cardView.findViewById<ImageView>(R.id.icon_share).visibility = View.GONE
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
                onShareClick(context, prop)
            }

            holder.cardView.setOnLongClickListener {
                onCardLongClick(holder.cardView, prop)
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

    private fun onShareClick(context: Context, prop: RemoteProperties) {
        val fileToShare = ESPUtilsApp.getExternalCache(prop.fileName)
        if (fileToShare.exists())
            fileToShare.delete()
        fileToShare.createNewFile()
        val writer = OutputStreamWriter(fileToShare.outputStream())
        writer.write(prop.toString())
        writer.flush()
        writer.close()

        val uri = FileProvider.getUriForFile(context,
            context.applicationContext.packageName + Strings.extensionProvider,
            fileToShare
        )

        val intent = ShareCompat.IntentBuilder.from(MainActivity.activity as Activity)
            .setType(Strings.intentTypeJson)
            .setSubject(context.getString(R.string.share_file))
            .setStream(uri)
            .setChooserTitle(context.getString(R.string.title_share_remote_prop))
            .createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        ContextCompat.startActivity(
            context,
            Intent.createChooser(intent, context.getString(R.string.share_file)),
            null
        )
    }

    private fun onCardLongClick(card:CardView, prop:RemoteProperties): Boolean{
        val remoteEditDialog = AlertDialog.Builder(card.context, R.style.AppTheme_AlertDialog)
            .setTitle(R.string.edit_remote_information)
            .setView(R.layout.new_remote_confirm)
            .setIcon(R.drawable.icon_ir_remote)
            .setPositiveButton(R.string.done){ _, _ -> }
            .setNegativeButton(R.string.cancel){ _, _ -> }
            .setNeutralButton(R.string.delete_remote){ _, _ -> }
            .create()

        remoteEditDialog.setOnShowListener {
            val vendor = remoteEditDialog.findViewById<TextInputEditText>(R.id.vendor_name)
            vendor?.setText(prop.remoteVendor)
            val name = remoteEditDialog.findViewById<TextInputEditText>(R.id.model_name)
            name?.setText(prop.remoteName)
            val desc = remoteEditDialog.findViewById<TextInputEditText>(R.id.remote_desc)
            desc?.setText(prop.description)

            val spinner = remoteEditDialog.findViewById<Spinner>(R.id.select_device)
            val devicePropList = arrayListOf<Any>(card.context.getString(R.string.select_device))
            devicePropList.addAll(ESPUtilsApp.devicePropList)
            spinner?.adapter = ArrayAdapter(card.context, android.R.layout.simple_list_item_1, devicePropList)

            spinner?.setSelection(ESPUtilsApp.devicePropList.indexOf(prop.deviceProperties) + 1)

            val btnDone = remoteEditDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            val btnDelete = remoteEditDialog.getButton(DialogInterface.BUTTON_NEUTRAL)

            btnDone.setOnClickListener {
                if((spinner?.selectedItemPosition?: 0) == 0){
                    Toast.makeText(card.context, card.context.getString(R.string.message_device_not_selected_note), Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val selectedDevice = ESPUtilsApp.devicePropList[(spinner?.selectedItemPosition ?: 0) - 1]

                prop.remoteVendor = vendor?.text.toString()
                prop.remoteName = name?.text.toString()
                prop.description = desc?.text.toString()
                prop.deviceConfigFileName = selectedDevice.deviceConfigFile.name
                setViewProps(card, prop)
                remoteEditDialog.dismiss()
            }

            btnDelete.setOnClickListener {
                AlertDialog.Builder(remoteEditDialog.context, R.style.AppTheme_AlertDialog)
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(R.string.delete) { _, _ ->
                        prop.remoteConfigFile.delete()
                        val index = propList.indexOf(prop)
                        propList.remove(prop)
                        notifyItemRemoved(index)
                        remoteEditDialog.dismiss()
                    }
                    .setMessage(ESPUtilsApp.getString(R.string.message_dialog_delete_remote, prop.remoteVendor, prop.remoteName))
                    .setTitle(R.string.confirm_deletion)
                    .setIcon(R.drawable.icon_delete)
                    .show()
            }
        }
        remoteEditDialog.show()
        return true
    }
}