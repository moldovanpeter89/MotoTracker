package com.pm.mototracker.manager

import android.telephony.SmsManager

object CommandSender {

    const val CMD_DATA_OFF = "CMD#100"
    const val CMD_DATA_ON = "CMD#101"
    const val CMD_SMS_TRACKING_OFF = "CMD#200"
    const val CMD_SMS_TRACKING_ON = "CMD#201"
    const val CMD_SMS_TRACKING_REPORT = "CMD#203"
    const val CMD_GET_CURRENT_STATUS = "CMD#300"
    const val PS_STATUS = "PS_STATUS#"

    const val ACK_DATA_OFF = "ACK#100#"
    const val ACK_DATA_ON = "ACK#101#"
    const val ACK_SMS_TRACKING_OFF = "ACK#200#"
    const val ACK_SMS_TRACKING_ON = "ACK#201#"
    const val ACK_SMS_TRACKING_REPORT = "ACK#203#"
    const val ACK_GET_CURRENT_STATUS = "ACK#300#"

    private val smsManager: SmsManager = SmsManager.getDefault()

    fun sendCommand(phoneNumber: String? = "", command: String = "") {
        val parts: ArrayList<String> = smsManager.divideMessage(command)
        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
    }
}