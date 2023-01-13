package com.github.meudayhegde.esputils.listeners

import android.content.res.Configuration

interface OnConfigurationChangeListener{
    var keepAlive:Boolean
    fun onConfigurationChanged(config: Configuration)
}