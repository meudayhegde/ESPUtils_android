package com.irware.remote.holders

class GPIOItem(private val gpioName: String){
    private val numStr = gpioName.split(" ")[0].filter { it.isDigit() }
    val pinNumber = (numStr.ifEmpty { "-1" }).toInt()
    override fun toString(): String {
        return gpioName
    }
}