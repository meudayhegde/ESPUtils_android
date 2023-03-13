package com.github.meudayhegde.esputils

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.github.meudayhegde.esputils.databinding.ActivityMainBinding
import com.github.meudayhegde.esputils.databinding.SplashScreenBinding
import com.github.meudayhegde.esputils.listeners.OnConfigurationChangeListener
import com.github.meudayhegde.esputils.listeners.OnFragmentInteractionListener
import com.github.meudayhegde.esputils.listeners.OnValidationListener
import com.github.meudayhegde.esputils.ui.buttons.RemoteButton
import com.github.meudayhegde.esputils.ui.fragments.AboutFragment
import com.github.meudayhegde.esputils.ui.fragments.DevicesFragment
import com.github.meudayhegde.esputils.ui.fragments.GPIOControllerFragment
import com.github.meudayhegde.esputils.ui.fragments.IRFragment
import com.google.android.material.navigation.NavigationView
import java.io.*
import kotlin.math.min


@Suppress("NAME_SHADOWING")
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {}

    private lateinit var mainBinding: ActivityMainBinding
    private val devicesFragment: DevicesFragment = DevicesFragment()
    val irFragment: IRFragment = IRFragment()
    private val gpioFragment: GPIOControllerFragment = GPIOControllerFragment()
    private var aboutFragment: AboutFragment = AboutFragment()
    private var splashDialog: Dialog? = null
    private var authenticated = false
    private val onConfigChangeListeners: ArrayList<OnConfigurationChangeListener> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
        layoutParams.width = resources.displayMetrics.widthPixels
        layoutParams.height = resources.displayMetrics.heightPixels

        splashDialog =
            object : Dialog(this, R.style.Theme_MaterialComponents_DayNight_NoActionBar) {
                var exit = false

                @Deprecated("Deprecated in Java")
                override fun onBackPressed() {
                    if (exit) finish()
                    else Toast.makeText(
                        this@MainActivity, "Press back again to exit", Toast.LENGTH_LONG
                    ).show()
                    exit = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        exit = false
                    }, 2000)
                }
            }

        val value = TypedValue()
        theme.resolveAttribute(R.attr.colorOnBackground, value, true)
        colorOnBackground = value.data

        val splashBinding = SplashScreenBinding.inflate(layoutInflater)
        splashDialog?.setContentView(splashBinding.root)
        splashDialog?.window?.attributes?.windowAnimations = R.style.ActivityStartAnimationTheme
        splashDialog?.show()
        hideSystemUI(splashBinding.root)
        val width = min(layoutParams.width, layoutParams.width)
        NUM_COLUMNS = when {
            (width > 920) -> 5; width < 720 -> 3; else -> 4
        }

        val file = ESPUtilsApp.getPrivateFile(Strings.nameDirRemoteConfig)
        if (!file.exists()) file.mkdir()

        val lParams = RelativeLayout.LayoutParams(width, width)
        lParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        splashBinding.splashLogo.layoutParams = lParams

        RemoteButton.onConfigChanged()

        val pref = getSharedPreferences(Strings.sharedPrefNameLogin, 0)
        val editor = pref.edit()

        splashBinding.skipLogin.setOnClickListener {
            splashDialog?.dismiss()
            setNavView()
        }

        splashBinding.editTextPassword.setText(pref.getString(Strings.sharedPrefItemPassword, ""))
        splashBinding.editTextUname.setText(pref.getString(Strings.sharedPrefItemUsername, ""))

        val validatedListener = object : OnValidationListener {
            override fun onValidated(verified: Boolean) {
                if (verified) {
                    splashDialog?.dismiss()
                    setNavView()
                } else Toast.makeText(
                    this@MainActivity, R.string.message_auth_failed, Toast.LENGTH_LONG
                ).show()
            }
        }

        splashBinding.cirLoginButton.setOnClickListener {
            authenticated = true
            editor.putString(
                Strings.sharedPrefItemUsername, splashBinding.editTextUname.text.toString()
            )
            editor.putString(
                Strings.sharedPrefItemPassword, splashBinding.editTextPassword.text.toString()
            )
            editor.apply()
            validatedListener.onValidated(true)
        }

        addOnConfigurationChangeListener(object : OnConfigurationChangeListener {
            override var keepAlive = true
            override fun onConfigurationChanged(config: Configuration) {
                if (splashDialog?.isShowing == true) {
                    if (splashBinding.loginView.visibility == View.VISIBLE) {
                        hideSystemUI(splashBinding.loginView)
                        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) splashLandscape(
                            splashBinding
                        )
                        else splashPortrait(splashBinding)
                    }
                } else {
                    keepAlive = false
                }
            }
        })

        if (pref.getString(
                Strings.sharedPrefItemUsername, ""
            ) != "" && pref.getString(Strings.sharedPrefItemPassword, "") != ""
        ) authenticated = true

        Handler(Looper.getMainLooper()).postDelayed({
            if (splashDialog?.isShowing == true) {
                splashBinding.loginView.visibility = View.VISIBLE
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    val lParams = RelativeLayout.LayoutParams(
                        layoutParams.width * 11 / 20, RelativeLayout.LayoutParams.WRAP_CONTENT
                    )
                    lParams.addRule(RelativeLayout.ALIGN_PARENT_END)
                    lParams.addRule(RelativeLayout.CENTER_VERTICAL)
                    splashBinding.loginView.layoutParams = lParams
                    splashBinding.loginView.setPadding(
                        layoutParams.height / 14,
                        layoutParams.height / 10,
                        layoutParams.height / 14,
                        0
                    )
                    splashBinding.loginView.startAnimation(
                        AnimationUtils.loadAnimation(
                            this, R.anim.expand_landscape
                        )
                    )
                    splashBinding.splashLogo.startAnimation(
                        AnimationUtils.loadAnimation(
                            this, R.anim.move_landscape
                        )
                    )
                } else {
                    splashBinding.loginView.startAnimation(
                        AnimationUtils.loadAnimation(
                            this, R.anim.expand
                        )
                    )
                    splashBinding.splashLogo.startAnimation(
                        AnimationUtils.loadAnimation(
                            this, R.anim.move
                        )
                    )
                }

                if (authenticated) {
                    splashDialog?.dismiss()
                    setNavView()
                }
            }
        }, 1100)
    }

    private fun splashPortrait(splashBinding: SplashScreenBinding) {
        splashBinding.root.setBackgroundResource(R.mipmap.background_circuit_portrait)
        splashBinding.loginView.clearAnimation()
        splashBinding.loginView.setPadding(
            layoutParams.width / 14, 0, layoutParams.width / 12, layoutParams.width / 22
        )
        val lParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        lParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        splashBinding.loginView.layoutParams = lParams
        splashBinding.splashLogo.clearAnimation()

        if (splashBinding.loginView.viewTreeObserver.isAlive) {
            splashBinding.loginView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    splashBinding.loginView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val height = layoutParams.height - splashBinding.loginView.height
                    val logoParams = RelativeLayout.LayoutParams(height / 2, height / 2)
                    logoParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    logoParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                    logoParams.topMargin = height / 4
                    splashBinding.splashLogo.layoutParams = logoParams
                }
            })
        }
    }

    private fun splashLandscape(splashBinding: SplashScreenBinding) {
        splashBinding.root.setBackgroundResource(R.mipmap.background_circuit_landscape)
        val lParams = RelativeLayout.LayoutParams(
            layoutParams.width * 11 / 20, RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        lParams.addRule(RelativeLayout.ALIGN_PARENT_END)
        lParams.addRule(RelativeLayout.CENTER_VERTICAL)
        splashBinding.loginView.layoutParams = lParams
        splashBinding.loginView.clearAnimation()
        splashBinding.loginView.setPadding(
            layoutParams.height / 14, layoutParams.height / 10, layoutParams.height / 14, 0
        )

        val logoParams = RelativeLayout.LayoutParams(layoutParams.width / 4, layoutParams.width / 4)
        logoParams.addRule(RelativeLayout.ALIGN_PARENT_START)
        logoParams.addRule(RelativeLayout.CENTER_VERTICAL)
        logoParams.marginStart = layoutParams.width / 8
        splashBinding.splashLogo.clearAnimation()
        splashBinding.splashLogo.layoutParams = logoParams
    }

    fun setNavView() {
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)
        setSupportActionBar(mainBinding.toolbar)
        val toggle = ActionBarDrawerToggle(
            this,
            mainBinding.drawerLayout,
            mainBinding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        mainBinding.drawerLayout.addDrawerListener(toggle)

        val navHeader: LinearLayout = mainBinding.navView.getHeaderView(0) as LinearLayout
        navHeader.post {
            val headerIcon = navHeader.findViewById<ImageView>(R.id.app_icon)
            headerIcon.layoutParams = LinearLayout.LayoutParams(
                (navHeader.width * 0.7).toInt(), (navHeader.width * 0.7).toInt()
            )
            val headerTitle = navHeader.findViewById<TextView>(R.id.app_name)
            val headerSubtitle = navHeader.findViewById<TextView>(R.id.app_description)
            headerTitle.post { headerSubtitle.post {
                navHeader.layoutParams = LinearLayout.LayoutParams(navHeader.width, headerIcon.layoutParams.height + (headerTitle.height * 2) + headerSubtitle.height)
            } }
        }
        toggle.syncState()
        mainBinding.navView.setNavigationItemSelectedListener(this)
        supportFragmentManager.beginTransaction().commitAllowingStateLoss()

        val homeFragment = getSharedPreferences(
            Strings.sharedPrefNameSettings, Context.MODE_PRIVATE
        ).getInt(
            Strings.sharedPrefItemHomeFragment, 0
        )
        replaceFragment(
            when (homeFragment) {
                1 -> irFragment
                2 -> gpioFragment
                3 -> aboutFragment
                else -> devicesFragment
            }
        )
        mainBinding.navView.setCheckedItem(
            when (homeFragment) {
                1 -> R.id.home_drawer
                2 -> R.id.gpio_drawer
                3 -> R.id.info_drawer
                else -> R.id.device_drawer_item
            }
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            private var backPressed = false
            override fun handleOnBackPressed() {
                when {
                    mainBinding.drawerLayout.isDrawerOpen(GravityCompat.START) -> mainBinding.drawerLayout.closeDrawer(
                        GravityCompat.START
                    )
                    backPressed -> finish()
                    else -> {
                        backPressed = true
                        Toast.makeText(
                            this@MainActivity, R.string.message_back_to_exit, Toast.LENGTH_SHORT
                        ).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            backPressed = false
                        }, 2000)
                    }
                }
            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.content_main, fragment)
            .commitAllowingStateLoss()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.device_drawer_item -> {
                replaceFragment(devicesFragment as Fragment)
            }
            R.id.home_drawer -> {
                replaceFragment(irFragment as Fragment)
            }
            R.id.gpio_drawer -> {
                replaceFragment(gpioFragment as Fragment)
            }
            R.id.info_drawer -> {
                replaceFragment(aboutFragment as Fragment)
            }
            R.id.share_drawer -> {
                shareApplication()
            }
            R.id.nav_action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_rate -> {

            }
        }

        mainBinding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun shareApplication() {
        try {
            val app: ApplicationInfo = applicationContext.applicationInfo
            val filePath: String = app.sourceDir

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = Strings.intentTypeAll
            val originalApk = File(filePath)

            val tempFile =
                ESPUtilsApp.getExternalCache(getString(app.labelRes) + Strings.extensionApk)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            tempFile.createNewFile()

            val ins: InputStream = FileInputStream(originalApk)
            val out: OutputStream = FileOutputStream(tempFile)

            val buf = ByteArray(1024)
            var len: Int
            while (ins.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
            ins.close()
            out.close()

            val uri: Uri = FileProvider.getUriForFile(
                this, BuildConfig.APPLICATION_ID + Strings.extensionProvider, tempFile
            )
            intent.putExtra(Intent.EXTRA_STREAM, uri)

            startActivity(Intent.createChooser(intent, getString(R.string.intent_title_share_app)))
        } catch (ex: java.lang.Exception) {
            Toast.makeText(this, R.string.message_err_app_share, Toast.LENGTH_LONG).show()
        }
    }

    private fun hideSystemUI(view: View) {
        @Suppress("DEPRECATION") view.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    private fun addOnConfigurationChangeListener(listener: OnConfigurationChangeListener) {
        onConfigChangeListeners.add(listener)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        layoutParams.width = resources.displayMetrics.widthPixels
        layoutParams.height = resources.displayMetrics.heightPixels
        onConfigChangeListeners.iterator().forEach {
            try {
                it.onConfigurationChanged(newConfig)
                if (!it.keepAlive) onConfigChangeListeners.remove(it)
            } catch (ex: Exception) {
                onConfigChangeListeners.remove(it)
                Toast.makeText(this, R.string.message_err_config_change, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        if (splashDialog?.isShowing == true) splashDialog?.dismiss()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        ESPUtilsApp.devicePropList.forEach { it.refreshGPIOStatus() }
    }

    companion object {
        var NUM_COLUMNS = 5
        var colorOnBackground = Color.BLACK
        var activity: MainActivity? = null
        var layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }
}
