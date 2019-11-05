package com.irware.remote

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.annotation.RequiresApi
import android.support.design.widget.NavigationView
import android.support.design.widget.TextInputLayout
import android.support.v4.app.Fragment
import android.support.v4.util.ArraySet
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import com.irware.remote.listeners.OnValidationListener
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.BlurBuilder
import com.irware.remote.ui.fragments.AboutFragment
import com.irware.remote.ui.fragments.HomeFragment
import com.irware.remote.ui.fragments.ManageRemoteFragment
import com.irware.remote.ui.fragments.OnFragmentInteractionListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import org.json.JSONObject
import java.io.*
import java.net.InetAddress
import java.net.Socket
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var homeFragment:HomeFragment?=null
    private var manageRemoteFragment : ManageRemoteFragment?=null
    private var aboutFragment: AboutFragment?=null
    private var ipList=ArrayList<String>()
    private var ipConf : File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ipConf = File(filesDir.absolutePath+File.separator+"iplist.conf")
        if(!ipConf!!.exists())ipConf!!.createNewFile()
        val splash=Dialog(this,android.R.style.Theme_Light_NoTitleBar_Fullscreen)
        val splashView=layoutInflater.inflate(R.layout.splash_screen,null)
        splash.setContentView(splashView)

        val originalBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_background)
        val blurredBitmap = BlurBuilder.blur(this, originalBitmap)
        splashView.background = BitmapDrawable(resources, blurredBitmap)
        splash.window?.attributes?.windowAnimations = R.style.DialogAnimationTheme
        splash.setCancelable(false)
        splash.show()
        windowManager.defaultDisplay.getSize(size)

        val logo=splash.findViewById<ImageView>(R.id.splash_logo)
        logo.layoutParams=LinearLayout.LayoutParams((min(size.x,size.y)*0.6F).roundToInt(),(min(size.x,size.y)*0.6F).roundToInt())

        Handler().postDelayed({
            val loginCard=splash.findViewById<LinearLayout>(R.id.login_view)

            val cardAnim=AnimationUtils.loadAnimation(this,R.anim.expand)
            val logoAnim=AnimationUtils.loadAnimation(this,R.anim.move)

            loginCard.visibility=View.VISIBLE
            logo.startAnimation(logoAnim)
            loginCard.startAnimation(cardAnim)

            if (!authenticated) {
                validate(splash,object:OnValidationListener{
                    override fun onValidated(verified: Boolean) {
                        splash.dismiss()
                        setContentView(R.layout.activity_main)
                        setNavView()
                    }
                })
            }else{
                splash.dismiss()
                setContentView(R.layout.activity_main)
                setNavView()
            }
        },2000)
        Thread{
            ipList= BufferedReader(InputStreamReader(ipConf!!.inputStream())).readLines() as ArrayList<String>

            for(ip in ipList){
                if(InetAddress.getByName(ip).isReachable(100)){
                    try{
                        val connector=SocketClient.Connector(ip)
                        connector.sendLine("{\"request\":\"ping\",\"username\":\"__\",\"password\":\"__\",\"data\":\"__\"}")
                        MCU_MAC = JSONObject(connector.readLine())["MAC"] as String
                        connector.close()
                        runOnUiThread {
                            splash.findViewById<EditText>(R.id.editTextIP).setText(ip)
                        }
                        ipList.remove(ip)
                        ipList.add(0,ip)
                    }catch(ex: IOException){}
                }
            }

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
        if(homeFragment==null)
            homeFragment=HomeFragment()
        replaceFragment(homeFragment as Fragment)
    }

    private fun validate(splash:Dialog, validatedListener:OnValidationListener){
        val pref=getSharedPreferences("login",0)
        val editor=pref.edit()

        val ipAddr= splash.findViewById<EditText>(R.id.editTextIP)
        val pass= splash.findViewById<EditText>(R.id.editTextPassword)
        pass.setText(pref.getString("password",""))

        val uname= splash.findViewById<EditText>(R.id.edit_text_uname)
        uname.setText(pref.getString("username",""))
        val  submit= splash.findViewById<Button>(R.id.cirLoginButton)

        submit.setOnClickListener {
            val ip = ipAddr.text.toString()
            Thread{
                if(InetAddress.getByName(ip).isReachable(500)) {
                    try{
                        val connector = SocketClient.Connector(ip)

                        connector.sendLine("{\"request\":\"authenticate\",\"username\":\""+uname.text.toString()+"\",\"password\":\""+pass.text.toString()+"\",\"data\":\"__\"}")
                        val response=connector.readLine()
                        if(response=="verified"){
                            if(!ipList.contains(ip)){
                                ipList.add(0,ip)
                            }
                            runOnUiThread {
                                authenticated=true
                                editor.putString("username",uname.text.toString())
                                editor.putString("password",pass.text.toString())
                                editor.apply()
                                validatedListener.onValidated(true)
                            }
                        }else{
                            runOnUiThread {
                                Toast.makeText(this@MainActivity,"Authentication failed, please check credentials",Toast.LENGTH_LONG).show()
                            }
                        }

                    }catch(ex:IOException){
                        runOnUiThread {
                            Toast.makeText(this@MainActivity,"ip is not of a valid iRWaRE device",Toast.LENGTH_LONG).show()
                        }
                    }

                }else{
                    runOnUiThread {
                        Toast.makeText(this@MainActivity,"!--Ip is not reachable--!",Toast.LENGTH_LONG).show()
                    }
                }
                val writer=OutputStreamWriter(ipConf!!.outputStream())
                writer.write(TextUtils.join("\n",ipList))
                writer.flush()
                writer.close()
            }.start()
        }

        if(ipAddr.text.isNotEmpty() and uname.text.isNotEmpty() and pass.text.isNotEmpty()){
            submit.callOnClick()
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings ->{
                val intent= Intent(this,SettingsActivity :: class.java )
                startActivity(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.

        when (item.itemId) {
            R.id.home_drawer -> {
                if(homeFragment==null)
                    homeFragment=HomeFragment()
                replaceFragment(homeFragment as Fragment)
            }
            R.id.manage_remote_drawer -> {
                if(manageRemoteFragment==null)
                    manageRemoteFragment=ManageRemoteFragment()
                replaceFragment(manageRemoteFragment as Fragment)
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

    private fun replaceFragment(fragment:Fragment){
        supportFragmentManager.beginTransaction().replace(R.id.include_content,fragment).commit()
    }

    companion object {
        val size:Point=Point();
        private var authenticated=false
        public val PORT=48321
        public var MCU_MAC=""
    }
}
