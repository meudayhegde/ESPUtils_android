package com.irware.remote

import android.annotation.SuppressLint
import android.app.Dialog
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

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        remotePropList.clear()
        activity = this
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

        val ipAddr= splash.findViewById<EditText>(R.id.editTextIP)
        val pass= splash.findViewById<EditText>(R.id.editTextPassword)
        pass.setText(pref.getString("password",""))

        val uname= splash.findViewById<EditText>(R.id.edit_text_uname)
        uname.setText(pref.getString("username",""))
        val  submit= splash.findViewById<Button>(R.id.cirLoginButton)
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
            val ip = ipAddr.text.toString()
            Thread{
                if(InetAddress.getByName(ip).isReachable(500)) {
                    try{
                        val connector = SocketClient.Connector(ip)

                        connector.sendLine("{\"request\":\"authenticate\",\"username\":\""+uname.text.toString()+"\",\"password\":\""+pass.text.toString()+"\",\"data\":\"__\",\"length\":0}")
                        val response=connector.readLine()
                        connector.close()
                        if(JSONObject(response)["response"]=="authenticated"){
                            MCU_IP = ip
                            USERNAME = uname.text.toString()
                            PASSWORD = pass.text.toString()
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

        Thread{
            ipList= BufferedReader(InputStreamReader(ipConf!!.inputStream())).readLines() as ArrayList<String>

            for(ip:String in ipList){
                if(InetAddress.getByName(ip).isReachable(100)){
                    try{
                        val connector=SocketClient.Connector(ip)
                        connector.sendLine("{\"request\":\"ping\",\"username\":\"__\",\"password\":\"__\",\"data\":\"__\",\"length\":0}")
                        MCU_MAC = JSONObject(connector.readLine())["MAC"] as String
                        connector.close()
                        runOnUiThread {
                            splash.findViewById<EditText>(R.id.editTextIP).setText(ip)
                            if(ipAddr.text.isNotEmpty() and uname.text.isNotEmpty() and pass.text.isNotEmpty()){
                                submit.callOnClick()
                            }
                        }
                        ipList.remove(ip)
                        ipList.add(0,ip)
                    }catch(ex: IOException){}
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
    }

    private fun min(x:Int, y:Int):Int{
        return if(x<y) x else y
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


    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
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
        val iconDrawableList=intArrayOf(R.drawable.icon_transparent,R.drawable.icon_power, R.drawable.icon_info, R.drawable.icon_media_next,
            R.drawable.icon_media_pause, R.drawable.icon_media_previous,R.drawable.icon_mic, R.drawable.icon_search,
            R.drawable.icon_volume, R.drawable.icon_wifi,R.drawable.icon_bluetooth,R.drawable.icon_alert,
            R.drawable.icon_cancel, R.drawable.icon_fast_forward,R.drawable.icon_fast_rewind,R.drawable.icon_flight_mode,
            R.drawable.icon_home, R.drawable.icon_back,R.drawable.icon_backspace,R.drawable.icon_block,
            R.drawable.icon_edit,R.drawable.icon_delete)
    }

    private var restart = false
    fun onRestartClicked(view: View) {
        if(restart) recreate()
        else Toast.makeText(this, "Press again to Restart",Toast.LENGTH_SHORT).show()
        restart = true
        Handler().postDelayed({ restart = false },1400)
    }

}
