package com.github.meudayhegde.esputils.holders

import com.github.meudayhegde.esputils.ESPUtilsApp
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.listeners.OnGPIORefreshListener
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

    var title: String = jsonObj.optString(Strings.gpioObjTitle, "")
        get() {
            return jsonObj.optString(Strings.gpioObjTitle, "")
        }
        set(value){
            field = value
            jsonObj.put(Strings.gpioObjTitle, value)
            parent?.update()
        }

    var subTitle: String = jsonObj.optString(Strings.gpioObjSubtitle, "")
        get(){
            return jsonObj.optString(Strings.gpioObjSubtitle, "")
        }
        set(value){
            field = value
            jsonObj.put(Strings.gpioObjSubtitle, value)
            parent?.update()
        }

    var macAddr: String = jsonObj.optString(Strings.gpioObjMACAddress, "")
        get() {
            return jsonObj.optString(Strings.gpioObjMACAddress, "")
        }
        set(value){
            field = value
            jsonObj.put(Strings.gpioObjMACAddress, value)
            parent?.update()
        }

    var gpioNumber: Int = jsonObj.optInt(Strings.gpioObjPinNumber, 0)
        get() {
            return jsonObj.optInt(Strings.gpioObjPinNumber, 0)
        }
        set(value){
            field = value
            jsonObj.put(Strings.gpioObjPinNumber, value)
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