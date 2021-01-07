package com.irware.remote.net

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import com.irware.ThreadHandler
import com.irware.getIPAddress
import com.irware.remote.MainActivity
import com.irware.remote.ui.adapters.insert
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress

class ARPTable(private val context: Context, private val scanCount: Int = -1) {
    private var arpTableFile: File = File(context.filesDir.absolutePath + File.separator + ARP_TABLE_FILE)
    private var jsonObj: JSONObject = getJSONObject()

    init{
        update()
        startScan()
    }

    fun getARPItemList() : ArrayList<ARPItem>{
        val arpItemList = ArrayList<ARPItem>()
        jsonObj.keys().forEach { arpItemList.add(ARPItem(it, jsonObj.getJSONArray(it).getString(0))) }
        return arpItemList
    }

    fun getIpFromMac(mac: String, listener: OnIpListener){
        val addresses = jsonObj.optJSONArray(mac) ?: JSONArray()
        MainActivity.threadHandler?.runOnThread(ThreadHandler.ESP_MESSAGE){
            for(i in 0 until addresses.length()){
                val address = addresses.getString(i)
                if(InetAddress.getByName(address).isReachable(50)){
                    try{
                        val connector = SocketClient.Connector(address)
                        connector.sendLine("{\"request\":\"ping\"}")
                        if(JSONObject(connector.readLine()).getString("MAC") == mac) {
                            if(i!=0){
                                addresses.remove(i)
                                addresses.insert(0, address)
                                update()
                            }
                            if(listener.uiThread) (context as Activity).runOnUiThread {
                                listener.onIpResult(address)
                            }
                            else listener.onIpResult(address)
                            return@runOnThread
                        }
                    }catch(ex: Exception){}
                }
            }
            if(listener.uiThread) (context as Activity).runOnUiThread {
                listener.onIpResult(null)
            } else listener.onIpResult(null)
        }

    }

    fun getIpFromMac(mac: String): String? {
        val addresses = jsonObj.optJSONArray(mac) ?: JSONArray()
        for(i in 0 until addresses.length()){
            return addresses.getString(i)
        }
        return null
    }

    private fun getJSONObject():JSONObject{
        if(!arpTableFile.exists()) {
            arpTableFile.createNewFile()
            arpTableFile = File(arpTableFile.absolutePath)
        }
        val isr = InputStreamReader(arpTableFile.inputStream())
        val content = TextUtils.join("\n", isr.readLines())
        isr.close()
        return try{
            JSONObject(content)
        }catch(ex: JSONException){
            JSONObject()
        }
    }

    private fun update(){
        val osr = OutputStreamWriter(arpTableFile.outputStream())
        osr.write(jsonObj.toString(4))
        osr.flush()
        osr.close()
    }

    private fun startScan(){
        Thread{
            var currentScanCount = 0
            while((scanCount == -1) or (currentScanCount < scanCount)){
                for(myIp in getIPAddress()){
                    @Suppress("NAME_SHADOWING") val myIp = myIp.split(".")
                    val myIpInt = myIp[3].toInt()
                    for(i in 1 until 255){
                        val addrIntAbove = myIpInt + i
                        val addrIntBelow = myIpInt - i
                        if(addrIntAbove < 255){verifyAddress("${myIp[0]}.${myIp[1]}.${myIp[2]}.$addrIntAbove")}
                        if(addrIntBelow >= 0){verifyAddress("${myIp[0]}.${myIp[1]}.${myIp[2]}.$addrIntBelow")}
                        if((addrIntAbove > 254) and (addrIntBelow < 0)) break
                    }
                }
                currentScanCount ++
                Thread.sleep(2000)
            }
        }.start()
    }

    private fun verifyAddress(address: String): Boolean{
        val inetAddr = InetAddress.getByName(address)
        if(inetAddr.isReachable(10)){
            try{
                val connector = SocketClient.Connector(address)
                connector.sendLine("{\"request\":\"ping\"}")
                val response = connector.readLine()
                connector.close()
                val macAddress = JSONObject(response).getString("MAC")
                val ipList = jsonObj.optJSONArray(macAddress) ?: JSONArray()
                val index = ipList.index(address)
                if(index == 0) return true
                if(index != -1) ipList.remove(index)
                ipList.insert(0, address)
                jsonObj.put(macAddress, ipList)
                update()
                return true
            }catch(ex: Exception){}
        }
        return false
    }

    companion object{
        const val ARP_TABLE_FILE = "ARPTable.json"
    }
}

private fun JSONArray.index(obj: String?):Int{
    for(position in 0 until length()){
        if(getString(position).equals(obj, true)) return position
    }
    return -1
}

private operator fun JSONArray.contains(obj: String?): Boolean {
    if(index(obj) == -1)
        return false
    return true
}

interface OnIpListener{
    val uiThread:Boolean
    fun onIpResult(address: String?)
}

class ARPItem(var macAddress: String, var ipAddress:String)