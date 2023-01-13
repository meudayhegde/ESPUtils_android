package com.github.meudayhegde.esputils.net

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.github.meudayhegde.ThreadHandler
import com.github.meudayhegde.Utils
import com.github.meudayhegde.esputils.ESPUtilsApp
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.holders.ARPItem
import com.github.meudayhegde.esputils.holders.DeviceProperties
import org.json.JSONArray
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * Used for scanning ESP Devices and operations involving MAC address and IP address
 * use [ESPTable.getInstance] method to obtain the [ESPTable] object.
 * necessary to pass [Context] object to [ESPTable.getInstance] method for the first time
 */
class ESPTable private constructor(context: Context) {
    private val sharedPref = context.getSharedPreferences(Strings.sharedPrefNameARPCache, Context.MODE_PRIVATE)
    private val prefEditor = sharedPref.edit()
    private var jsonObj: JSONObject = getJSONObject()

    init{
        instance = this
        update()
    }

    /**
     * invokes the listener when an esp device is detected
     * @param onArpItemListener
     * listener that takes [ARPItem] object as argument
     */
    fun scanForARPItems(onArpItemListener: ((arpItem: ARPItem) -> Unit)){
        startScan(onArpItemListener = onArpItemListener, timeout = SCAN_TIMEOUT * 5)
    }

    /**
     *
     * @param macToPropMap: { macAddress: deviceProperties} map from macAddress to corresponding
     * deviceProperties object.
     */
    fun refreshDevicesStatus(macToPropMap: MutableMap<String, DeviceProperties>){
        startScan(macToPropMap, timeout = SCAN_TIMEOUT)
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
                            ex.printStackTrace()
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
        return JSONObject(sharedPref.getString(Strings.sharedPrefNameARPCache, "{}")?: "{}")
    }

    private fun update(arpItem: ARPItem? = null){
        arpItem?.let {
            val ipList = jsonObj.optJSONArray(it.macAddress) ?: JSONArray()
            val index = ipList.index(it.ipAddress)
            if(index != 0) {
                if (index != -1) ipList.remove(index)
                ipList.insert(0, it.ipAddress)
                jsonObj.put(it.macAddress, ipList)
                update()
            }
        }
        prefEditor.putString(Strings.sharedPrefNameARPCache, jsonObj.toString())
        prefEditor.apply()
    }

    private fun startScan(macToPropMap: MutableMap<String, DeviceProperties>? = null,
                  onArpItemListener: ((arpItem: ARPItem) -> Unit)? = null,
                  timeout: Int = SCAN_TIMEOUT){
        ThreadHandler.runOnThread(ThreadHandler.ESP_MESSAGE) {
            Handler(Looper.getMainLooper()).post {
                macToPropMap?.values?.forEach { it.onDeviceStatusListener?.onBeginRefresh() }
            }

            Utils.getBroadcastAddresses().forEach { broadcastAddress ->
                sendBroadcastMessage(broadcastAddress, Strings.espCommandPing)
            }

            val socket = DatagramSocket(ESPUtilsApp.UDP_PORT_APP)
            socket.soTimeout = timeout

            val recvBuf = ByteArray(1024)
            val packet = DatagramPacket(recvBuf, recvBuf.size)

            while (true) {
                Log.d(javaClass.simpleName, "Ready to receive broadcast packets!")
                try{
                    socket.receive(packet)
                }catch (_: SocketTimeoutException){
                    Log.d(javaClass.simpleName, "Socket timeout, stopping UDP listener")
                    socket.close()
                    break
                }

                val ipAddress = packet.address.hostAddress
                Log.d(javaClass.simpleName, "Packet received from: $ipAddress")
                val macAddress = JSONObject(String(packet.data).trim { it <= ' ' }).optString(Strings.espResponseMac)

                ipAddress?.let{
                    onArpItemListener?.invoke(ARPItem(macAddress, it))
                    update(ARPItem(macAddress, it))
                }

                val devProp = macToPropMap?.remove(macAddress)
                Handler(Looper.getMainLooper()).post{
                    devProp?.isConnected = (ipAddress != null)
                }
                Thread.sleep(10)
                if(macToPropMap?.isEmpty() == true) {
                    socket.close()
                    break
                }
            }

            Handler(Looper.getMainLooper()).post {
                macToPropMap?.values?.forEach { it.getIpAddress {  } }
                macToPropMap?.clear()
            }
        }
    }

    private fun sendBroadcastMessage(broadcastAddress: InetAddress, message: String){
        Log.d(javaClass.simpleName, "Broadcast address: ${broadcastAddress.hostName}")
        val socket = DatagramSocket()
        socket.broadcast = true
        val sendData: ByteArray = message.encodeToByteArray()
        val sendPacket = DatagramPacket(sendData, sendData.size, broadcastAddress, ESPUtilsApp.UDP_PORT_ESP)
        socket.send(sendPacket)
    }

    companion object{
        const val MAX_RETRY = 3
        const val SCAN_TIMEOUT = 2000
        private var instance: ESPTable? = null

        /**
         * @param context cannot be null for the first function call.
         * @return instance of [ESPTable]
         */
        fun getInstance(context: Context? = null): ESPTable{
            return instance?: ESPTable(context?: throw NullPointerException("${(ESPTable::class.java).simpleName} instance not available"))
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

    private fun JSONArray.insert(position: Int, value: Any){
        if(length() > 0) for (i in length() downTo position + 1) {
            put(i, get(i - 1))
        }
        put(position, value)
    }
}