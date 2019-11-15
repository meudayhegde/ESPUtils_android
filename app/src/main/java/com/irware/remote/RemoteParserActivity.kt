package com.irware.remote

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
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
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter


class RemoteParserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.import_remote_activity)
        setFinishOnTouchOutside(false)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val lWindowParams = WindowManager.LayoutParams()
        lWindowParams.copyFrom(window?.attributes)
        lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT
        lWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes = lWindowParams

        windowManager.defaultDisplay.getSize(MainActivity.size)
        lWindowParams.width = MainActivity.size.x - MainActivity.size.x/8
        window?.attributes = lWindowParams

        val msg = findViewById<TextView>(R.id.message_text)
        val progress = findViewById<ProgressBar>(R.id.progress_status)

        val imV =findViewById<ImageView>(R.id.imv_status)

        val cancel = findViewById<Button>(R.id.button_cancel)
        val import = findViewById<Button>(R.id.button_import)

        val intent = intent
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
                progress.visibility = View.GONE;imV.visibility = View.VISIBLE
                val drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getDrawable(R.drawable.icon_cancel)
                } else {
                    resources.getDrawable(R.drawable.icon_cancel)
                }
                DrawableCompat.setTint(drawable!!,Color.RED)
                imV.setImageDrawable(drawable)
                msg.visibility = View.VISIBLE
                msg.text = getString(R.string.import_error)
            }
        }

        cancel.setOnClickListener { finish() }
        import.setOnClickListener {
            msg.visibility = View.GONE;progress.visibility = View.VISIBLE
            try{
                val fileName= jsonObject.getString("fileName")
                var outFile = File(filesDir.absolutePath + File.separator + MainActivity.CONFIG_DIR,fileName)
                val parent = outFile.parentFile
                if(!parent.exists()) parent.mkdirs()
                var count = 1
                if(outFile.exists()){
                    AlertDialog.Builder(this).setTitle("Confirm").setMessage("This remote controller configuration already exists!!")
                        .setNegativeButton("Quit") { _, _ -> finish() }
                        .setNeutralButton("Keep both"){ dialog,_ ->
                            while(outFile.exists()) {
                                outFile = File(outFile.parent, count.toString() + "_" + fileName)
                                count++
                            }
                            outFile.createNewFile()
                            jsonObject.put("fileName",outFile)
                            writeFile(outFile,jsonObject)
                            onSuccess(progress,imV,msg,import)
                            dialog.dismiss()
                        }
                        .setPositiveButton("Overwrite"){dialog,_ ->
                            outFile.delete()
                            outFile.createNewFile()
                            writeFile(outFile,jsonObject)
                            onSuccess(progress,imV,msg,import)
                            dialog.dismiss()
                        }
                        .setCancelable(false)
                        .show()
                }else{
                    outFile.createNewFile()
                    writeFile(outFile,jsonObject)
                    onSuccess(progress,imV,msg,import)
                }

            }catch(ex:Exception){
                progress.visibility = View.GONE;imV.visibility = View.VISIBLE
                val drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getDrawable(R.drawable.icon_cancel)
                } else {
                    resources.getDrawable(R.drawable.icon_cancel)
                }
                DrawableCompat.setTint(drawable!!,Color.RED)
                imV.setImageDrawable(drawable)
                msg.visibility = View.VISIBLE
                msg.text = getString(R.string.import_error)
            }
        }

    }

    private fun onSuccess(progress:ProgressBar,imV:ImageView,msg:TextView,btn:Button){
        progress.visibility = View.GONE;imV.visibility = View.VISIBLE
        val drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getDrawable(R.drawable.icon_check_circle)
        } else {
            resources.getDrawable(R.drawable.icon_check_circle)
        }
        DrawableCompat.setTint(drawable!!,Color.GREEN)
        imV.setImageDrawable(drawable)

        msg.text = getString(R.string.import_success)

        btn.text = getString(R.string.start_app)

        btn.setOnClickListener {
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
            finish()
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

    private fun onIntentRead(ins:InputStream?):JSONObject{
        val insr = InputStreamReader(ins)
        val out = TextUtils.join("",insr.readLines())

        insr.close()

        val jsonObj = JSONObject(out)
        val name= jsonObj.optString("name")
        val vendor = jsonObj.optString("vendor")
        val fileName= jsonObj.optString("fileName")
        val id = jsonObj.optString("id")
        val description = jsonObj.optString("description")

        val buttons = jsonObj.getJSONArray("buttons")
        for(i in 0 until buttons.length()){
            val button = buttons.getJSONObject(i)
            val protocol = button.getString("protocol")
            val irCode = button.getString("irCode")
            val length =button.getString("length")
        }

        return jsonObj
    }
}
