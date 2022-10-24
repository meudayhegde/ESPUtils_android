package com.irware.remote.holders

import com.irware.remote.MainActivity
import org.json.JSONObject

class GPIOObject(var jsonObj: JSONObject) {

    var parent:GPIOConfig? = null
    var deviceProperties: DeviceProperties? = null

    constructor(json: JSONObject, parent: GPIOConfig) : this(json) {
        this.parent = parent
        val curProp = jsonObj
        jsonObj = parent.addGPIO(jsonObj)?:jsonObj
        curProp.keys().forEach {
            jsonObj.put(it,curProp.get(it))
        }
        parent.update()
        macAddr.intern()
        deviceProperties!!.pinConfig.add(this)
    }

    var title: String = jsonObj.optString("title", "")
        get() { return jsonObj.optString("title", "")}
        set(value){ field = value;jsonObj.put("title", value); parent?.update()}
    var subTitle: String = jsonObj.optString("subTitle", "")
        get() { return jsonObj.optString("subTitle", "")}
        set(value){ field = value;jsonObj.put("subTitle", value); parent?.update()}
    var macAddr: String = jsonObj.optString("macAddr", "")
        get() {
            if (deviceProperties == null) for(prop in MainActivity.devicePropList) {
                if(prop.macAddress == field) {deviceProperties = prop; break}
            }
            return jsonObj.optString("macAddr", "")
        }
        set(value){
            field = value;
            jsonObj.put("macAddr", value);
            parent?.update()
            if (deviceProperties == null) for(prop in MainActivity.devicePropList) {
                if(prop.macAddress == field) {deviceProperties = prop; break}
            }
        }
    var gpioNumber: Int = jsonObj.optInt("gpioNumber", 0)
        get() { return jsonObj.optInt("gpioNumber", 0)}
        set(value){ field = value; jsonObj.put("gpioNumber", value); parent?.update() }

    var pinValue = 0
}