package com.github.meudayhegde

import android.util.Log
import java.io.*
import java.math.BigInteger
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class Utils{
    companion object{

        /**
         * @return [ArrayList]<[InetAddress]> broadcast addresses
         * of all the local network that the android device is connected to.
         */
        fun getBroadcastAddresses(): ArrayList<InetAddress> {
            val broadcastAddresses = ArrayList<InetAddress>()
            System.setProperty("java.net.preferIPv4Stack", "true")
            try {
                NetworkInterface.getNetworkInterfaces().toList()
                    .filter { !it.isLoopback }
                    .forEach { ni ->
                        ni.interfaceAddresses.forEach { ia ->
                            ia?.broadcast?.toString()?.let { broadcastAddresses.add(InetAddress.getByName(it.substring(1))) }
                        }
                    }
            } catch (e: SocketException) {
                e.printStackTrace()
            }
            return broadcastAddresses
        }

        /**
         * @param file instance of [File] to which MD5 need to be calculated
         * @return [String] containing the MD% hash of the requested file
         */
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

        /**
         * @param length Long value as in Bytes count
         * @return human readable length in [String]
         * @sample getConventionalSize(1024) returns "1 KB"
         */
        fun getConventionalSize(length: Long): String {
            var size = length
            var count = 0
            while (size >= 1000) {
                size /= 1024
                count++
            }
            val unit = when (count) { 0 -> "B" 1 -> "KB" 2 -> "MB"  else -> "B" }
            return ((size * 100.0).roundToInt() / 100.0).toString() + " " + unit
        }
    }
}