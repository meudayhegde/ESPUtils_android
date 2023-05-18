package com.github.meudayhegde.esputils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.menu.MenuBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.meudayhegde.esputils.databinding.ActivitySettingsBinding
import com.google.android.material.radiobutton.MaterialRadioButton
import com.github.meudayhegde.esputils.holders.SettingsItem
import com.github.meudayhegde.esputils.ui.adapters.SettingsAdapter


class SettingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainBinding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        when (getSharedPreferences(
            Strings.sharedPrefNameSettings, Context.MODE_PRIVATE
        ).getInt(
            Strings.sharedPrefItemApplicationTheme,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                0
            } else {
                2
            }
        )) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        mainBinding.toolbarSettings.setNavigationOnClickListener {
            finish()
        }

        viewManager = LinearLayoutManager(this)

        val fragmentList = ArrayList<String>()
        val menu = MenuBuilder(this)
        MenuInflater(this).inflate(R.menu.activity_main_drawer, menu)

        menu.visibleItems.forEach {
            fragmentList.add(it.title.toString())
        }
        for (i in 0..1) if (fragmentList.size != 0) fragmentList.removeAt(fragmentList.size - 1)

        viewAdapter = SettingsAdapter(
            arrayListOf(
                SettingsItem(
                    getString(R.string.title_application_theme),
                    getString(R.string.title_sub_application_theme),
                    selectionDialog(
                        getString(R.string.title_application_theme),
                        R.drawable.icon_theme,
                        Strings.sharedPrefItemApplicationTheme,
                        resources.getStringArray(R.array.settings_theme_list).asList()
                    ) {
                        themeChanged = true
                        AppCompatDelegate.setDefaultNightMode(
                            when (getSharedPreferences(
                                Strings.sharedPrefNameSettings, MODE_PRIVATE
                            ).getInt(Strings.sharedPrefItemApplicationTheme, 0)) {
                                1 -> {
                                    AppCompatDelegate.MODE_NIGHT_NO
                                }
                                2 -> {
                                    AppCompatDelegate.MODE_NIGHT_YES
                                }
                                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            }
                        )
                    },
                    R.drawable.icon_theme
                ), SettingsItem(
                    getString(R.string.title_home_fragment),
                    getString(R.string.title_sub_home_fragment),
                    selectionDialog(
                        getString(R.string.title_home_fragment),
                        R.drawable.icon_home,
                        Strings.sharedPrefItemHomeFragment,
                        fragmentList,
                        null
                    ),
                    R.drawable.icon_home
                )
            )
        )

        recyclerView = mainBinding.settingsList.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return true
    }

    private fun selectionDialog(
        title: String, icon: Int, prefName: String, optList: List<String>, action: Runnable?
    ): AlertDialog {
        val pref = getSharedPreferences(Strings.sharedPrefNameSettings, Context.MODE_PRIVATE)
        val editor = pref.edit()

        val content = RadioGroup(this)
        content.setPaddingRelative(24, 0, 0, 0)
        optList.forEach {
            val radioButton = MaterialRadioButton(this)
            radioButton.text = it
            radioButton.id = optList.indexOf(it)
            content.addView(radioButton)
            radioButton.layoutParams =
                Class.forName(radioButton.parent.javaClass.name).classes[1].getConstructor(
                    Int::class.java, Int::class.java
                ).newInstance(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ) as ViewGroup.LayoutParams?
        }

        val dialog =
            AlertDialog.Builder(this, R.style.AppTheme_AlertDialog).setTitle(title).setView(content)
                .setIcon(icon).setNegativeButton(R.string.cancel) { _, _ -> }
                .setPositiveButton(R.string.apply) { _, _ ->
                    editor.putInt(prefName, content.checkedRadioButtonId)
                    editor.apply()
                    action?.run()
                    ESPUtilsApp.showAd(this)
                }.create()

        dialog.setOnShowListener {
            content.check(pref.getInt(prefName, 0))
            content.layoutParams =
                Class.forName(content.parent.javaClass.name).classes[0].getConstructor(
                    Int::class.java, Int::class.java
                ).newInstance(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ) as ViewGroup.LayoutParams?
        }

        return dialog
    }

    companion object {
        var themeChanged = false
    }
}
