package com.irware.remote

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.irware.getIPAddress
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.listeners.OnValidationListener
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.BlurBuilder
import com.irware.remote.ui.buttons.RemoteButton
import com.irware.remote.ui.fragments.AboutFragment
import com.irware.remote.ui.fragments.HomeFragment
import com.irware.remote.ui.fragments.OnFragmentInteractionListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import org.json.JSONObject
import java.io.*
import java.net.InetAddress
import kotlin.math.min
import kotlin.math.roundToInt


@Suppress("NAME_SHADOWING")
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {}

    var homeFragment:HomeFragment? = null
    private var aboutFragment: AboutFragment? = null
    private var ipList:ArrayList<String> = ArrayList()
    private lateinit var ipConf : File
    private var authenticated=false
    private var connected = false
    private val onConfigChangeListeners:ArrayList<OnConfigurationChangeListener> = ArrayList()

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when(getSharedPreferences("theme_setting", Context.MODE_PRIVATE).getInt("application_theme",if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { 0 }else{ 2 }))
        {1-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);2-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)}
        remotePropList.clear()
        activity = this
        val arr = resources.obtainTypedArray(R.array.icons)
        iconDrawableList = IntArray(arr.length())
        for(i in 0 until arr.length())
            iconDrawableList[i] = arr.getResourceId(i,0)
        arr.recycle()
        configPath = filesDir.absolutePath + File.separator + CONFIG_DIR
        ipConf = File(filesDir.absolutePath+File.separator+"iplist.conf")
        if(!ipConf.exists())ipConf.createNewFile()
        val splash=object:Dialog(this,android.R.style.Theme_Light_NoTitleBar_Fullscreen){
            var exit = false
            override fun onBackPressed() {
                if(exit) finish()
                else Toast.makeText(this@MainActivity,"Press back again to exit", Toast.LENGTH_LONG).show()
                exit = true
                Handler().postDelayed({
                    exit = false
                },2000)
            }
        }

        val value = TypedValue()
        theme.resolveAttribute(R.attr.colorOnBackground, value, true)
        colorOnBackground = value.data

        val splashView=layoutInflater.inflate(R.layout.splash_screen,null)
        splash.setContentView(splashView)

        val originalBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_background)
        val blurredBitmap = BlurBuilder.blur(this, originalBitmap)
        splashView.background = BitmapDrawable(resources, blurredBitmap)
        splash.window?.attributes?.windowAnimations = R.style.ActivityStartAnimationTheme
        splash.show()
        hideSystemUI(splashView)
        windowManager.defaultDisplay.getSize(size)
        val x = min(size.x,size.y)
        NUM_COLUMNS = when{x>920->5;x<720->3;else->4}

        val file = File(filesDir.absolutePath+File.separator+ CONFIG_DIR)
        if(!file.exists()) file.mkdir()

        val lparams = RelativeLayout.LayoutParams((min(size.x,size.y)*0.6F).roundToInt(),(min(size.x,size.y)*0.6F).roundToInt())
        lparams.addRule(RelativeLayout.CENTER_IN_PARENT)
        splash.findViewById<ImageView>(R.id.splash_logo).layoutParams = lparams

        RemoteButton.onConfigChanged()

        val pref=getSharedPreferences("login",0)
        val editor=pref.edit()

        splash.findViewById<TextView>(R.id.skip_login).setOnClickListener {
            splash.dismiss()
            setNavView()
        }

        val ipEdit:EditText= splash.findViewById(R.id.editTextIP)
        val passEdit:EditText= splash.findViewById(R.id.editTextPassword)
        passEdit.setText(pref.getString("password",""))

        val userEdit:EditText= splash.findViewById(R.id.edit_text_uname)
        userEdit.setText(pref.getString("username",""))

        val submit:Button= splash.findViewById(R.id.cirLoginButton)
        val validatedListener=object:OnValidationListener{
            override fun onValidated(verified: Boolean) {
                if(verified){
                    splash.dismiss()
                    this@MainActivity.setContentView(R.layout.activity_main)
                    setNavView()
                }
                else Toast.makeText(this@MainActivity,"Authentication failed",Toast.LENGTH_LONG).show()
            }
        }

        submit.setOnClickListener {
            val ip = ipEdit.text.toString()
            Thread{
                if(InetAddress.getByName(ip).isReachable(500)) {
                    try{
                        val connector = SocketClient.Connector(ip)

                        connector.sendLine("{\"request\":\"authenticate\",\"username\":\""+userEdit.text.toString()+"\",\"password\":\""+passEdit.text.toString()+"\",\"data\":\"__\",\"length\":\"0\"}")
                        val response=connector.readLine()
                        connector.close()
                        if(JSONObject(response)["response"]=="authenticated"){
                            MCU_IP = ip
                            USERNAME = userEdit.text.toString()
                            PASSWORD = passEdit.text.toString()
                            if(!ipList.contains(ip)) ipList.add(0,ip)
                            else {ipList.remove(ip); ipList.add(0,ip)}
                            runOnUiThread {
                                authenticated=true
                                editor.putString("username",USERNAME)
                                editor.putString("password",PASSWORD)
                                editor.apply()
                                validatedListener.onValidated(true)
                            }
                        }else{
                            runOnUiThread {
                                Toast.makeText(this@MainActivity,
                                    "Authentication failed, please check credentials",Toast.LENGTH_LONG).show()
                            }
                        }

                    }catch(ex:IOException){
                        runOnUiThread {
                            Toast.makeText(this@MainActivity,"ip is not of a valid iRWaRE device",Toast.LENGTH_LONG).show()
                        }
                    }

                }else{
                    runOnUiThread {
                        Toast.makeText(this@MainActivity,"Ip is not reachable!",Toast.LENGTH_LONG).show()
                    }
                }
                val writer=OutputStreamWriter(ipConf.outputStream())
                writer.write(TextUtils.join("\n",ipList))
                writer.flush()
                writer.close()
            }.start()
        }

        addOnConfigurationChangeListener(object:OnConfigurationChangeListener{
            override var keepAlive = true
            override fun onConfigurationChanged(config: Configuration) {
                if(splash.isShowing && splash.findViewById<LinearLayout>(R.id.login_view).visibility == View.VISIBLE){
                    hideSystemUI(splash.findViewById<LinearLayout>(R.id.login_view))
                    if(config.orientation ==  Configuration.ORIENTATION_LANDSCAPE) splashLandscape(splash)
                    else splashPortrait(splash)
                }else{
                    keepAlive = false
                }
            }
        })

        Thread{
            ipList= BufferedReader(InputStreamReader(ipConf.inputStream())).readLines() as ArrayList<String>
            val newList = ArrayList<String>()
            newList.addAll(ipList)
            for(ip:String in newList){
                if(!connected and InetAddress.getByName(ip).isReachable(50)){
                    if(ipVerified(ip)) {
                        onIpVerified(ip,ipEdit,userEdit,passEdit,submit)
                        break
                    }
                }
            }
            val addrs = getIPAddress()
            for(addr in addrs){
                if(!connected) {
                    val str = addr.split(".").toMutableList()
                    str.removeAt(str.lastIndex)
                    val subnet = TextUtils.join(".", str)
                    if(!connected){
                        for (i in 0 until 255) {
                                val ip = "$subnet.$i"
                                if (!connected and InetAddress.getByName(ip).isReachable(20)) {
                                    if (ipVerified(ip)) {
                                        onIpVerified(ip,ipEdit,userEdit,passEdit,submit)
                                        break
                                    }
                                }
                        }
                    }
                }
            }

        }.start()

        Thread{
            val files = File(configPath).listFiles { pathname ->
                pathname!!.isFile and (pathname.name.endsWith(
                    ".json",
                    true
                )) and pathname.canWrite()
            }
            for (file in files)
                remotePropList.add(RemoteProperties(file, null))
        }.start()

        Handler().postDelayed({
            if(splash.isShowing) {
                splash.findViewById<RelativeLayout>(R.id.splash_restart_layout).visibility = View.VISIBLE
                val loginCard = splash.findViewById<LinearLayout>(R.id.login_view)

                loginCard.visibility = View.VISIBLE
                if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    val lparams = RelativeLayout.LayoutParams(size.x*11/20,RelativeLayout.LayoutParams.WRAP_CONTENT)
                    lparams.addRule(RelativeLayout.ALIGN_PARENT_END)
                    lparams.addRule(RelativeLayout.CENTER_VERTICAL)
                    loginCard.layoutParams = lparams
                    loginCard.setPadding(size.y/14,size.y/10,size.y/14,0)
                    loginCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.expand_landscape))
                    splash.findViewById<ImageView>(R.id.splash_logo).startAnimation(AnimationUtils.loadAnimation(this, R.anim.move_landscape))
                }else{
                    loginCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.expand))
                    splash.findViewById<ImageView>(R.id.splash_logo).startAnimation(AnimationUtils.loadAnimation(this, R.anim.move))
                }

                if (authenticated) {
                    splash.dismiss()
                    setContentView(R.layout.activity_main)
                    setNavView()
                }
            }
        },2000)
    }

    private fun splashPortrait(splash:Dialog){
        val login = splash.findViewById<LinearLayout>(R.id.login_view)
        login.clearAnimation()
        login.setPadding(size.x/14,0,size.x/12,size.y/22)
        val lparams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT)
        lparams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        login.layoutParams = lparams

        if(login.viewTreeObserver.isAlive){
            login.viewTreeObserver.addOnGlobalLayoutListener(object:ViewTreeObserver.OnGlobalLayoutListener{
                override fun onGlobalLayout() {
                    login.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val height = size.y-login.height

                    val logoParams = RelativeLayout.LayoutParams(height/2,height/2)
                    logoParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    logoParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                    logoParams.topMargin = height/4
                    splash.findViewById<ImageView>(R.id.splash_logo).clearAnimation()
                    splash.findViewById<ImageView>(R.id.splash_logo).layoutParams = logoParams

                }
            })
        }
    }

    private fun splashLandscape(splash:Dialog){
        val login = splash.findViewById<LinearLayout>(R.id.login_view)
        val lparams = RelativeLayout.LayoutParams(size.x*11/20,RelativeLayout.LayoutParams.WRAP_CONTENT)
        lparams.addRule(RelativeLayout.ALIGN_PARENT_END)
        lparams.addRule(RelativeLayout.CENTER_VERTICAL)
        login.layoutParams = lparams
        login.clearAnimation()
        login.setPadding(size.y/14,size.y/10,size.y/14,0)

        val logoParams = RelativeLayout.LayoutParams(size.x/4,size.x/4)
        logoParams.addRule(RelativeLayout.ALIGN_PARENT_START)
        logoParams.addRule(RelativeLayout.CENTER_VERTICAL)
        logoParams.marginStart = size.x/8
        splash.findViewById<ImageView>(R.id.splash_logo).clearAnimation()
        splash.findViewById<ImageView>(R.id.splash_logo).layoutParams = logoParams
    }

    private fun ipVerified(ip:String):Boolean{
        try{
            val connector=SocketClient.Connector(ip)
            connector.sendLine("{\"request\":\"ping\"}")
            MCU_MAC = JSONObject(connector.readLine()).getString("MAC")
            connector.close()
            if(ip in ipList){
                ipList.remove(ip)
                ipList.add(0,ip)
            }else ipList.add(0,ip)
            return true
        }catch(ex: Exception){}
        return false
    }

    private fun onIpVerified(ip:String,ipEdit:EditText,userEdit:EditText,passEdit:EditText,submit:Button){
        runOnUiThread {
            if(!connected) {
                connected = true
                ipEdit.setText(ip)
                if (userEdit.text.isNotEmpty() and passEdit.text.isNotEmpty()) {
                    submit.callOnClick()
                }
            }
        }
    }

    fun setNavView(){
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)

        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
        supportFragmentManager.beginTransaction().commitAllowingStateLoss()
        nav_view.setCheckedItem(R.id.home_drawer)
        if(homeFragment == null)
            homeFragment=HomeFragment()

        replaceFragment(homeFragment as Fragment)
    }

    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction().replace(R.id.include_content,fragment).commitAllowingStateLoss()
    }

    private var backPressed = false
    override fun onBackPressed() {
        when {
            drawer_layout.isDrawerOpen(GravityCompat.START) -> drawer_layout.closeDrawer(GravityCompat.START)
            backPressed -> super.onBackPressed()
            else -> {
                backPressed = true
                Toast.makeText(this,"press back button again to exit",Toast.LENGTH_SHORT).show()
                Handler().postDelayed({
                    backPressed = false
                },2000)

            }
        }
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
            R.id.home_drawer -> {
                if(homeFragment==null)
                    homeFragment=HomeFragment()
                replaceFragment(homeFragment as Fragment)
            }
            R.id.info_drawer -> {
                if(aboutFragment==null)
                    aboutFragment=AboutFragment()
                replaceFragment(aboutFragment as Fragment)
            }
            R.id.share_drawer -> {

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

    private fun hideSystemUI(view:View) {
        view.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }


    fun startConfigChooser(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/json"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a remote controller configuration file"), FILE_SELECT_CODE)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                this, "Please install a File Manager.",
                Toast.LENGTH_SHORT
            ).show()
        }


    }

    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?) {
        when (requestCode) {
            FILE_SELECT_CODE ->
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    val uri = data?.data
                    Log.d("CONFIG_SELECTOR", "File Uri: " + uri.toString())
                    try {
                        val mIntent = Intent(Intent.ACTION_VIEW)

                        mIntent.setDataAndType(uri, "application/json")
                        mIntent.setPackage(packageName)
                        startActivity(Intent.createChooser(mIntent, "Import Config File"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun addOnConfigurationChangeListener(listener: OnConfigurationChangeListener){
        onConfigChangeListeners.add(listener)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        windowManager.defaultDisplay.getSize(size)
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

    companion object {
        val size:Point=Point()
        const val PORT=48321
        const val CONFIG_DIR = "remotes"
        const val FILE_SELECT_CODE = 0
        val remotePropList = ArrayList<RemoteProperties>()
        var MCU_MAC = ""
        var configPath =""
        var MCU_IP = "192.168.4.1"
        var USERNAME = ""
        var PASSWORD = ""
        var NUM_COLUMNS = 5
        var activity:MainActivity? = null
        var colorOnBackground = Color.BLACK
        var iconDrawableList:IntArray = intArrayOf()
    }

    private var restart = false
    fun onRestartClicked(view: View) {
        view.visibility = View.VISIBLE
        if(restart) recreate()
        else Toast.makeText(this, "Press again to Restart",Toast.LENGTH_SHORT).show()
        restart = true
        Handler().postDelayed({ restart = false },1400)
    }
}

interface OnConfigurationChangeListener{
    var keepAlive:Boolean
    fun onConfigurationChanged(config:Configuration)
}
