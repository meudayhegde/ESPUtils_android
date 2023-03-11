package com.github.meudayhegde.esputils.listeners

import org.json.JSONObject

interface OnRemoteButtonSelectedListener {
    fun onSelected(jsonObject: JSONObject)
}