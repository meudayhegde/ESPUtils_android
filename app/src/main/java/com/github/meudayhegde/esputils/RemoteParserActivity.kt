package com.github.meudayhegde.esputils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.github.meudayhegde.esputils.databinding.ImportRemoteActivityBinding
import com.github.meudayhegde.esputils.holders.DeviceProperties
import com.github.meudayhegde.esputils.holders.RemoteProperties
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter


class RemoteParserActivity : AppCompatActivity() {

    private var configFile: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainBinding = ImportRemoteActivityBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        when(getSharedPreferences(
            Strings.sharedPrefNameSettings, Context.MODE_PRIVATE).getInt(
            Strings.sharedPrefItemApplicationTheme, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            { 0 } else { 2 })
        ){
            1-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        setFinishOnTouchOutside(false)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val lWindowParams = WindowManager.LayoutParams()
        lWindowParams.copyFrom(window?.attributes)
        lWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        MainActivity.layoutParams.width = resources.displayMetrics.widthPixels
        MainActivity.layoutParams.height = resources.displayMetrics.heightPixels
        lWindowParams.width = MainActivity.layoutParams.width * 7 / 8
        window?.attributes = lWindowParams

        if(ESPUtilsApp.devicePropList.isEmpty()){
            val deviceConfigDir = ESPUtilsApp.getPrivateFile(Strings.nameDirDeviceConfig)
            deviceConfigDir.exists() or deviceConfigDir.mkdirs()
            for(file: File in deviceConfigDir.listFiles { _, name -> name?.endsWith(Strings.extensionJson)?: false } ?: emptyArray()){
                ESPUtilsApp.devicePropList.add(DeviceProperties(file))
            }
        }

        val devicePropList = arrayListOf<Any>(getString(R.string.select_device))
        devicePropList.addAll(ESPUtilsApp.devicePropList)
        mainBinding.selectDevice.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devicePropList)

        val action = intent.action

        var jsonObject = JSONObject()
        if (action!!.compareTo(Intent.ACTION_VIEW) == 0) {
            val scheme = intent.scheme
            val resolver = contentResolver

            try {
                when {
                    scheme!!.compareTo(ContentResolver.SCHEME_CONTENT) == 0 -> {
                        val uri = intent.data!!
                        val name = getContentName(resolver, uri)

                        Log.v("tag", "Content intent detected: " + action + " : " + intent.dataString + " : " + intent.type + " : " + name)

                        val input = resolver.openInputStream(uri)

                        jsonObject = onIntentRead(input)
                    }
                    scheme.compareTo(ContentResolver.SCHEME_FILE) == 0 -> {
                        val uri = intent.data
                        val name = uri!!.lastPathSegment

                        Log.v(
                            "tag",
                            "File intent detected: " + action + " : " + intent.dataString + " : " + intent.type + " : " + name
                        )
                        val input = resolver.openInputStream(uri)
                        jsonObject = onIntentRead(input)
                    }
                    scheme.compareTo("http") == 0 -> {
                        // TODO Import from HTTP!
                    }
                    scheme.compareTo("ftp") == 0 -> {
                        // TODO Import from FTP!
                    }
                }

            }catch(ex:Exception){
                mainBinding.progressStatus.visibility = View.GONE
                mainBinding.imvStatus.visibility = View.VISIBLE
                val drawable = ContextCompat.getDrawable(this, R.drawable.icon_cancel)
                DrawableCompat.setTint(drawable!!, Color.RED)
                mainBinding.imvStatus.setImageDrawable(drawable)
                mainBinding.messageText.visibility = View.VISIBLE
                mainBinding.messageText.text = getString(R.string.message_import_error)
            }
        }

