package com.irware.remote.holders

import com.irware.remote.ESPUtils
import com.irware.remote.listeners.OnGPIORefreshListener
import org.json.JSONObject

class GPIOObject(var jsonObj: JSONObject) {
    var onGPIORefreshListener: OnGPIORefreshListener? = null
    var parent: GPIOConfig? = null
    val deviceProperties: DeviceProperties
        get(){
            for(prop in ESPUtils.devicePropList)
                if(prop.macAddress == macAddr)
                    return prop
            return DeviceProperties()
        }

    var title: String = jsonObj.optString("title", "")
        get() { return jsonObj.optString("title", "")}
        set(value){ field = value; jsonObj.put("title", value); parent?.update()}
    var subTitle: String = jsonObj.optString("subTitle", "")
        get() { return jsonObj.optString("subTitle", "")}
        set(value){ field = value; jsonObj.put("subTitle", value); parent?.update()}
    var macAddr: String = jsonObj.optString("macAddr", "")
        get() {
            return jsonObj.optString("macAddr", "")
        }
        set(value){
            field = value
            jsonObj.put("macAddr", value)
            parent?.update()
        }
    var gpioNumber: Int = jsonObj.optInt("gpioNumber", 0)
        get() { return jsonObj.optInt("gpioNumber", 0)}
        set(value){ field = value; jsonObj.put("gpioNumber", value); parent?.update() }

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