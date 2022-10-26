package com.irware.remote.listeners

import org.json.JSONObject

interface IrCodeListener{
    var parentDialog:androidx.appcompat.app.AlertDialog?
    var mode: Int
    fun onIrRead(jsonObj: JSONObject)
    fun onTimeout()
    fun onDeny(err_info: String?)
    fun onProgress(value: Int)
}