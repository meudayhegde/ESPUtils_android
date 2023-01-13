package com.github.meudayhegde.esputils

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.meudayhegde.ThreadHandler
import com.github.meudayhegde.esputils.holders.RemoteProperties
import com.github.meudayhegde.esputils.ui.adapters.RemoteListAdapter
import com.github.meudayhegde.esputils.ui.buttons.RemoteButton
import com.github.meudayhegde.esputils.ui.dialogs.RemoteDialog
import kotlinx.android.synthetic.main.activity_widget_configurator.*
import kotlinx.android.synthetic.main.recycler_refresh_layout.*
import kotlin.math.min

class RemoteBtnWidgetConfActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    var widgetId = 0
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private val remotePropList = ArrayList<RemoteProperties>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this

        widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val pref = getSharedPreferences(Strings.sharedPrefNameWidgetAssociations, Context.MODE_PRIVATE)

        if(!pref.getString(widgetId.toString(), "").isNullOrEmpty()){
            updateAppWidget()
        }else if(!pref.getString(Strings.sharedPrefItemQueuedButton, "").isNullOrEmpty()){
            pref.edit().putString(Strings.sharedPrefItemQueuedButton, "").apply()
            updateAppWidget()
        }

        when(getSharedPreferences(Strings.sharedPrefNameSettings, Context.MODE_PRIVATE)
            .getInt(Strings.sharedPrefItemApplicationTheme, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            { 0 } else { 2 })){
            1-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        setContentView(R.layout.activity_widget_configurator)

        val lWindowParams = WindowManager.LayoutParams()
        lWindowParams.copyFrom(window?.attributes)
        lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT
        lWindowParams.height = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes = lWindowParams

        MainActivity.layoutParams.width = resources.displayMetrics.widthPixels
        MainActivity.layoutParams.height = resources.displayMetrics.heightPixels

        val width = min(MainActivity.layoutParams.width, MainActivity.layoutParams.height)
        MainActivity.NUM_COLUMNS = when{ width > 920 -> 5; width < 720 -> 3; else -> 4}
        lWindowParams.width = MainActivity.layoutParams.width * 7 / 8
        lWindowParams.height = MainActivity.layoutParams.height * 6 / 8
        window?.attributes = lWindowParams
        RemoteButton.onConfigChanged()

        val arr = resources.obtainTypedArray(R.array.icons)
        ESPUtilsApp.iconDrawableList = IntArray(arr.length())
        for(i in 0 until arr.length())
            ESPUtilsApp.iconDrawableList[i] = arr.getResourceId(i,0)
        arr.recycle()

        viewManager = LinearLayoutManager(this)
        viewAdapter = RemoteListAdapter(remotePropList,RemoteDialog.MODE_SELECT_BUTTON)
        refresh_layout_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        (remote_refresh_layout as SwipeRefreshLayout).setOnRefreshListener(this)
        onRefresh()
    }

    override fun onRefresh() {
        (remote_refresh_layout as SwipeRefreshLayout).isRefreshing = true
        ThreadHandler.runOnFreeThread{
            remotePropList.clear()
            val files = ESPUtilsApp.getPrivateFile(Strings.nameDirRemoteConfig).listFiles { pathname ->
                pathname!!.isFile and (pathname.name.endsWith(Strings.extensionJson, true)) and pathname.canWrite()
            }
            files?.forEach {
                remotePropList.add(RemoteProperties(it, null))
            }
            runOnUiThread{
                viewAdapter.notifyDataSetChanged()
                (remote_refresh_layout as SwipeRefreshLayout).isRefreshing = false
            }
        }
    }

    private fun updateAppWidget(){
        val intent = Intent(this, RemoteButtonWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val  ids = AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, RemoteButtonWidget::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }

    companion object{
        var activity:RemoteBtnWidgetConfActivity? = null
    }
}
