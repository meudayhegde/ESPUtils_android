package com.github.meudayhegde.esputils.listeners

interface OnOTAIntermediateListener {
    fun onStatusUpdate(status: String, progress: Boolean)
    fun onProgressUpdate(fileLength: Long, offset: Long)
    fun onError(message: String)
}