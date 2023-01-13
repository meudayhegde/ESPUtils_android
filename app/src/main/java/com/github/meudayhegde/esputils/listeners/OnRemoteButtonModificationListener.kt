package com.github.meudayhegde.esputils.listeners

interface OnRemoteButtonModificationListener{
    fun onIconModified()
    fun onTypeModified()
    fun onTextModified()
    fun onColorModified()
    fun onIrModified()
    fun onPositionModified()
    fun onTextColorChanged()
}