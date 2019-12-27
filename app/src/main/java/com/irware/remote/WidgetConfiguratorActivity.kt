package com.irware.remote

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.ui.adapters.RemoteListAdapter
import com.irware.remote.ui.buttons.RemoteButton
import com.irware.remote.ui.dialogs.RemoteDialog
import kotlinx.android.synthetic.main.activity_widget_configurator.*
import kotlinx.android.synthetic.main.controllers_refresh_layout.*
import java.io.File
import kotlin.math.min

class WidgetConfiguratorActivity : AppCompatActivity(),SwipeRefreshLayout.OnRefreshListener {

    var widgetId = 0
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    val remotePropList = ArrayList<RemoteProperties>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
        when(getSharedPreferences("theme_setting", Context.MODE_PRIVATE).getInt("application_theme",if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { 0 }else{ 2 }))
        {1-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);2-> AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_YES)}
        setContentView(R.layout.activity_widget_configurator)

        widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        window?.setBackgroundDrawableResource(R.drawable.layout_border_round_corner)
        val lWindowParams = WindowManager.LayoutParams()
        lWindowParams.copyFrom(window?.attributes)
        lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT
        lWindowParams.height = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes = lWindowParams

        windowManager.defaultDisplay.getSize(MainActivity.size)
        val x = min(MainActivity.size.x, MainActivity.size.y)
        MainActivity.NUM_COLUMNS = when{x>920->5;x<720->3;else->4}
        lWindowParams.width = MainActivity.size.x*7/8
        lWindowParams.height = MainActivity.size.y*6/8
        window?.attributes = lWindowParams
        RemoteButton.onConfigChanged()

        val arr = resources.obtainTypedArray(R.array.icons)
        MainActivity.iconDrawableList = IntArray(arr.length())
        for(i in 0 until arr.length())
            MainActivity.iconDrawableList[i] = arr.getResourceId(i,0)
        arr.recycle()

        viewManager = LinearLayoutManager(this)
        viewAdapter = RemoteListAdapter(remotePropList,RemoteDialog.MODE_SELECT_BUTTON)
        manage_remotes_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        (remote_refresh_layout as SwipeRefreshLayout).setOnRefreshListener(this)
        onRefresh()
    }

    override fun onRefresh() {
        (remote_refresh_layout as SwipeRefreshLayout).isRefreshing = true
        Thread{
            remotePropList.clear()
            val files = File(filesDir.absolutePath + File.separator + MainActivity.CONFIG_DIR).listFiles { pathname ->
                pathname!!.isFile and (pathname.name.endsWith(
                    ".json",
                    true
                )) and pathname.canWrite()
            }
            files.forEach {
                remotePropList.add(RemoteProperties(it, null))
            }
            runOnUiThread{
                viewAdapter.notifyDataSetChanged()
                (remote_refresh_layout as SwipeRefreshLayout).isRefreshing = false
            }
        }.start()
    }

    companion object{
        var activity:WidgetConfiguratorActivity? = null
    }
}
