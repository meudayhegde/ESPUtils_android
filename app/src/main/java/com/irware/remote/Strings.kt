package com.irware.remote

object Strings {
    const val btnPropBtnId = "buttonID"
    const val btnPropBtnPosition = "btnPosition"
    const val btnPropColor = "color"
    const val btnPropIcon = "icon"
    const val btnPropIconType = "iconType"
    const val btnPropIrcode = "irCode"
    const val btnPropLength = "length"
    const val btnPropProtocol = "protocol"
    const val btnPropText = "text"
    const val btnPropTextColor = "textColor"

    const val devPropDescription = "description"
    const val devPropMacAddress = "macAddress"
    const val devPropNickname = "nickName"
    const val devPropPassword = "password"
    const val devPropUsername = "userName"

    const val espCommandPing = "{\"request\": \"ping\"}"

    fun espCommandAuth(username: String, password: String): String{
        return "{\"request\": \"authenticate\", \"username\": \"$username\", \"password\": \"$password\"}"
    }

    fun espCommandChangeUser(username: String, password: String, usernameNew: String, passwordNew: String): String{
        return "{\"request\": \"set_user\", \"username\": \"$username\", \"password\": \"$password\", \"new_username\": \"$usernameNew\", \"new_password\": \"$passwordNew\"}"
    }

    fun espCommandChangeWireless(username: String, password: String, wirelessMode: String, wirelessSSIDNew: String, wirelessPSKNew: String): String{
        return "{\"request\": \"set_wireless\", \"username\": \"$username\", \"password\": \"$password\", \"wireless_mode\": \"$wirelessMode\", \"new_ssid\": \"$wirelessSSIDNew\", \"new_pass\": \"$wirelessPSKNew\"}"
    }

    fun espCommandGetGpio(username: String, password: String): String{
        return "{\"request\": \"gpio_get\", \"username\": \"$username\", \"password\": \"$password\", \"pinNumber\": -1}"
    }

    fun espCommandGetWireless(username: String, password: String): String{
        return "{\"request\": \"get_wireless\", \"username\": \"$username\", \"password\": \"$password\"}"
    }

    fun espCommandReadIrcode(username: String, password: String, captureMode: Int): String{
        return "{\"request\": \"ir_capture\", \"username\": \"$username\", \"password\": \"$password\", \"capture_mode\": $captureMode}"
    }

    fun espCommandRestart(username: String, password: String): String{
        return "{\"request\": \"restart\", \"username\": \"$username\", \"password\": \"$password\"}"
    }

    fun espCommandSendIrcode(username: String, password: String, length: String, protocol: String, irCode: String): String{
        return "{\"request\": \"ir_send\", \"username\": \"$username\", \"password\": \"$password\", \"length\": \"$length\", \"protocol\": \"$protocol\", \"irCode\": \"$irCode\"}"
    }

    fun espCommandSetGpio(username: String, password: String, pinNumber: Int, pinValue: Int): String{
        return "{\"request\": \"gpio_set\", \"username\": \"$username\", \"password\": \"$password\", \"pinMode\": \"OUTPUT\", \"pinNumber\": $pinNumber, \"pinValue\": $pinValue}"
    }

    const val espResponse = "response"
    const val espResponseMac = "MAC"
    const val espResponsePinNumber = "pinNumber"
    const val espResponsePinValue = "pinValue"
    const val espResponseProgress = "progress"
    const val espResponseSuccess = "success"
    const val espResponseTimeout = "timeout"
    const val espResponseValue = "value"

    const val nameDirDeviceConfig = "devices"
    const val nameDirRemoteConfig = "remotes"
    const val nameFileARPTable = "ARPTable.json"
    const val nameFileDummyJson = "Dummy.json"
    const val nameFileGPIOConfig = "GPIOConfig.json"
}                                  