package com.irware.remote.net

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.irware.remote.MainActivity
import java.io.*
import java.lang.Thread.sleep
import java.net.Socket

object SocketClient{

    class Connector(ip:String){
        private val soc = Socket(ip,MainActivity.PORT)
        private val br = BufferedReader(InputStreamReader(soc.getInputStream()))
        private val bw = BufferedWriter(OutputStreamWriter(soc.getOutputStream()))

        fun sendLine(content:String){
            bw.write(content)
            bw.newLine()
            bw.flush()
        }

        fun readLine():String{
            return br.readLine()
        }

        fun close(){
            soc.close()
        }
    }

    fun authenticate(ipAddr:String, user_name:String, passwd:String): Boolean {
        return true
    }

    fun readIrCode(irlistener:IrCodeListener,pref:SharedPreferences) {
        Thread {
            var ip = pref.getString("mcu_ip", "192.168.1.1")
            var sock = Socket("192.168.1.1", 48321)

            var dis = DataInputStream(sock.getInputStream())
            var dos = DataOutputStream(sock.getOutputStream())

            dos.write("{\"task\":\"irrecv\"}\n".toByteArray())

            var code = dis.readUTF()
            sock.close()
            irlistener.onIrRead(code)
        }.start()
    }

    fun sendIrCode(array:IntArray,pref:SharedPreferences) {
        Thread {
            var ip = pref.getString("mcu_ip", "192.168.1.1")
            var sock = Socket(ip, 4832)
            var dos = DataOutputStream(sock.getOutputStream())
            dos.write(("{\"task\":\"irsend\",\"code\":"+array.getString()+"}\n").toByteArray())

            sock.close()
        }.start()
    }

}

private fun IntArray.getString(): String? {
    var str="["
    for(i in 0..size-2){
        str+=this[i].toString()+","
    }
    str+=this[size-1].toString()+"]"
    return str
}

interface IrCodeListener{
    fun onIrRead(code:String)
}