package com.irware.remote

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
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
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


@Suppress("NAME_SHADOWING")
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {}

    private var homeFragment:HomeFragment? = null
    private var aboutFragment: AboutFragment? = null
    private var ipList:ArrayList<String> = ArrayList<String>()
    private var ipConf : File? = null
    private var authenticated=false
    private var connected = false

    private lateinit var ipEdit:EditText
    private lateinit var userEdit:EditText
    private lateinit var passEdit:EditText
    private lateinit var submit:Button

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when(getSharedPreferences("theme_setting", Context.MODE_PRIVATE).getInt("application_theme",0)){1->setTheme(R.style.LightTheme_NoActionBar);2->setTheme(R.style.DarkTheme_NoActionBar);else->setTheme(R.style.AppTheme_NoActionBar)}

        remotePropList.clear()
        activity = this
        val arr = resources.obtainTypedArray(R.array.icons)
        iconDrawableList = IntArray(arr.length())
        for(i in 0 until arr.length())
            iconDrawableList[i] = arr.getResourceId(i,0)
        arr.recycle()

        configPath = filesDir.absolutePath + File.separator + CONFIG_DIR
        ipConf = File(filesDir.absolutePath+File.separator+"iplist.conf")
        if(!ipConf!!.exists())ipConf!!.createNewFile()
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

        val file = File(filesDir.absolutePath+File.separator+ CONFIG_DIR)
        if(!file.exists()) file.mkdir()


        val logo=splash.findViewById<ImageView>(R.id.splash_logo)
        logo.layoutParams=LinearLayout.LayoutParams((min(size.x,size.y)*0.6F).roundToInt(),(min(size.x,size.y)*0.6F).roundToInt())
        RemoteButton.onActivityLoad()

        val pref=getSharedPreferences("login",0)
        val editor=pref.edit()

        ipEdit= splash.findViewById(R.id.editTextIP)
        passEdit= splash.findViewById(R.id.editTextPassword)
        passEdit.setText(pref.getString("password",""))

        userEdit= splash.findViewById(R.id.edit_text_uname)
        userEdit.setText(pref.getString("username",""))

        submit= splash.findViewById(R.id.cirLoginButton)
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

        splash.findViewById<TextView>(R.id.skip_login).setOnClickListener {
            splash.dismiss()
            setContentView(R.layout.activity_main)
            setNavView()
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
                val writer=OutputStreamWriter(ipConf!!.outputStream())
                writer.write(TextUtils.join("\n",ipList))
                writer.flush()
                writer.close()
            }.start()
        }

        Thread{
            ipList= BufferedReader(InputStreamReader(ipConf!!.inputStream())).readLines() as ArrayList<String>
            val newList = ArrayList<String>()
            newList.addAll(ipList)
            for(ip:String in newList){
                if(!connected and InetAddress.getByName(ip).isReachable(50)){
                    if(ipVerified(ip)) {
                        onIpVerified(ip)
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
                                        onIpVerified(ip)
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

                val cardAnim = AnimationUtils.loadAnimation(this, R.anim.expand)
                val logoAnim = AnimationUtils.loadAnimation(this, R.anim.move)

                loginCard.visibility = View.VISIBLE
                logo.startAnimation(logoAnim)
                loginCard.startAnimation(cardAnim)

                if (authenticated) {
                    splash.dismiss()
                    setContentView(R.layout.activity_main)
                    setNavView()
                }
            }
        },2000)
    }

    private fun min(x:Int, y:Int):Int{
        return if(x<y) x else y
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

    private fun onIpVerified(ip:String){
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
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)

        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
        nav_view.setCheckedItem(R.id.home_drawer)
        if(homeFragment == null)
            homeFragment=HomeFragment()
        replaceFragment(homeFragment as Fragment)
        val pref = getSharedPreferences("general",0)
        NUM_COLUMNS = pref.getInt("num_columns",5)
    }


    var backPressed = false
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
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction().replace(R.id.include_content,fragment).commit()
    }

    private fun hideSystemUI(view:View) {
        view.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }


    private fun showSystemUI(view: View) {
        view.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    override fun onResume() {
        super.onResume()
        if(SettingsActivity.themeChanged){
            SettingsActivity.themeChanged = false
            recreate()
        }
    }

    companion object {
        val size:Point=Point()
        const val PORT=48321
        const val CONFIG_DIR = "remotes"
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
        if(restart) recreate()
        else Toast.makeText(this, "Press again to Restart",Toast.LENGTH_SHORT).show()
        restart = true
        Handler().postDelayed({ restart = false },1400)
    }
}
