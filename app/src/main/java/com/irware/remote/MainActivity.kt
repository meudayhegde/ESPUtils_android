package com.irware.remote

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import com.irware.remote.ui.fragments.AboutFragment
import com.irware.remote.ui.fragments.CreateRemoteFragment
import com.irware.remote.ui.fragments.HomeFragment
import com.irware.remote.ui.fragments.ManageRemoteFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    HomeFragment.OnFragmentInteractionListener,AboutFragment.OnFragmentInteractionListener,
    CreateRemoteFragment.OnFragmentInteractionListener,ManageRemoteFragment.OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var homeFragment:HomeFragment?=null
    private var createRemoteFragment : CreateRemoteFragment?=null
    private var manageRemoteFragment : ManageRemoteFragment?=null
    private var aboutFragment: AboutFragment?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val empty_overlay= Dialog(this,R.style.AppTheme_NoActionBar);
        empty_overlay.setCancelable(false)
        empty_overlay.show()

        if(validate())
            empty_overlay.dismiss()

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    fun validate():Boolean{

        val login_dialog=object:Dialog(this,R.style.AppTheme){
            override fun onBackPressed(){
                finish()
            }
        }

        val login_view=getLayoutInflater().inflate(R.layout.layout_login,null) as ConstraintLayout
        login_dialog.setContentView(login_view)
        val ipAddr= login_view.findViewById<EditText>(R.id.et_ip_addr)
        val pass= login_view.findViewById<EditText>(R.id.et_password)
        val uname= login_view.findViewById<EditText>(R.id.et_user_name)
        val  submit=login_view.findViewById<Button>(R.id.btn_submit)
        login_dialog.setCancelable(false)
        login_dialog.show()

        submit.setOnClickListener {
            login_dialog.dismiss()
        }

        return true
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
                    homeFragment=HomeFragment.newInstance()
                replaceFragment(homeFragment as Fragment)
            }
            R.id.create_remote_drawer -> {
                if(createRemoteFragment==null)
                    createRemoteFragment=CreateRemoteFragment.newInstance()
                replaceFragment(createRemoteFragment as Fragment)
            }
            R.id.manage_remote_drawer -> {
                if(manageRemoteFragment==null)
                    manageRemoteFragment=ManageRemoteFragment.newInstance()
                replaceFragment(manageRemoteFragment as Fragment)
            }
            R.id.info_drawer -> {
                if(aboutFragment==null)
                    aboutFragment=AboutFragment.newInstance()
                replaceFragment(aboutFragment as Fragment)
            }
            R.id.share_drawer -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun replaceFragment(fragment:Fragment){
        getSupportFragmentManager().beginTransaction().replace(R.id.include_content,fragment).commit()
    }
}
