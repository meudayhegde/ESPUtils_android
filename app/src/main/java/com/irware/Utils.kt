package com.irware

import java.net.NetworkInterface
import java.util.*
import kotlin.collections.ArrayList


fun getIPAddress(): ArrayList<String> {

    val result = ArrayList<String>()
    try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (`interface` in interfaces) {
            if(`interface`.name.contains("wlan")){
                val addrs = Collections.list(`interface`.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        if (sAddr.indexOf(':') < 0)
                            result.add(sAddr)
                    }
                }
            }
        }
    } catch (ignored: Exception) { }
    return result
}