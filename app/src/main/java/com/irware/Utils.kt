package com.irware

import java.net.NetworkInterface
import java.util.*
import kotlin.collections.ArrayList


fun getIPAddress(): ArrayList<String> {

    val result = ArrayList<String>()
    try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            val addrs = Collections.list(intf.inetAddresses)
            for (addr in addrs) {
            if (!addr.isLoopbackAddress) {
                val sAddr = addr.hostAddress
                if (sAddr.indexOf(':') < 0)
                    result.add(sAddr)
                }
            }
        }
    } catch (ignored: Exception) { }
    return result
}