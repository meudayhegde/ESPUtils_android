package com.irware.remote.listeners

interface OnDeviceStatusListener{
    fun onBeginRefresh()
    fun onStatusUpdate(connected: Boolean)
}
