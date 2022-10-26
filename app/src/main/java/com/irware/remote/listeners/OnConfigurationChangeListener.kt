package com.irware.remote.listeners

import android.content.res.Configuration

interface OnConfigurationChangeListener{
    var keepAlive:Boolean
    fun onConfigurationChanged(config: Configuration)
}