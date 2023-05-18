package com.github.meudayhegde.esputils.ui.adapters

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.github.meudayhegde.esputils.ESPUtilsApp
import com.github.meudayhegde.esputils.MainActivity
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.databinding.ManageRemoteListItemBinding
import com.github.meudayhegde.esputils.databinding.NewRemoteConfirmBinding
import com.github.meudayhegde.esputils.holders.RemoteProperties
import com.github.meudayhegde.esputils.ui.dialogs.RemoteDialog
import java.io.OutputStreamWriter

class RemoteListAdapter(private val propList: ArrayList<RemoteProperties>, private val mode: Int) :
    RecyclerView.Adapter<RemoteListAdapter.RemoteListViewHolder>() {

    class RemoteListViewHolder(val viewBinding: ManageRemoteListItemBinding, mode: Int) :
        RecyclerView.ViewHolder(viewBinding.root) {
        val context: Context = viewBinding.root.context
        val btnCount = viewBinding.btnCount
        val iconShare = viewBinding.iconShare

        init {
            if (mode == RemoteDialog.MODE_SELECT_BUTTON) {
                iconShare.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemoteListViewHolder {
        return RemoteListViewHolder(
            ManageRemoteListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            mode
        )
    }

    override fun onBindViewHolder(holder: RemoteListViewHolder, position: Int) {
        val prop = propList[position]
        setViewProps(holder.viewBinding, prop)
        holder.btnCount.text = prop.getButtons().length().toString()

        holder.viewBinding.root.visibility = View.GONE
        Handler(Looper.getMainLooper()).postDelayed({
            holder.viewBinding.root.visibility = View.VISIBLE
            holder.viewBinding.root.startAnimation(
                AnimationUtils.loadAnimation(
                    holder.context, R.anim.anim_button_show
                )
            )
        }, position * 20L)

        if (mode != RemoteDialog.MODE_SELECT_BUTTON) {
            holder.iconShare.setOnClickListener {
                onShareClick(holder.context, prop)
            }

            holder.viewBinding.root.setOnLongClickListener {
                onCardLongClick(holder.viewBinding, prop)
            }

            holder.viewBinding.root.setOnClickListener {
                RemoteDialog(holder.context, prop, RemoteDialog.MODE_VIEW_EDIT).show()
            }
        } else {
            holder.viewBinding.root.setOnClickListener {
                RemoteDialog(holder.context, prop, RemoteDialog.MODE_SELECT_BUTTON).show()
            }
        }
    }

    private fun setViewProps(binding: ManageRemoteListItemBinding, prop: RemoteProperties) {
        binding.macAddr.text = prop.remoteVendor
        binding.modelNameText.text = prop.remoteName
        binding.remoteDesc.text = prop.description
        binding.container.background = ContextCompat.getDrawable(
            binding.root.context,
            if (prop.deviceProperties.isConnected) R.drawable.round_corner_success
            else R.drawable.round_corner_error
        )
    }

    override fun getItemCount() = propList.size

    private fun onShareClick(context: Context, prop: RemoteProperties) {
        val fileToShare = ESPUtilsApp.getExternalCache(prop.fileName)
        if (fileToShare.exists()) fileToShare.delete()
        fileToShare.createNewFile()
        val writer = OutputStreamWriter(fileToShare.outputStream())
        writer.write(prop.toString())
        writer.flush()
        writer.close()

        val uri = FileProvider.getUriForFile(
            context, context.applicationContext.packageName + Strings.extensionProvider, fileToShare
        )

        val intent = ShareCompat.IntentBuilder(context).setType(Strings.intentTypeJson)
            .setSubject(context.getString(R.string.share_file)).setStream(uri)
            .setChooserTitle(context.getString(R.string.title_share_remote_prop))
            .createChooserIntent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        ContextCompat.startActivity(
            context, Intent.createChooser(intent, context.getString(R.string.share_file)), null
        )
    }

    private fun onCardLongClick(
        itemBinding: ManageRemoteListItemBinding, prop: RemoteProperties
    ): Boolean {
        val context = itemBinding.root.context
        val dialogBinding = NewRemoteConfirmBinding.inflate(LayoutInflater.from(context))
        val remoteEditDialog = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
            .setTitle(R.string.edit_remote_information).setView(dialogBinding.root)
            .setIcon(R.drawable.icon_ir_remote).setPositiveButton(R.string.done) { _, _ -> }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setNeutralButton(R.string.delete_remote) { _, _ -> }.create()

        remoteEditDialog.setOnShowListener {
            dialogBinding.vendorName.setText(prop.remoteVendor)
            dialogBinding.modelName.setText(prop.remoteName)
            dialogBinding.remoteDesc.setText(prop.description)

            val devicePropList = arrayListOf<Any>(context.getString(R.string.select_device))
            devicePropList.addAll(ESPUtilsApp.devicePropList)
            dialogBinding.deviceSelector.adapter =
                ArrayAdapter(context, android.R.layout.simple_list_item_1, devicePropList)

            dialogBinding.deviceSelector.setSelection(ESPUtilsApp.devicePropList.indexOf(prop.deviceProperties) + 1)

            val btnDone = remoteEditDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            val btnDelete = remoteEditDialog.getButton(DialogInterface.BUTTON_NEUTRAL)

            btnDone.setOnClickListener {
                if (dialogBinding.deviceSelector.selectedItemPosition == 0) {
                    Toast.makeText(
                        context, R.string.message_device_not_selected_note, Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
                val selectedDevice =
                    ESPUtilsApp.devicePropList[dialogBinding.deviceSelector.selectedItemPosition - 1]

                prop.remoteVendor = dialogBinding.vendorName.text.toString()
                prop.remoteName = dialogBinding.modelName.text.toString()
                prop.description = dialogBinding.remoteDesc.text.toString()
                prop.deviceConfigFileName = selectedDevice.deviceConfigFile.name
                setViewProps(itemBinding, prop)
                ESPUtilsApp.showAd(context as MainActivity)
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
                        ESPUtilsApp.showAd(context as MainActivity)
                        remoteEditDialog.dismiss()
                    }.setMessage(
                        ESPUtilsApp.getString(
                            R.string.message_dialog_delete_remote,
                            prop.remoteVendor,
                            prop.remoteName
                        )
                    ).setTitle(R.string.confirm_deletion).setIcon(R.drawable.icon_delete).show()
            }
        }
        remoteEditDialog.show()
        return true
    }
}