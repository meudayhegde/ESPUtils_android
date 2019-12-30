package com.irware.remote.ui.adapters

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.ui.dialogs.RemoteDialog
import java.io.File
import java.io.OutputStreamWriter
import kotlin.math.min

class RemoteListAdapter(private val propList: ArrayList<RemoteProperties>, private val mode:Int) : RecyclerView.Adapter<RemoteListAdapter.MyViewHolder>(){

    class MyViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        val cardView = LayoutInflater.from(parent.context).inflate(R.layout.manage_remote_list_item, parent, false) as CardView
        if(mode == RemoteDialog.MODE_SELECT_BUTTON){
            cardView.findViewById<ImageView>(R.id.icon_share).visibility =View.GONE
        }
        return MyViewHolder(cardView)
    }

    @SuppressLint("InflateParams")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val prop = propList[position]
        setViewProps(holder.cardView, prop)
        holder.cardView.findViewById<TextView>(R.id.btn_count).text =
            prop.getButtons().length().toString()
        val context = holder.cardView.context
        holder.cardView.visibility = View.GONE
        Handler().postDelayed({
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
                RemoteDialog(holder.cardView.context, prop, RemoteDialog.MODE_VIEW_ONLY).show()
            }
        }else{
            holder.cardView.setOnClickListener {
                RemoteDialog(holder.cardView.context, prop, RemoteDialog.MODE_SELECT_BUTTON).show()
            }
        }
    }

    private fun setViewProps(cardView: CardView, prop: RemoteProperties){
        cardView.findViewById<TextView>(R.id.vendor_name_text).text = prop.remoteVendor
        cardView.findViewById<TextView>(R.id.model_name_text).text = prop.remoteName
        cardView.findViewById<TextView>(R.id.remote_desc).text = prop.description
    }

    override fun getItemCount() = propList.size

    private fun onShareClick(context: Context,prop:RemoteProperties) {
        val parent = context.externalCacheDir
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

        val intent = ShareCompat.IntentBuilder.from(MainActivity.activity)
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

        val btnEdit = inputLayout.findViewById<Button>(R.id._edit_buttons)
        val btnDelete = inputLayout.findViewById<Button>(R.id.delete_remote)
        val btnCancel = inputLayout.findViewById<Button>(R.id.cancel)
        val btnFinish = inputLayout.findViewById<Button>(R.id.button_done)
        inputLayout.findViewById<TextView>(R.id.title_new_remote_confirm).text =
            card.context.getString(
                R.string.edit_remote_informtion
            )
        val dialog = Dialog(card.context)
        dialog.setContentView(inputLayout)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnFinish.setOnClickListener {
            prop.remoteVendor = vendor.text.toString()
            prop.remoteName = name.text.toString()
            prop.description = desc.text.toString()
            setViewProps(card, prop)
            dialog.dismiss()
        }
        btnEdit.setOnClickListener {
            prop.remoteVendor = vendor.text.toString()
            prop.remoteName = name.text.toString()
            prop.description = desc.text.toString()
            setViewProps(card, prop)
            RemoteDialog(card.context, prop, RemoteDialog.MODE_EDIT).show()
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            val icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                dialog.context.getDrawable(R.drawable.icon_delete)
            } else {
                @Suppress("DEPRECATION")
                dialog.context.resources.getDrawable(R.drawable.icon_delete)
            }
            DrawableCompat.setTint(icon!!, Color.RED)
            AlertDialog.Builder(dialog.context)
                .setNegativeButton("No,Quit") { dialog, _ -> dialog.dismiss() }
                .setPositiveButton("Yes,Delete") { _, _ ->
                    prop.remoteConfigFile.delete()
                    propList.remove(prop)
                    notifyDataSetChanged()
                    dialog.dismiss()
                }
                .setMessage("This action cannot be unDone\nAre you sure you want to delete " + prop.remoteVendor + " " + prop.remoteName + " ?")
                .setTitle("Confirm Deletion")
                .setIcon(icon)
                .show()
        }

        dialog.show()

        val width = min(MainActivity.size.x, MainActivity.size.y)
        dialog.window?.setLayout(width - width / 8, WindowManager.LayoutParams.WRAP_CONTENT)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return true
    }
}