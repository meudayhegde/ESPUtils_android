package com.irware

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.math.BigInteger
import java.net.NetworkInterface
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.collections.ArrayList

class Utils{
    companion object{
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

        fun md5(file: File): String?{

            val tag = "MD5"
            val digest: MessageDigest = try {
                MessageDigest.getInstance("MD5")
            } catch (e: NoSuchAlgorithmException) {
                Log.e(tag, "Exception while getting digest", e)
                return null
            }

            val ins: InputStream
            try {
                ins = FileInputStream(file)
            } catch (e: FileNotFoundException) {
                Log.e(tag, "Exception while getting FileInputStream", e)
                return null
            }

            val buffer = ByteArray(8192)
            var read: Int
            return try {
                while (ins.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                }
                val md5sum: ByteArray = digest.digest()
                val bigInt = BigInteger(1, md5sum)
                var output: String = bigInt.toString(16)
                // Fill to 32 chars
                output = String.format("%32s", output).replace(' ', '0')
                output
            } catch (e: IOException) {
                throw RuntimeException("Unable to process file for MD5", e)
            } finally {
                try {
                    ins.close()
                } catch (e: IOException) {
                    Log.e(tag, "Exception on closing MD5 input stream", e)
                }
            }
        }

        fun md5(input: String): String{
            return MessageDigest.getInstance("MD5").digest(input.toByteArray()).toHex()
        }

        private fun ByteArray.toHex(): String {
            return joinToString("") { "%02x".format(it) }
        }

    }
}