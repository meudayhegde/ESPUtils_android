package com.irware.remote.listeners

interface OnOTAIntermediateListener{
    fun onStatusUpdate(status: String, progress: Boolean)
    fun onProgressUpdate(progress: Float)
    fun onError(message: String)
}