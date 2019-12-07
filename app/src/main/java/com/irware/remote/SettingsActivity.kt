package com.irware.remote

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.adapters.SettingsAdapter
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when(getSharedPreferences("theme_setting", Context.MODE_PRIVATE).getInt("application_theme",0)){1->setTheme(R.style.LightTheme);2->setTheme(R.style.DarkTheme);else->setTheme(R.style.AppTheme)}

        setContentView(R.layout.activity_settings)

        viewManager = LinearLayoutManager(this)
        viewAdapter = SettingsAdapter(arrayListOf(
            SettingsItem("Application Theme","UI theme for iRWaRE Application",themeSelectionDialog()),
            SettingsItem("Wireless Settings","Wi-Fi/Hotspot SSID and passwords",wirelessSettingsDialog()),
            SettingsItem("User Settings","User credentials (username and password)",userSettingsdialog())
        ))

        recyclerView = findViewById<RecyclerView>(R.id.settings_list).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId){
            android.R.id.home-> {
                finish()
            }
        }
        return true
    }

    private fun userSettingsdialog():AlertDialog{
        val content = LayoutInflater.from(this).inflate(R.layout.user_settings,null) as LinearLayout
        val cUname = content.findViewById<TextInputEditText>(R.id.cur_user_name)
        val cPass = content.findViewById<TextInputEditText>(R.id.cur_user_passwd)
        val nUname = content.findViewById<TextInputEditText>(R.id.til_user_name)
        val nPass = content.findViewById<TextInputEditText>(R.id.til_user_passwd)
        val nPassCon = content.findViewById<TextInputEditText>(R.id.til_user_confirm_passwd)

        val dialog = AlertDialog.Builder(this)
            .setTitle("User Settings")
            .setView(content)
            .setNegativeButton("Cancel"){dialog,_ -> dialog.dismiss()}
            .setPositiveButton(getString(R.string.apply)){_,_->}
            .create()

        for(item in listOf<TextInputEditText>(cUname,cPass,nUname,nPass,nPassCon)){
            item.addTextChangedListener(object:TextWatcher{
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    (item.parent.parent as TextInputLayout).error = null
                }
            })
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.layout_border_round_corner)
            dialog.window?.setLayout((MainActivity.size.x*0.8).toInt(),WindowManager.LayoutParams.WRAP_CONTENT)
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                var hasError = false
                for( tiet in listOf<TextInputEditText>(cUname,cPass,nUname,nPass,nPassCon)){
                    if (tiet.text.isNullOrEmpty()) {
                        (tiet.parent.parent as TextInputLayout).error = getString(R.string.empty_field)
                        hasError = true
                    }
                }
                if(nPassCon.text != nPass.text){
                    (nPassCon.parent.parent as TextInputLayout).error = getString(R.string.passwd_mismatch)
                    hasError = true
                }
                if(!hasError){
                    AlertDialog.Builder(this)
                        .setTitle("Confirm")
                        .setMessage("Wrong settings may result in inaccessibility of iRWaRE device (full reset will be required to recover))."
                                +"Make Sure UserName and password are correct")
                        .setNegativeButton("Cancel"){dg,_->dg.dismiss()}
                        .setPositiveButton("Confirm"){ _, _->
                            dialog.dismiss()
                            Thread {
                                try {
                                    val connector = SocketClient.Connector(MainActivity.MCU_IP)
                                    connector.sendLine("{\"request\":\"set_user\",\"username\":\""
                                            + cUname.text.toString() + "\",\"password\":\""
                                            + cPass.text.toString() + "\",\"new_username\":\""
                                            + nUname.text.toString()+"\",\"new_password\":\""
                                            + nPass.text.toString() + "\"}"
                                    )
                                    val result = connector.readLine()
                                    val resultObj = JSONObject(result)
                                    runOnUiThread{
                                        AlertDialog.Builder(this)
                                            .setTitle(if(resultObj.getString("response").contains("success",true)) "Success" else "Failed")
                                            .setMessage(resultObj.getString("response"))
                                            .setPositiveButton("Done"){dg,_->dg.dismiss()}
                                            .show()
                                    }
                                    connector.close()
                                }catch(ex:Exception){
                                    runOnUiThread{
                                        AlertDialog.Builder(this)
                                            .setTitle("Failed")
                                            .setMessage("Failed to apply user settings\n$ex")
                                            .setPositiveButton("Close"){dg,_->dg.dismiss()}
                                            .show()
                                    }
                                }
                            }.start()
                        }
                        .show()
                }
            }
        }

        return dialog
    }

    private fun wirelessSettingsDialog():AlertDialog{
        val content = LayoutInflater.from(this).inflate(R.layout.wireless_settings,null) as LinearLayout
        val ssid = content.findViewById<TextInputEditText>(R.id.til_wifi_name)
        val pass = content.findViewById<TextInputEditText>(R.id.til_wifi_passwd)
        val spinner = content.findViewById<Spinner>(R.id.spinner_wireless_mode)
        spinner.adapter = ArrayAdapter(this,android.R.layout.simple_list_item_1, arrayListOf("Station (WiFi)","Access Point (Hotspot)"))
        var mode = if(spinner.selectedItemPosition == 0) "WIFI" else "AP"
        spinner.onItemSelectedListener = object:AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                mode = if(position ==0 ) "WIFI" else "AP"
                (ssid.parent.parent as TextInputLayout).hint = "$mode Name"
                (pass.parent.parent as TextInputLayout).hint = "$mode Password"
            }
        }

        for(item in listOf<TextInputEditText>(ssid,pass)){
            item.addTextChangedListener(object:TextWatcher{
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    (item.parent.parent as TextInputLayout).error = null
                }
            })
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Wireless Settings")
            .setView(content)
            .setNegativeButton("Cancel"){dialog,_ -> dialog.dismiss()}
            .setPositiveButton(getString(R.string.apply)){_,_->}
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.layout_border_round_corner)
            dialog.window?.setLayout((MainActivity.size.x*0.8).toInt(),WindowManager.LayoutParams.WRAP_CONTENT)
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

            positiveButton.setOnClickListener {
                if(ssid.text.isNullOrEmpty())
                    (ssid.parent.parent as TextInputLayout).error = getString(R.string.empty_ssid)
                if(pass.text.isNullOrEmpty())
                    (pass.parent.parent as TextInputLayout).error = getString(R.string.empty_password)
                if(ssid.text!!.isNotEmpty() and pass.text!!.isNotEmpty()){
                    AlertDialog.Builder(this)
                        .setTitle("Confirm")
                        .setMessage("Wrong settings may result in inaccessibility of iRWaRE device (full reset will be required to recover))."
                                +"\nMake Sure All SSID and password are correct")
                        .setNegativeButton("Cancel"){dg,_->dg.dismiss()}
                        .setPositiveButton("Confirm"){dg,_->
                            dialog.dismiss()
                            Thread {
                                try {
                                    val connector = SocketClient.Connector(MainActivity.MCU_IP)
                                    connector.sendLine("{\"request\":\"set_wireless\",\"username\":\""
                                                + MainActivity.USERNAME + "\",\"password\":\""
                                                + MainActivity.PASSWORD + "\",\"wireless_mode\":\""+mode+"\",\"new_ssid\":\""
                                                + ssid.text.toString()+"\",\"new_pass\":\""
                                                +pass.text.toString() + "\"}"
                                    )
                                    val result = connector.readLine()
                                    val resultObj = JSONObject(result)
                                    runOnUiThread{
                                        AlertDialog.Builder(this)
                                            .setTitle(if(resultObj.getString("response").contains("success",true)) "Success" else "Failed")
                                            .setMessage(resultObj.getString("response"))
                                            .setPositiveButton("Done"){dg,_->dg.dismiss()}
                                            .show()
                                    }
                                    connector.close()
                                }catch(ex:Exception){
                                    runOnUiThread{
                                        AlertDialog.Builder(this)
                                            .setTitle("Failed")
                                            .setMessage("Failed to apply wireless settings\n$ex")
                                            .setPositiveButton("Close"){dg,_->dg.dismiss()}
                                            .show()
                                    }
                                }
                            }.start()

                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }

        return dialog
    }

    @SuppressLint("ApplySharedPref")
    private fun themeSelectionDialog():AlertDialog{
        val pref = getSharedPreferences("theme_setting", Context.MODE_PRIVATE)
        val editor = pref.edit()

        val content = LayoutInflater.from(this).inflate(R.layout.theme_settings,null) as RadioGroup

        val dialog = AlertDialog.Builder(this)
            .setTitle("Application Theme")
            .setView(content)
            .setNegativeButton("Cancel"){_,_->}
            .setPositiveButton("Apply"){_,_-> }
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.layout_border_round_corner)
            dialog.window?.setLayout((MainActivity.size.x * 0.8).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)

            content.check(when(getSharedPreferences("theme_setting", Context.MODE_PRIVATE).getInt("application_theme",0)){1->R.id.rb_light_theme;2->R.id.rb_dark_theme;else->R.id.rb_system_theme})
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Confirm")
                    .setNegativeButton("Cancel"){_,_->}
                    .setMessage("Confirm to Apply Theme")
                    .setPositiveButton("Confirm"){_,_-> dialog.dismiss()
                        editor.putInt("application_theme",when(content.checkedRadioButtonId){R.id.rb_light_theme->1;R.id.rb_dark_theme->2;else -> 0 })
                        editor.commit()
                        themeChanged = true
                        recreate()
                    }
                    .show()
            }
        }

        return dialog
    }

    companion object{
        var themeChanged = false
    }
}

class SettingsItem(var title:String, var subtitle:String,var dialog:Dialog)
