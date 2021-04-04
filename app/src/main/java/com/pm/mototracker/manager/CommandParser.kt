package com.pm.mototracker.manager

import android.util.Log
import com.pm.mototracker.model.TrackingStatus

class CommandParser {

    var commandDataListener: ((Boolean) -> Unit)? = null
    var commandSmsTrackingListener: ((Boolean) -> Unit)? = null
    var commandCurrentStatusListener: (() -> Unit)? = null

    var ackDataOnListener: ((TrackingStatus) -> Unit)? = null
    var ackDataOffListener: ((TrackingStatus) -> Unit)? = null
    var ackCurrentStatusListener: ((TrackingStatus) -> Unit)? = null

    fun parseCommand(command: String) {
        try {
            if (command.startsWith("CMD")) {
                parseCmd(command)

            } else if (command.startsWith("ACK")) {
                parseAck(command)
            }
        } catch (e: Exception) {
            Log.e("TAG", "Parsing failed:", e)
        }
    }

    private fun parseCmd(command: String) {
        when (command) {
            CommandSender.CMD_DATA_OFF -> commandDataListener?.invoke(false)
            CommandSender.CMD_DATA_ON -> commandDataListener?.invoke(true)
            CommandSender.CMD_SMS_TRACKING_OFF -> commandSmsTrackingListener?.invoke(false)
            CommandSender.CMD_SMS_TRACKING_ON -> commandSmsTrackingListener?.invoke(true)
            CommandSender.CMD_GET_CURRENT_STATUS -> commandCurrentStatusListener?.invoke()
        }
    }

    private fun parseAck(ack: String) {
        when {
            ack.startsWith(CommandSender.ACK_DATA_ON) ->
                parseDataOnAck(ack)
            ack.startsWith(CommandSender.ACK_DATA_OFF) ->
                parseDataOffAck(ack)
            ack.startsWith(CommandSender.CMD_SMS_TRACKING_ON) ->
                parseSmsTrackingOnAck(ack)
            ack.startsWith(CommandSender.CMD_SMS_TRACKING_OFF) ->
                parseSmsTrackingOffAck(ack)
            ack.startsWith(CommandSender.ACK_GET_CURRENT_STATUS) ->
                parseCurrentStatusAck(ack)
        }
    }

    private fun parseDataOnAck(ack: String) {
        ackDataOnListener?.invoke(TrackingStatus(internetAvailability = ack.toTrackingValues()[2].toInt()))
    }

    private fun parseDataOffAck(ack: String) {
        ackDataOnListener?.invoke(TrackingStatus(internetAvailability = ack.toTrackingValues()[2].toInt()))
    }

    private fun parseSmsTrackingOnAck(ack: String) {
        Log.d("TAG", "###parseSmsTrackingOnAck: ${ack.toTrackingValues()}")
    }

    private fun parseSmsTrackingOffAck(ack: String) {
        Log.d("TAG", "###parseSmsTrackingOffAck: ${ack.toTrackingValues()}")
    }

    private fun parseCurrentStatusAck(ack: String) {
        val internetAvailable = ack.toTrackingValues()[2].toInt()
        val plugged = ack.toTrackingValues()[3].toInt()
        val charging = ack.toTrackingValues()[4].toInt()
        val batteryLevel = ack.toTrackingValues()[5].toInt()
        val location = ack.toTrackingValues()[6].toLocationValues()
        val latitude = location[0].toDoubleOrNull()
        val longitude = location[1].toDoubleOrNull()

        ackCurrentStatusListener?.invoke(
            TrackingStatus(
                internetAvailable,
                plugged,
                charging,
                batteryLevel,
                latitude,
                longitude
            )
        )
    }
}

fun String.toTrackingValues(): List<String> {
    return this.split("#")
}

fun String.toLocationValues(): List<String> {
    return this.split("@")
}