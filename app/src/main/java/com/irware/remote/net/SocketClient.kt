package com.irware.remote.net

import android.os.Handler
import android.os.Looper
import java.lang.Thread.sleep

class SocketClient{
    fun authenticate(ipAddr:String, user_name:String, passwd:String): Boolean {
        return true
    }

    fun readIrCode(irlistener:IrCodeListener){
        Thread{
            sleep(5000)
            Handler(Looper.getMainLooper()).post { irlistener.onIrRead("TEST") }
        }.start()
    }
}

interface IrCodeListener{
    fun onIrRead(code:String)
}