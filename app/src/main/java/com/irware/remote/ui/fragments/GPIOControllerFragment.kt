package com.irware.remote.ui.fragments

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.android.material.textfield.TextInputEditText
import com.irware.ThreadHandler
import com.irware.remote.ESPUtilsApp
import com.irware.remote.R
import com.irware.remote.holders.GPIOItem
import com.irware.remote.holders.GPIOObject
import com.irware.remote.listeners.OnFragmentInteractionListener
import com.irware.remote.ui.adapters.GPIOListAdapter
import org.json.JSONObject

class GPIOControllerFragment : androidx.fragment.app.Fragment()  {
    private var listener: OnFragmentInteractionListener? = null
    private var recyclerView: RecyclerView? = null
    private var viewAdapter: GPIOListAdapter? = null
    private var viewManager: RecyclerView.LayoutManager? = null
    private var rootView: RelativeLayout? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(rootView == null ){
            rootView = inflater.inflate(R.layout.fragment_gpio_controller, container, false) as RelativeLayout
            viewManager = LinearLayoutManager(context)
            viewAdapter = GPIOListAdapter(ESPUtilsApp.gpioObjectList, this)
            recyclerView = rootView!!.findViewById<RecyclerView>(R.id.manage_remotes_recycler_view).apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
            rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_gpio).setClosedOnTouchOutside(true)

            val refreshLayout = rootView!!.findViewById<SwipeRefreshLayout>(R.id.refresh_layout)
            refreshLayout.setOnRefreshListener {
                refreshLayout.isRefreshing = true
                ESPUtilsApp.devicePropList.filter {
                    it.pinConfig.size > 0
                }.forEach {
                    it.refreshGPIOStatus()
                }
                ThreadHandler.runOnFreeThread{
                    Thread.sleep(100)
                    ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
                        refreshLayout.isRefreshing = false
                    }
                }
            }
            rootView!!.findViewById<FloatingActionButton>(R.id.fab_new_switch).setOnClickListener { gpioDialog() }
        }
        val manageMenu = rootView!!.findViewById<FloatingActionMenu>(R.id.fam_manage_gpio)
        if(!manageMenu.isOpened)
            manageMenu.hideMenuButton(false)
        Handler(Looper.getMainLooper()).postDelayed({
            if(manageMenu.isMenuButtonHidden)
                manageMenu.showMenuButton(true)
            if(ESPUtilsApp.remotePropList.isEmpty())
                Handler(Looper.getMainLooper()).postDelayed({manageMenu.showMenu(true)},400)
        },400)
        return rootView
    }

    fun gpioDialog(gpioObject: GPIOObject? = null){
        val gpioDialog = AlertDialog.Builder(requireContext(), R.style.AppTheme_AlertDialog)
            .setIcon(R.drawable.icon_lamp)
            .setTitle(resources.getString(R.string.add_new_gpio_switch))
            .setNegativeButton(R.string.cancel) { p0, _ -> p0.dismiss() }
            .setNeutralButton(R.string.delete){ _, _ -> }
            .setPositiveButton(R.string.confirm) {_, _ -> }
            .setView(R.layout.layout_new_gpio_switch)
            .create()
        gpioDialog.setOnShowListener {
            val devicesSpinner = gpioDialog.findViewById<Spinner>(R.id.select_device)!!
            val gpioSpinner = gpioDialog.findViewById<Spinner>(R.id.pin_number)!!
            val btnPositive = gpioDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            val btnNeutral = gpioDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
            btnNeutral.visibility = View.GONE

            val gpioList = arrayListOf(GPIOItem(getString(R.string.pin)))
            requireContext().resources.getStringArray(R.array.esp_gpio).forEach { gpioList.add(GPIOItem(it)) }
            gpioSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, gpioList)

            val devicePropList = arrayListOf<Any>(getString(R.string.select_device))
            devicePropList.addAll(ESPUtilsApp.devicePropList)

            devicesSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, devicePropList)
            val nameText = gpioDialog.findViewById<TextInputEditText>(R.id.edit_text_switch_name)!!
            val description = gpioDialog.findViewById<TextInputEditText>(R.id.switch_desc)!!

            gpioObject?.let{gpio ->
                nameText.setText(gpio.title)
                description.setText(gpio.subTitle)

                gpioDialog.setTitle(R.string.edit_gpio_switch)
                btnPositive.setText(R.string.save)
                devicesSpinner.setSelection(ESPUtilsApp.devicePropList.indexOf(ESPUtilsApp.devicePropList.find { it.macAddress == gpio.macAddr }) + 1)
                gpioSpinner.setSelection(gpioList.indexOf(gpioList.find { it.pinNumber == gpio.gpioNumber }))

                gpioSpinner.isEnabled = false
                devicesSpinner.isEnabled = false

                btnNeutral.visibility = View.VISIBLE
                btnNeutral.setOnClickListener {
                    AlertDialog.Builder(requireContext(), R.style.AppTheme_AlertDialog)
                        .setIcon(R.drawable.icon_delete)
                        .setTitle(R.string.confirm_delete)
                        .setMessage(getString(R.string.message_delete_gpio_switch, gpio.title))
                        .setNegativeButton(R.string.cancel){ _, _ -> }
                        .setPositiveButton(R.string.delete){_, _ ->
                            val index = ESPUtilsApp.gpioObjectList.indexOf(gpio)
                            ESPUtilsApp.gpioObjectList.removeAt(index)
                            gpio.delete()
                            viewAdapter?.notifyItemRemoved(index)
                            gpioDialog.dismiss()
                        }.show()
                }
            }

            btnPositive.setOnClickListener {
                when {
                    devicesSpinner.selectedItemPosition == 0 -> {
                        Toast.makeText(requireContext(), R.string.please_select_device, Toast.LENGTH_SHORT).show()
                    }
                    gpioSpinner.selectedItemPosition == 0 -> {
                        Toast.makeText(requireContext(), R.string.please_select_gpio_pin_number, Toast.LENGTH_SHORT).show()
                    }
                    nameText.text?.isEmpty()?: false -> {
                        nameText.setText(R.string.name_field_cannot_be_empty)
                    }
                    else -> {
                        val gpioObj = gpioObject?: GPIOObject(JSONObject(), ESPUtilsApp.gpioConfig!!)
                        gpioObj.macAddr = ESPUtilsApp.devicePropList[devicesSpinner.selectedItemPosition - 1].macAddress
                        gpioObj.title = nameText.text.toString()
                        gpioObj.subTitle = description.text.toString()
                        gpioObj.subTitle = description.text.toString()
                        gpioObj.gpioNumber = gpioList[gpioSpinner.selectedItemPosition].pinNumber

                        if(gpioObject == null ){
                            ESPUtilsApp.gpioObjectList.add(gpioObj)
                            viewAdapter?.notifyItemInserted(ESPUtilsApp.gpioObjectList.size - 1)
                        }else{
                            viewAdapter?.notifyItemChanged(ESPUtilsApp.gpioObjectList.indexOf(gpioObj))
                        }
                        gpioDialog.dismiss()
                    }
                }
            }
        }
        gpioDialog.show()
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
