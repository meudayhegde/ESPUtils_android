package com.irware.remote.net

import android.util.Log
import com.irware.Utils
import com.irware.remote.holders.DeviceProperties
import com.irware.remote.listeners.OnOTAIntermediateListener
import java.io.File
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket

class EspOta(private val devProp: DeviceProperties, private val localPort: Int = OTA_PORT, private val remotePort: Int = OTA_PORT) {
    private fun sendInvitation(updateFile: File, command: Int = SYSTEM, updateIntermediateListener: OnOTAIntermediateListener? = null): Boolean{

        updateIntermediateListener?.onStatusUpdate("Authenticating...", true)

        val fileMD5 = Utils.md5(updateFile)
        val fileLength = updateFile.length()
        var message = "$command $localPort $fileLength ${fileMD5}\n"
        val sock = DatagramSocket()
        var pack = DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length, InetAddress.getByName(devProp.ipAddress), remotePort)
        sock.send(pack)

        sock.soTimeout = 10

        var dgram = DatagramPacket(ByteArray(128), 128)
        try {
            sock.receive(dgram)
        }catch (ex: IOException){
            updateIntermediateListener?.onError("No Answer from Device.\n${ex.message}")
            sock.close()
            return false
        }

        var content = dgram.data.toString(Charsets.UTF_8).filter { it.isLetterOrDigit() or it.isWhitespace()}
        if(content != "OK"){
            if(content.startsWith("AUTH")){
                val nonce = content.split(" ")[1]
                val cnonce = Utils.md5("${updateFile.absolutePath}$fileLength$fileMD5${devProp.ipAddress}")
                val passMD5 = Utils.md5(devProp.password)
                val result = Utils.md5("$passMD5:$nonce:$cnonce")

                message = "$AUTH $cnonce $result"
                pack = DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length, InetAddress.getByName(devProp.ipAddress), remotePort)
                sock.send(pack)

                sock.soTimeout = 10

                dgram = DatagramPacket(ByteArray(128), 128)
                try {
                    sock.receive(dgram)
                }catch (ex: IOException){
                    updateIntermediateListener?.onError("No response from the device.\n${ex.message}")
                    sock.close()
                    return false
                }

                content = dgram.data.toString(Charsets.UTF_8).filter { it.isLetterOrDigit() or it.isWhitespace() }

                if(content != "OK"){
                    updateIntermediateListener?.onError("$content, Check credentials.")
                    return false
                }

                Log.d(TAG, "Authentication: OK")

                sock.close()
                return true
            }else{
                updateIntermediateListener?.onError("$content, Check credentials.")
                sock.close()
                return false
            }
        }else{
            Log.d(TAG, "Authentication: OK")
            return true
        }
    }

    fun installUpdate(updateFile: File, command: Int = SYSTEM, updateIntermediateListener: OnOTAIntermediateListener? = null){

        val serverSocket = ServerSocket(localPort)
        var authenticated = false
        val authenticate = { authenticated = sendInvitation(updateFile, command, updateIntermediateListener) }
        for(i in 0..10){
            if(authenticated) break
            authenticate.invoke()
        }
        if(!authenticated) {
            serverSocket.close()
            return
        }
        updateIntermediateListener?.onStatusUpdate("Installing Update: ${updateFile.name}", true)
        serverSocket.use { server ->
            try{
                server.accept()
            }catch(ex: IOException){
                updateIntermediateListener?.onError("No response from Device\n${ex.message}")
                return
            }.use { socket ->
                serverSocket.soTimeout = 0
                socket.soTimeout = 0
                val sos = socket.getOutputStream()
                val sis = socket.getInputStream()

                updateIntermediateListener?.onProgressUpdate(0F)

                var offset = 0F
                val fileLength = updateFile.length()
                val bytes = ByteArray(1460)
                var receivedOK = false
                var receivedError = false
                updateFile.inputStream().buffered().use{
                    try{
                        while(true){
                            val len = it.read(bytes)
                            if(len <= 0) break
                            offset += len
                            updateIntermediateListener?.onProgressUpdate(offset / fileLength.toFloat())
                            sos.write(bytes, 0, len)

                            val recvd = ByteArray(32)
                            sis.read(recvd)
                            val response = recvd.toString(Charsets.UTF_8).filter { ch -> ch.isLetterOrDigit() }
                            receivedOK = response.indexOf('O') >= 0
                            receivedError = response.indexOf('E') >= 0
                        }
                    }catch(ex: Exception){
                        updateIntermediateListener?.onError("Error uploading update.\n${ex.message}")
                        return
                    }
                }
                while (!(receivedOK or receivedError)){
                    try{
                        val recvd = ByteArray(32)
                        sis.read(recvd)
                        val response = recvd.toString(Charsets.UTF_8).filter { ch -> ch.isLetterOrDigit() }
                        receivedOK = response.indexOf('O') >= 0
                        receivedError = response.indexOf('E') >= 0
                    }catch(ex: Exception){
                        updateIntermediateListener?.onError("Failed to get update status.\n${ex.message}")
                    }
                }
                if(receivedError) updateIntermediateListener?.onError("Update Failed")
                if(receivedOK) {
                    updateIntermediateListener?.onProgressUpdate(1F)
                    updateIntermediateListener?.onStatusUpdate("Update Successfully Installed.\n" +
                            "Please Wait for a while for the device to restart", false)
                }
            }
        }
    }

    companion object{
        const val SYSTEM = 0
        const val SPIFFS = 100
        const val AUTH = 200
        const val OTA_PORT = 48325

        val TAG: String = (EspOta::class.java).simpleName
    }
}