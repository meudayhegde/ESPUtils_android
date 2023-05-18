package com.github.meudayhegde.esputils.ui.fragments

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.meudayhegde.ThreadHandler
import com.github.meudayhegde.esputils.ESPUtilsApp
import com.github.meudayhegde.esputils.MainActivity
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.databinding.FragmentGpioControllerBinding
import com.github.meudayhegde.esputils.databinding.LayoutNewGpioSwitchBinding
import com.github.meudayhegde.esputils.holders.GPIOItem
import com.github.meudayhegde.esputils.holders.GPIOObject
import com.github.meudayhegde.esputils.listeners.OnFragmentInteractionListener
import com.github.meudayhegde.esputils.ui.adapters.GPIOListAdapter
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class GPIOControllerFragment : androidx.fragment.app.Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private var viewAdapter: GPIOListAdapter? = null
    private var viewManager: RecyclerView.LayoutManager? = null

    private lateinit var fragmentBinding: FragmentGpioControllerBinding
    private var _binding: FragmentGpioControllerBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            fragmentBinding = FragmentGpioControllerBinding.inflate(inflater, container, false)
            _binding = fragmentBinding
            viewManager = LinearLayoutManager(context)
            viewAdapter = GPIOListAdapter(ESPUtilsApp.gpioObjectList, this)
            fragmentBinding.refreshLayout.refreshLayoutRecyclerView.apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
            fragmentBinding.famMenuMain.setClosedOnTouchOutside(true)

            fragmentBinding.refreshLayout.refreshLayout.setOnRefreshListener {
                fragmentBinding.refreshLayout.refreshLayout.isRefreshing = true
                ESPUtilsApp.devicePropList.filter {
                    it.pinConfig.size > 0
                }.forEach {
                    it.refreshGPIOStatus()
                }
                ThreadHandler.runOnFreeThread {
                    Thread.sleep(100)
                    ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
                        fragmentBinding.refreshLayout.refreshLayout.isRefreshing = false
                    }
                }
            }
            fragmentBinding.fabNewSwitch.setOnClickListener { gpioDialog() }
        }
        if (!fragmentBinding.famMenuMain.isOpened) fragmentBinding.famMenuMain.hideMenuButton(
            false
        )
        Handler(Looper.getMainLooper()).postDelayed({
            if (fragmentBinding.famMenuMain.isMenuButtonHidden) fragmentBinding.famMenuMain.showMenuButton(
                true
            )
            if (ESPUtilsApp.gpioObjectList.isEmpty()) Handler(Looper.getMainLooper()).postDelayed({
                fragmentBinding.famMenuMain.open(
                    true
                )
            }, 250)
        }, 400)
        return fragmentBinding.root
    }

    fun gpioDialog(gpioObject: GPIOObject? = null) {
        val contentBinding = LayoutNewGpioSwitchBinding.inflate(layoutInflater)
        val gpioDialog = AlertDialog.Builder(requireContext(), R.style.AppTheme_AlertDialog)
            .setIcon(R.drawable.icon_lamp)
            .setTitle(resources.getString(R.string.add_new_gpio_switch))
            .setNegativeButton(R.string.cancel) { p0, _ -> p0.dismiss() }
            .setNeutralButton(R.string.delete) { _, _ -> }
            .setPositiveButton(R.string.confirm) { _, _ -> }
            .setView(contentBinding.root)
            .create()
        gpioDialog.setOnShowListener {
            val btnPositive = gpioDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            val btnNeutral = gpioDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
            btnNeutral.visibility = View.GONE

            val gpioList = arrayListOf(GPIOItem(getString(R.string.pin)))
            requireContext().resources.getStringArray(R.array.esp_gpio)
                .forEach { gpioList.add(GPIOItem(it)) }
            contentBinding.gpioSpinner.adapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, gpioList)

            val devicePropList = arrayListOf<Any>(getString(R.string.select_device))
            devicePropList.addAll(ESPUtilsApp.devicePropList)

            contentBinding.deviceSpinner.adapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, devicePropList)

            gpioObject?.let { gpio ->
                contentBinding.nameText.setText(gpio.title)
                contentBinding.switchDesc.setText(gpio.subTitle)

                gpioDialog.setTitle(R.string.edit_gpio_switch)
                btnPositive.setText(R.string.save)
                contentBinding.deviceSpinner.setSelection(
                    ESPUtilsApp.devicePropList.indexOf(ESPUtilsApp.devicePropList.find { it.macAddress == gpio.macAddress }) + 1
                )
                contentBinding.gpioSpinner.setSelection(gpioList.indexOf(gpioList.find { it.pinNumber == gpio.gpioNumber }))

                contentBinding.gpioSpinner.isEnabled = false
                contentBinding.deviceSpinner.isEnabled = false

                btnNeutral.visibility = View.VISIBLE
                btnNeutral.setOnClickListener {
                    AlertDialog.Builder(requireContext(), R.style.AppTheme_AlertDialog)
                        .setIcon(R.drawable.icon_delete).setTitle(R.string.confirm_delete)
                        .setMessage(getString(R.string.message_delete_gpio_switch, gpio.title))
                        .setNegativeButton(R.string.cancel) { _, _ -> }
                        .setPositiveButton(R.string.delete) { _, _ ->
                            val index = ESPUtilsApp.gpioObjectList.indexOf(gpio)
                            ESPUtilsApp.gpioObjectList.removeAt(index)
                            gpio.delete()
                            viewAdapter?.notifyItemRemoved(index)
                            gpioDialog.dismiss()
                            ESPUtilsApp.showAd(context as MainActivity)
                        }.show()
                }
            }

            contentBinding.nameText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    (contentBinding.nameText.parent.parent as TextInputLayout).error = null
                }
            })

            btnPositive.setOnClickListener {
                if (contentBinding.nameText.text?.isEmpty() != false) {
                    (contentBinding.nameText.parent.parent as TextInputLayout).error =
                        context?.getString(R.string.message_name_field_empty)
                }
                when {
                    contentBinding.deviceSpinner.selectedItemPosition == 0 -> {
                        Toast.makeText(
                            requireContext(),
                            R.string.message_gpio_device_no_selection,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    contentBinding.gpioSpinner.selectedItemPosition == 0 -> {
                        Toast.makeText(
                            requireContext(),
                            R.string.message_gpio_pin_no_selection,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    contentBinding.nameText.text?.isEmpty() ?: false -> {
                        Toast.makeText(
                            context, R.string.message_name_field_empty, Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        val gpioObj =
                            gpioObject ?: GPIOObject(JSONObject(), ESPUtilsApp.gpioConfig!!)
                        gpioObj.macAddress =
                            ESPUtilsApp.devicePropList[contentBinding.deviceSpinner.selectedItemPosition - 1].macAddress
                        gpioObj.title = contentBinding.nameText.text.toString()
                        gpioObj.subTitle = contentBinding.switchDesc.text.toString()
                        gpioObj.subTitle = contentBinding.switchDesc.text.toString()
                        gpioObj.gpioNumber =
                            gpioList[contentBinding.gpioSpinner.selectedItemPosition].pinNumber

                        if (gpioObject == null) {
                            ESPUtilsApp.gpioObjectList.add(gpioObj)
                            viewAdapter?.notifyItemInserted(ESPUtilsApp.gpioObjectList.size - 1)
                        } else {
                            viewAdapter?.notifyItemChanged(
                                ESPUtilsApp.gpioObjectList.indexOf(gpioObj)
                            )
                        }
                        ESPUtilsApp.showAd(context as MainActivity)
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
