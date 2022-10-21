package com.irware.remote

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.menu.MenuBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.radiobutton.MaterialRadioButton
import com.irware.remote.holders.DeviceProperties
import com.irware.remote.ui.adapters.SettingsAdapter


class SettingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    @SuppressLint("UseCompatLoadingForDrawables", "RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when(getSharedPreferences("theme_setting", Context.MODE_PRIVATE).getInt("application_theme",if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { 0 }else{ 2 }))
        {1-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);2-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)}
        setContentView(R.layout.activity_settings)

        viewManager = LinearLayoutManager(this)


        val fragmentList = ArrayList<String>()
        val menu = MenuBuilder(this)
        MenuInflater(this).inflate(R.menu.activity_main_drawer, menu)

        menu.visibleItems.forEach {
            fragmentList.add(it.title.toString())
        }
        for(i in 0..1) if(fragmentList.size != 0 ) fragmentList.removeAt(fragmentList.size - 1)

        viewAdapter = SettingsAdapter(arrayListOf(
            SettingsItem("Application Theme","UI theme for iRWaRE Application", selectionDialog("Application Theme", R.drawable.icon_theme, "application_theme", arrayListOf("Follow System Theme", "Light Theme", "Dark Theme")) {
                themeChanged = true
                AppCompatDelegate.setDefaultNightMode(
                    when (getSharedPreferences("settings", Context.MODE_PRIVATE).getInt("application_theme", 0)) {
                        1 -> { AppCompatDelegate.MODE_NIGHT_NO }
                        2 -> { AppCompatDelegate.MODE_NIGHT_YES }
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                )
            }, R.drawable.icon_theme),
            SettingsItem("Home Fragment","Fragment that opens on app launch", selectionDialog("Home Fragment", R.drawable.icon_home, "home_fragment", fragmentList, null), R.drawable.icon_home)
        ))

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
    private fun selectionDialog(title: String, icon: Int, prefName: String, optList: List<String>, action: Runnable?):AlertDialog{
        val pref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val editor = pref.edit()

        val content = RadioGroup(this)
        content.setPaddingRelative(24, 0,0,0)
        optList.forEach {
            val radioButton = MaterialRadioButton(this)
            radioButton.text = it
            radioButton.id = optList.indexOf(it)
            content.addView(radioButton)
            radioButton.layoutParams =
                Class.forName(radioButton.parent.javaClass.name).classes[1].getConstructor(Int::class.java, Int::class.java).newInstance(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) as ViewGroup.LayoutParams?
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(content)
            .setIcon(icon)
            .setNegativeButton("Cancel"){_,_->}
            .setPositiveButton("Apply"){_,_->
                editor.putInt(prefName, content.checkedRadioButtonId)
                editor.apply()
                action?.run()
            }
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.layout_border_round_corner)
            dialog.window?.setLayout((MainActivity.size.x * 0.8).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)

            content.check(pref.getInt(prefName,0))
            content.layoutParams =
                Class.forName(content.parent.javaClass.name).classes[0].getConstructor(Int::class.java, Int::class.java).newInstance(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) as ViewGroup.LayoutParams?
        }

        return dialog
    }

    companion object{
        var themeChanged = false
    }
}

class SettingsItem(var title: String, var subtitle: String, var dialog:Dialog?,
                   var iconRes: Int = 0, var clickAction: Runnable? = null, var prop: DeviceProperties? = null){
}
