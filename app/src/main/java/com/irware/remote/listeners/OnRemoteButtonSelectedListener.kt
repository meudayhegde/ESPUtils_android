package com.irware.remote.listeners

import org.json.JSONObject

interface OnRemoteButtonSelectedListener{
    fun onSelected(jsonObject: JSONObject)
}