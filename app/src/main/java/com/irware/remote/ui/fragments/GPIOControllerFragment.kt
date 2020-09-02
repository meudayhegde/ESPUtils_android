package com.irware.remote.ui.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Switch
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.net.SocketClient
import com.irware.remote.ui.adapters.GPIOListAdapter
import com.irware.remote.ui.adapters.RemoteListAdapter
import java.io.File

class GPIOControllerFragment : androidx.fragment.app.Fragment()  {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var rootView: RelativeLayout? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(rootView == null ){
            rootView = inflater.inflate(R.layout.fragment_gpio_controller, container, false) as RelativeLayout
            viewManager = LinearLayoutManager(context)
            viewAdapter = GPIOListAdapter(MainActivity.remotePropList,0)
            recyclerView = rootView!!.findViewById<RecyclerView>(R.id.manage_remotes_recycler_view).apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
            rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_gpio).setClosedOnTouchOutside(true)

            val refreshLayout = rootView!!.findViewById<SwipeRefreshLayout>(R.id.refresh_layout)
            refreshLayout.setOnRefreshListener {
                refreshLayout.isRefreshing = true
                Thread{
                    Thread.sleep(5000)
                    refreshLayout.isRefreshing = false
                }.start()
            }
//            rootView!!.findViewById<FloatingActionButton>(R.id.fab_new_switch).setOnClickListener(this)
            val switch = rootView!!.findViewById<Switch>(R.id.led_switch)
            val switchLayout = rootView!!.findViewById<CardView>(R.id.layout_switch)
            switch.setOnCheckedChangeListener { _, isChecked ->
                Thread {
                    try {
                        val pref = context!!.getSharedPreferences("login",Context.MODE_PRIVATE)
                        val connector = SocketClient.Connector(pref.getString("lastIP", "")!!)
                        connector.sendLine(
                            "{\"request\":\"gpio_set\",\"username\":\""
                                    + pref.getString("username","") + "\",\"password\":\""
                                    + pref.getString("password","") + "\",\"pinNumber\":12,\"pinMode\":\"OUTPUT\",\"pinValue\":${if(isChecked) 1 else 0}}")
                        val result = connector.readLine()
                        connector.close()
                    }catch(ex:Exception){ }
                }.start()
            }
            switchLayout.setOnClickListener {
                switch.toggle()
            }
        }
        val manageMenu = rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_gpio)
        if(!manageMenu.isOpened)
            manageMenu.hideMenuButton(false)
        Handler().postDelayed({
            if(manageMenu.isMenuButtonHidden)
                manageMenu.showMenuButton(true)
            if(MainActivity.remotePropList.isEmpty())
                Handler().postDelayed({manageMenu.showMenu(true)},400)
        },400)
        return rootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
