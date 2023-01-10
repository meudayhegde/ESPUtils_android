package com.irware.remote.holders

/**
 * @param macAddress([String]) MAC Address of the network device
 * @param ipAddress([String]) IP Address of the network device
 * @param devNickName([String]) Device NickName stored on the app
 */
class ARPItem(var macAddress: String, var ipAddress: String, var devNickName: String? = null)