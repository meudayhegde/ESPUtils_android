package com.irware.remote

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.irware.remote.ui.adapters.SettingsAdapter
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when(getSharedPreferences("theme_setting", Context.MODE_PRIVATE).getInt("application_theme",if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { 0 }else{ 2 }))
        {1-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);2-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)}
        setContentView(R.layout.activity_settings)

        viewManager = LinearLayoutManager(this)
        viewAdapter = SettingsAdapter(arrayListOf(
            SettingsItem("Application Theme","UI theme for iRWaRE Application",themeSelectionDialog(), R.drawable.icon_theme)
        ))

//        supportActionBar?.setBackgroundDrawable(
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                getDrawable(R.mipmap.ic_launcher_background)
//            }else{
//                @Suppress("DEPRECATION")
//                resources.getDrawable(R.mipmap.ic_launcher_background)
//            }
//        )

        recyclerView = findViewById<RecyclerView>(R.id.settings_list).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home-> {
                finish()
            }
        }
        return true
    }

    @SuppressLint("ApplySharedPref", "InflateParams")
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
                        AppCompatDelegate.setDefaultNightMode(when(content.checkedRadioButtonId){R.id.rb_light_theme->AppCompatDelegate.MODE_NIGHT_NO;R.id.rb_dark_theme->AppCompatDelegate.MODE_NIGHT_YES;else->AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM})
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

class SettingsItem(var title: String, var subtitle: String, var dialog:Dialog, var iconRes: Int = 0)

interface OnSocketReadListener{
    fun onSocketRead(data:JSONObject)
}
