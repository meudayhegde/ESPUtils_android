package com.irware.remote

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
        NUM_COLUMNS = when{ size.x<920->3;else->5}
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
        NUM_COLUMNS = when{size.x>920->5;size.x<720->3;else->4}

        val file = File(filesDir.absolutePath+File.separator+ CONFIG_DIR)
        if(!file.exists()) file.mkdir()


        val logo=splash.findViewById<ImageView>(R.id.splash_logo)
        val lparams = RelativeLayout.LayoutParams((min(size.x,size.y)*0.6F).roundToInt(),(min(size.x,size.y)*0.6F).roundToInt())
        lparams.addRule(RelativeLayout.CENTER_IN_PARENT)
        logo.layoutParams = lparams

        RemoteButton.onActivityLoad()

        val pref=getSharedPreferences("login",0)
        val editor=pref.edit()

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
                val writer=OutputStreamWriter(ipConf.outputStream())
                writer.write(TextUtils.join("\n",ipList))
                writer.flush()
                writer.close()
            }.start()
        }

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
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
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


    fun startConfigChooser(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/json"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a remote controller configuration file"), FILE_SELECT_CODE)
        } catch (ex: ActivityNotFoundException) {
            // Potentially direct the user to the Market with a Dialog
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
                        mIntent.putExtra("fromIrware",true)
                        startActivity(Intent.createChooser(mIntent, "Import Config File"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
        }
        super.onActivityResult(requestCode, resultCode, data)
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
