package com.github.meudayhegde.esputils.listeners

interface OnDeviceStatusListener{
    fun onBeginRefresh()
    fun onStatusUpdate(connected: Boolean)
}
