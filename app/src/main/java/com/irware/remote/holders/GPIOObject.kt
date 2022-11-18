package com.irware.remote.holders

import com.irware.remote.ESPUtilsApp
import com.irware.remote.R
import com.irware.remote.listeners.OnGPIORefreshListener
import org.json.JSONObject

class GPIOObject(var jsonObj: JSONObject) {
    var onGPIORefreshListener: OnGPIORefreshListener? = null
    var parent: GPIOConfig? = null
    val deviceProperties: DeviceProperties
        get(){
            for(prop in ESPUtilsApp.devicePropList)
                if(prop.macAddress == macAddr)
                    return prop
            return DeviceProperties()
        }

    var title: String = jsonObj.optString(ESPUtilsApp.getString(R.string.gpio_obj_title), "")
        get() {
            return jsonObj.optString(ESPUtilsApp.getString(R.string.gpio_obj_title), "")
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.gpio_obj_title), value)
            parent?.update()
        }

    var subTitle: String = jsonObj.optString(ESPUtilsApp.getString(R.string.gpio_obj_subtitle), "")
        get(){
            return jsonObj.optString(ESPUtilsApp.getString(R.string.gpio_obj_subtitle), "")
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.gpio_obj_subtitle), value)
            parent?.update()
        }

    var macAddr: String = jsonObj.optString(ESPUtilsApp.getString(R.string.gpio_obj_mac_address), "")
        get() {
            return jsonObj.optString(ESPUtilsApp.getString(R.string.gpio_obj_mac_address), "")
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.gpio_obj_mac_address), value)
            parent?.update()
        }

    var gpioNumber: Int = jsonObj.optInt(ESPUtilsApp.getString(R.string.gpio_obj_pin_number), 0)
        get() {
            return jsonObj.optInt(ESPUtilsApp.getString(R.string.gpio_obj_pin_number), 0)
        }
        set(value){
            field = value
            jsonObj.put(ESPUtilsApp.getString(R.string.gpio_obj_pin_number), value)
            parent?.update()
        }

    var pinValue = 0

    constructor(json: JSONObject, parent: GPIOConfig) : this(json) {
        this.parent = parent
        val curProp = jsonObj
        jsonObj = parent.addGPIO(jsonObj)?: jsonObj
        curProp.keys().forEach {
            jsonObj.put(it, curProp.get(it))
        }
        parent.update()
        deviceProperties.pinConfig.add(this)
    }

    fun delete(): Boolean{
        return parent?.removeGPIO(jsonObj)?: false
    }
}