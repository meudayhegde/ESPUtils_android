package com.github.meudayhegde.esputils.listeners

interface OnGPIORefreshListener {
    fun onRefreshBegin()
    fun onRefresh(pinValue: Int)
}