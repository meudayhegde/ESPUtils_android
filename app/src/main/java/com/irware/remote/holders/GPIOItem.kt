package com.irware.remote.holders

class GPIOItem(val gpioName: String){
    val pinNumber = gpioName.split(" ")[0].filter { it.isDigit() }.toInt()
    override fun toString(): String {
        return gpioName
    }
}