        mainBinding.buttonCancel.setOnClickListener { finish() }
        mainBinding.buttonImport.setOnClickListener {
            if(mainBinding.selectDevice.selectedItemPosition == 0){
                Toast.makeText(this, getString(R.string.message_device_not_selected_note), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val selectedDevice = ESPUtilsApp.devicePropList[mainBinding.selectDevice.selectedItemPosition - 1]
            jsonObject.put(Strings.remotePropDevPropFileName, selectedDevice.deviceConfigFile.name)

            mainBinding.messageText.visibility = View.GONE
            mainBinding.progressStatus.visibility = View.VISIBLE
            try{
                val fileName= jsonObject.getString(Strings.remotePropFileName)
                var outFile = ESPUtilsApp.getPrivateFile(Strings.nameDirRemoteConfig, fileName)
                val parent = outFile.parentFile!!
                if(!parent.exists()) parent.mkdirs()
                var count = 1
                if(outFile.exists()){
                    AlertDialog.Builder(this)
                        .setTitle(R.string.confirm)
                        .setMessage(R.string.message_remote_conf_exists)
                        .setNegativeButton(R.string.cancel) { _, _ -> finish() }
                        .setNeutralButton(R.string.btn_text_keep_both){ dialog, _ ->
                            while(outFile.exists()) {
                                outFile = File(outFile.parent,
                                    fileName.removeSuffix(Strings.extensionJson) +
                                            "_" + count.toString() + Strings.extensionJson)
                                count++
                            }
                            outFile.createNewFile()
                            jsonObject.put(Strings.remotePropName, outFile.name)
                            writeFile(outFile, jsonObject)
                            configFile = outFile
                            onSuccess(mainBinding)
                            dialog.dismiss()
                        }
                        .setPositiveButton(R.string.btn_text_overwrite){ dialog, _ ->
                            outFile.delete()
                            outFile.createNewFile()
                            writeFile(outFile, jsonObject)
                            configFile = outFile
                            onSuccess(mainBinding)
                            dialog.dismiss()
                        }
                        .setCancelable(false)
                        .show()
                }else{
                    outFile.createNewFile()
                    writeFile(outFile, jsonObject)
                    configFile = outFile
                    onSuccess(mainBinding)
                }

            }catch(ex:Exception){
                mainBinding.progressStatus.visibility = View.GONE
                mainBinding.imvStatus.visibility = View.VISIBLE
                val drawable = ContextCompat.getDrawable(this, R.drawable.icon_cancel)
                DrawableCompat.setTint(drawable!!, Color.RED)
                mainBinding.imvStatus.setImageDrawable(drawable)
                mainBinding.messageText.visibility = View.VISIBLE
                mainBinding.messageText.text = getString(R.string.message_import_error)
            }
        }

    }

    private fun onSuccess(binding: ImportRemoteActivityBinding){
        ESPUtilsApp.showAd(this)
        binding.progressStatus.visibility = View.GONE
        binding.imvStatus.visibility = View.VISIBLE
        val drawable =ContextCompat.getDrawable(this, R.drawable.icon_check_circle)
        DrawableCompat.setTint(drawable!!, Color.GREEN)
        binding.imvStatus.setImageDrawable(drawable)

        binding.messageText.text = getString(R.string.message_import_success)

        if(MainActivity.activity != null && !MainActivity.activity!!.isDestroyed){
            if(configFile!=null){
                ESPUtilsApp.remotePropList.add(RemoteProperties(configFile!!,null))
                MainActivity.activity?.irFragment?.notifyDataChanged()
            }
            binding.buttonImport.text = getString(R.string.done)
            binding.buttonCancel.visibility = View.GONE
            binding.buttonImport.setOnClickListener {
                finish()
            }
        }else {
            binding.buttonImport.text = getString(R.string.start_app)
            binding.buttonCancel.text = getString(R.string.done)
            binding.buttonImport.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun writeFile(file:File,jsonObj:JSONObject){
        val outputStreamWriter = OutputStreamWriter(file.outputStream())
        outputStreamWriter.write(jsonObj.toString().replace("\n",""))
        outputStreamWriter.flush()
        outputStreamWriter.close()
    }

    @SuppressLint("Recycle")
    private fun getContentName(resolver: ContentResolver, uri: Uri): String? {
        val cursor = resolver.query(uri, null, null, null, null)
        cursor!!.moveToFirst()
        val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        return if (nameIndex >= 0) {
            cursor.getString(nameIndex)
        } else {
            null
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val lWindowParams = WindowManager.LayoutParams()
        lWindowParams.copyFrom(window?.attributes)
        lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT
        lWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes = lWindowParams

        MainActivity.layoutParams.width = resources.displayMetrics.widthPixels
        MainActivity.layoutParams.height = resources.displayMetrics.heightPixels
        lWindowParams.width = MainActivity.layoutParams.width * 7 / 8
        window?.attributes = lWindowParams
    }

    private fun onIntentRead(ins:InputStream?):JSONObject{
        val insr = InputStreamReader(ins)
        val out = TextUtils.join("", insr.readLines())

        insr.close()

        val jsonObj = JSONObject(out)
        jsonObj.getString(Strings.remotePropName)
        jsonObj.getString(Strings.remotePropVendor)
        jsonObj.getString(Strings.remotePropFileName)
        jsonObj.getString(Strings.remotePropID)

        val buttons = jsonObj.getJSONArray(Strings.remotePropButtonsArray)
        for(i in 0 until buttons.length()){
            val button = buttons.getJSONObject(i)
            button.getString(Strings.btnPropProtocol)
            button.getString(Strings.btnPropIrcode)
            button.getString(Strings.btnPropLength)
        }
        return jsonObj
    }
}
