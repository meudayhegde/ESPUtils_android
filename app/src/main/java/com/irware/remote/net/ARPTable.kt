package com.irware.remote.net

import android.text.TextUtils
import android.util.Log
import com.irware.ThreadHandler
import com.irware.Utils
import com.irware.remote.Strings
import com.irware.remote.ESPUtilsApp
import com.irware.remote.R
import com.irware.remote.holders.ARPItem
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress

class ARPTable(private val scanCount: Int = 1, private val arpTableFile: File = ESPUtilsApp.getPrivateFile(R.string.name_file_arp_table)) {
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

    fun getARPItemList(onArpItemListener: ((arpItem: ARPItem) -> Unit)): ThreadHandler.InfiniteThread{
        return ThreadHandler.getThreadByPosition(
            ThreadHandler.runOnFreeThread{
                val ipList = ArrayList<String>()
                val reachableIPList = ArrayList<String>()
                jsonObj.keys().forEach {
                    val ipArray = jsonObj.getJSONArray(it)
                    for(index in 0 until ipArray.length()){
                        ipList.add(ipArray.getString(index))
                    }
                }
                ipList.forEach { address ->
                    for(i in 0 until 3){
                        val inetAddress = InetAddress.getByName(address)
                        if((address !in reachableIPList) and inetAddress.isReachable(10)){
                            try{
                                reachableIPList.add(address)
                                val macAddress = getMacForIP(address)
                                onArpItemListener.invoke(ARPItem(macAddress, address))
                                break
                            }catch(_: Exception){}
                        }
                    }
                }
                for(myIp in Utils.getIPAddress()){
                    val myIpArr = myIp.split(".")
                    val myIpInt = myIpArr[3].toInt()
                    for(i in 1 until 255){
                        arrayOf(i, -i).forEach { ind ->
                            val addressInt = myIpInt + ind
                            if(addressInt in 0 until 255){
                                val address = "${myIpArr[0]}.${myIpArr[1]}.${myIpArr[2]}.$addressInt"
                                val inetAddress = InetAddress.getByName(address)
                                if((address !in reachableIPList) and inetAddress.isReachable(10)){
                                    try{
                                        reachableIPList.add(address)
                                        val macAddress = getMacForIP(address)
                                        onArpItemListener.invoke(ARPItem(macAddress, address))
                                    }catch(_: Exception){}
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    fun getIpFromMac(mac: String, listener: ((address: String?) -> Unit)? = null): String?{
        if(listener != null) ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE){
            val addresses = jsonObj.optJSONArray(mac) ?: JSONArray()
            for(j in 0 until MAX_RETRY)
                for(i in 0 until addresses.length()){
                    val address = addresses.getString(i)
                    if(InetAddress.getByName(address).isReachable(50)){
                        try{
                            val connector = SocketClient.Connector(address)
                            connector.sendLine(Strings.espCommandPing)
                            if(JSONObject(connector.readLine()).getString(Strings.espResponseMac) == mac) {
                                if(i != 0){
                                    addresses.remove(i)
                                    addresses.insert(0, address)
                                    update()
                                }
                                listener.invoke(address)
                                return@runOnThread
                            }
                        }catch(ex: Exception){
                            Log.d(javaClass.name, ex.toString() + ex.message)
                        }
                    }
                }
            listener.invoke(null)
        }

        val addresses = jsonObj.optJSONArray(mac) ?: JSONArray()
        for(i in 0 until addresses.length()){
            return addresses.getString(i)
        }
        return null
    }

    private fun getJSONObject(): JSONObject{
        if(!arpTableFile.exists()) {
            arpTableFile.createNewFile()
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
        Log.d(javaClass.simpleName, jsonObj.toString(4))
        osr.write(jsonObj.toString(4))
        osr.flush()
        osr.close()
    }

    private fun startScan(){
        ThreadHandler.runOnFreeThread {
            var currentScanCount = 0
            while((scanCount == -1) or (currentScanCount < scanCount)){
                for(myIp in Utils.getIPAddress()){
                    val intArr = myIp.split(".")
                    val myIpInt = intArr[3].toInt()
                    for(i in 1 until 255){
                        val ipIntAbove = myIpInt + i
                        val ipIntBelow = myIpInt - i
                        if(ipIntAbove < 255){verifyAddress("${intArr[0]}.${intArr[1]}.${intArr[2]}.$ipIntAbove")}
                        if(ipIntBelow >= 0){verifyAddress("${intArr[0]}.${intArr[1]}.${intArr[2]}.$ipIntBelow")}
                        if((ipIntAbove > 254) and (ipIntBelow < 0)) break
                    }
                }
                currentScanCount ++
                Thread.sleep(200)
            }
        }
    }

    private fun verifyAddress(address: String): Boolean{
        val inetAddress = InetAddress.getByName(address)
        if(inetAddress.isReachable(10)){
            try{
                val macAddress = getMacForIP(address)
                val ipList = jsonObj.optJSONArray(macAddress) ?: JSONArray()
                val index = ipList.index(address)
                if(index == 0) return true
                if(index != -1) ipList.remove(index)
                ipList.insert(0, address)
                jsonObj.put(macAddress, ipList)
                update()
                return true
            }catch(_: Exception){}
        }
        return false
    }

    private fun getMacForIP(address: String): String{
        val connector = SocketClient.Connector(address)
        connector.sendLine(Strings.espCommandPing)
        val response = connector.readLine()
        connector.close()
        return JSONObject(response).optString(Strings.espResponseMac)
    }

    companion object{
        const val MAX_RETRY = 3
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

    private fun JSONArray.insert(position: Int, value: Any){
        if(length() > 0) for (i in length() downTo position + 1) {
            put(i, get(i - 1))
        }
        put(position, value)
    }
}