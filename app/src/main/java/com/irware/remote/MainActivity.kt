package com.irware.remote

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.os.Environment.getExternalStorageDirectory
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
import com.google.android.material.navigation.NavigationView
import com.irware.remote.listeners.OnConfigurationChangeListener
import com.irware.remote.listeners.OnFragmentInteractionListener
import com.irware.remote.listeners.OnValidationListener
import com.irware.remote.ui.buttons.RemoteButton
import com.irware.remote.ui.fragments.AboutFragment
import com.irware.remote.ui.fragments.DevicesFragment
import com.irware.remote.ui.fragments.GPIOControllerFragment
import com.irware.remote.ui.fragments.IRFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import java.io.*
import kotlin.math.min


@Suppress("NAME_SHADOWING")
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {}

    private val devicesFragment: DevicesFragment = DevicesFragment()
    val irFragment:IRFragment = IRFragment()
    private val gpioFragment: GPIOControllerFragment = GPIOControllerFragment()
    private var aboutFragment: AboutFragment = AboutFragment()
    private var splash: Dialog? = null
    private var authenticated = false
    private val onConfigChangeListeners: ArrayList<OnConfigurationChangeListener> = ArrayList()

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
        layoutParams.width = resources.displayMetrics.widthPixels
        layoutParams.height = resources.displayMetrics.heightPixels

        splash = object: Dialog(this, R.style.Theme_MaterialComponents_DayNight_NoActionBar){
            var exit = false
            @Deprecated("Deprecated in Java")
            override fun onBackPressed() {
                if(exit) finish()
                else Toast.makeText(this@MainActivity,"Press back again to exit", Toast.LENGTH_LONG).show()
                exit = true
                Handler(Looper.getMainLooper()).postDelayed({
                    exit = false
                }, 2000)
            }
        }

        val value = TypedValue()
        theme.resolveAttribute(R.attr.colorOnBackground, value, true)
        colorOnBackground = value.data

        val splashView = layoutInflater.inflate(R.layout.splash_screen,null)
        splash?.setContentView(splashView)
        splash?.window?.attributes?.windowAnimations = R.style.ActivityStartAnimationTheme
        splash?.show()
        hideSystemUI(splashView)
        val width = min(layoutParams.width, layoutParams.width)
        NUM_COLUMNS = when{(width > 920) -> 5; width < 720 -> 3; else -> 4}

        val file = ESPUtilsApp.getAbsoluteFile(R.string.name_dir_remote_config)
        if(!file.exists()) file.mkdir()

        val lparams = RelativeLayout.LayoutParams(width, width)
        lparams.addRule(RelativeLayout.CENTER_IN_PARENT)
        splash?.findViewById<ImageView>(R.id.splash_logo)?.layoutParams = lparams

        RemoteButton.onConfigChanged()

        val pref= getSharedPreferences(getString(R.string.shared_pref_name_login), 0)
        val editor=pref.edit()

        splash?.findViewById<TextView>(R.id.skip_login)?.setOnClickListener {
            splash?.dismiss()
            setNavView()
        }

        val passEdit:EditText? = splash?.findViewById(R.id.editTextPassword)
        passEdit?.setText(pref.getString(getString(R.string.shared_pref_item_password),""))

        val userEdit: EditText? = splash?.findViewById(R.id.edit_text_uname)
        userEdit?.setText(pref.getString(getString(R.string.shared_pref_item_username),""))

        val submit: Button? = splash?.findViewById(R.id.cirLoginButton)
        val validatedListener = object: OnValidationListener{
            override fun onValidated(verified: Boolean) {
                if(verified){
                    splash?.dismiss()
                    this@MainActivity.setContentView(R.layout.activity_main)
                    setNavView()
                }
                else Toast.makeText(this@MainActivity,"Authentication failed",Toast.LENGTH_LONG).show()
            }
        }

        submit?.setOnClickListener {

            USERNAME = userEdit?.text.toString()
            PASSWORD = passEdit?.text.toString()

            authenticated=true
            editor.putString(getString(R.string.shared_pref_item_username), USERNAME)
            editor.putString(getString(R.string.shared_pref_item_password), PASSWORD)
            editor.apply()

            validatedListener.onValidated(true)
        }

        addOnConfigurationChangeListener(object:OnConfigurationChangeListener{
            override var keepAlive = true
            override fun onConfigurationChanged(config: Configuration) {
                if(splash?.isShowing == true){
                    if(splash?.findViewById<LinearLayout>(R.id.login_view)?.visibility == View.VISIBLE) {
                        hideSystemUI(splash!!.findViewById<LinearLayout>(R.id.login_view))
                        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) splashLandscape(splash!!)
                        else splashPortrait(splash!!)
                    }
                }else{
                    keepAlive = false
                }
            }
        })

        if(pref.getString(getString(R.string.shared_pref_item_username), "") != "" &&
            pref.getString(getString(R.string.shared_pref_item_password), "") != "")
            authenticated = true

        Handler(Looper.getMainLooper()).postDelayed({
            if(splash?.isShowing == true) {
                val loginCard = splash!!.findViewById<LinearLayout>(R.id.login_view)

                loginCard.visibility = View.VISIBLE
                if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    val lparams = RelativeLayout.LayoutParams(layoutParams.width * 11/20, RelativeLayout.LayoutParams.WRAP_CONTENT)
                    lparams.addRule(RelativeLayout.ALIGN_PARENT_END)
                    lparams.addRule(RelativeLayout.CENTER_VERTICAL)
                    loginCard.layoutParams = lparams
                    loginCard.setPadding(layoutParams.height / 14, layoutParams.height / 10, layoutParams.height / 14, 0)
                    loginCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.expand_landscape))
                    splash!!.findViewById<ImageView>(R.id.splash_logo).startAnimation(AnimationUtils.loadAnimation(this, R.anim.move_landscape))
                }else{
                    loginCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.expand))
                    splash!!.findViewById<ImageView>(R.id.splash_logo).startAnimation(AnimationUtils.loadAnimation(this, R.anim.move))
                }

                if (authenticated) {
                    splash?.dismiss()
                    setContentView(R.layout.activity_main)
                    setNavView()
                }
            }
        }, 1100)
    }

    private fun splashPortrait(splash: Dialog){
        splash.findViewById<RelativeLayout>(R.id.splash_screen).setBackgroundResource(R.mipmap.background_circuit_portrait)
        val login = splash.findViewById<LinearLayout>(R.id.login_view)
        login.clearAnimation()
        login.setPadding(layoutParams.width / 14,0,layoutParams.width / 12,layoutParams.width / 22)
        val lparams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        lparams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        login.layoutParams = lparams
        splash.findViewById<ImageView>(R.id.splash_logo).clearAnimation()

        if(login.viewTreeObserver.isAlive){
            login.viewTreeObserver.addOnGlobalLayoutListener(object:ViewTreeObserver.OnGlobalLayoutListener{
                override fun onGlobalLayout() {
                    login.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val height = layoutParams.height - login.height
                    val logoParams = RelativeLayout.LayoutParams(height / 2,height / 2)
                    logoParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    logoParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                    logoParams.topMargin = height / 4
                    splash.findViewById<ImageView>(R.id.splash_logo).layoutParams = logoParams
                }
            })
        }
    }

    private fun splashLandscape(splash:Dialog){
        splash.findViewById<RelativeLayout>(R.id.splash_screen).setBackgroundResource(R.mipmap.background_circuit_landscape)
        val login = splash.findViewById<LinearLayout>(R.id.login_view)
        val lparams = RelativeLayout.LayoutParams(layoutParams.width * 11 / 20, RelativeLayout.LayoutParams.WRAP_CONTENT)
        lparams.addRule(RelativeLayout.ALIGN_PARENT_END)
        lparams.addRule(RelativeLayout.CENTER_VERTICAL)
        login.layoutParams = lparams
        login.clearAnimation()
        login.setPadding(layoutParams.height / 14,layoutParams.height / 10,layoutParams.height / 14,0)

        val logoParams = RelativeLayout.LayoutParams(layoutParams.width / 4,layoutParams.width / 4)
        logoParams.addRule(RelativeLayout.ALIGN_PARENT_START)
        logoParams.addRule(RelativeLayout.CENTER_VERTICAL)
        logoParams.marginStart = layoutParams.width / 8
        splash.findViewById<ImageView>(R.id.splash_logo).clearAnimation()
        splash.findViewById<ImageView>(R.id.splash_logo).layoutParams = logoParams
    }

    fun setNavView(){
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)

        val navHeader: LinearLayout = nav_view.getHeaderView(0) as LinearLayout
        navHeader.post{
            navHeader.layoutParams = LinearLayout.LayoutParams(navHeader.width, navHeader.width)
            navHeader.findViewById<ImageView>(R.id.app_icon).layoutParams =
                LinearLayout.LayoutParams((navHeader.width * 0.7).toInt(), (navHeader.width * 0.7).toInt())
            navHeader.findViewById<TextView>(R.id.app_name).textSize = (navHeader.width / 18).toFloat()
            navHeader.findViewById<TextView>(R.id.app_description).textSize = (navHeader.width / 26).toFloat()
        }
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        supportFragmentManager.beginTransaction().commitAllowingStateLoss()

        val homeFragment = getSharedPreferences(
            getString(R.string.shared_pref_name_settings), Context.MODE_PRIVATE).getInt(
            getString(R.string.shared_pref_item_home_fragment), 0)
        replaceFragment(when(homeFragment){1 -> irFragment 2-> gpioFragment 3 -> aboutFragment else-> devicesFragment})
        nav_view.setCheckedItem(when(homeFragment){1 -> R.id.home_drawer 2-> R.id.gpio_drawer 3 -> R.id.info_drawer else-> R.id.device_drawer_item})

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            private var backPressed = false
            override fun handleOnBackPressed() {
                when {
                    drawer_layout.isDrawerOpen(GravityCompat.START) -> drawer_layout.closeDrawer(GravityCompat.START)
                    backPressed -> finish()
                    else -> {
                        backPressed = true
                        Toast.makeText(this@MainActivity, "press back button again to exit", Toast.LENGTH_SHORT).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            backPressed = false
                        },2000)
                    }
                }
            }
        })
    }

    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction().replace(R.id.include_content,fragment).commitAllowingStateLoss()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings ->{
                val intent = Intent(this,SettingsActivity :: class.java )
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
                val intent = Intent(this,SettingsActivity :: class.java )
                startActivity(intent)
            }
            R.id.nav_rate -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun shareApplication(){
        try{
            val app: ApplicationInfo = applicationContext.applicationInfo
            val filePath: String = app.sourceDir

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "*/*"
            val originalApk = File(filePath)

            @Suppress("DEPRECATION") val tempFile = File("${(externalCacheDir?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {Environment.getStorageDirectory()} else getExternalStorageDirectory()).absolutePath}${File.separator}${getString(app.labelRes)}.apk")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            tempFile.createNewFile()

            val `in`: InputStream = FileInputStream(originalApk)
            val out: OutputStream = FileOutputStream(tempFile)

            val buf = ByteArray(1024)
            var len: Int
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
            `in`.close()
            out.close()
            println("File copied.")
            val uri: Uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", tempFile)
            intent.putExtra(Intent.EXTRA_STREAM, uri)

            startActivity(Intent.createChooser(intent, "Share app via"))
        }catch(ex: java.lang.Exception){
            Toast.makeText(this, "Error while sharing application.\n${ex.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun hideSystemUI(view: View) {

        @Suppress("DEPRECATION")
        view.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    private fun addOnConfigurationChangeListener(listener: OnConfigurationChangeListener){
        onConfigChangeListeners.add(listener)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        layoutParams.width = resources.displayMetrics.widthPixels
        layoutParams.height = resources.displayMetrics.heightPixels
        onConfigChangeListeners.iterator().forEach {
            try {
                it.onConfigurationChanged(newConfig)
                if(!it.keepAlive) onConfigChangeListeners.remove(it)
            }catch(ex:Exception){
                onConfigChangeListeners.remove(it)
                Toast.makeText(this, "Config changed listener error $ex",Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        if(splash?.isShowing == true) splash?.dismiss()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        ESPUtilsApp.devicePropList.forEach { it.refreshGPIOStatus() }
    }

    companion object {
        var USERNAME = ""
        var PASSWORD = ""
        var NUM_COLUMNS = 5
        var colorOnBackground = Color.BLACK
        var activity: MainActivity? = null
        var layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    private var restart = false
    fun onRestartClicked(view: View) {
        view.visibility = View.VISIBLE
        if(restart) recreate()
        else Toast.makeText(this, "Press again to Restart",Toast.LENGTH_SHORT).show()
        restart = true

        Handler(Looper.getMainLooper()).postDelayed({ restart = false },1400)
    }
}
