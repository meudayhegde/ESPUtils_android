package com.irware.remote.holders

class GPIOItem(val gpioName: String){
    private val numStr = gpioName.split(" ")[0].filter { it.isDigit() }
    val pinNumber = (if(numStr.isEmpty()) "-1" else numStr).toInt()
    override fun toString(): String {
        return gpioName
    }
}