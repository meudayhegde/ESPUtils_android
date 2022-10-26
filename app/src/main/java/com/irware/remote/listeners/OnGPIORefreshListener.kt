package com.irware.remote.listeners

interface OnGPIORefreshListener{
    fun onRefreshBegin()
    fun onRefresh(pinValue: Int)
}