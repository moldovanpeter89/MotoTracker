package com.pm.mototracker.manager

import android.util.Log
import com.pm.mototracker.model.TrackingStatus
import com.pm.mototracker.toLocationValues
import com.pm.mototracker.toTrackingValues

class CommandParser {

    companion object {
        const val TRACKING_DELIMITER = "#"
        const val LOCATION_DELIMITER = "@"
        private const val CMD = "CMD"
        private const val ACK = "ACK"
        private const val PS_STATUS = "PS_STATUS"
    }

    var commandDataListener: ((Boolean) -> Unit)? = null
    var commandSmsTrackingListener: ((Boolean) -> Unit)? = null
    var commandCurrentStatusListener: (() -> Unit)? = null

    var ackDataOnListener: ((TrackingStatus) -> Unit)? = null
    var ackDataOffListener: ((TrackingStatus) -> Unit)? = null
    var ackSmsTrackingOnListener: (() -> Unit)? = null
    var ackSmsTrackingOffListener: (() -> Unit)? = null
    var ackSmsTrackingReportListener: ((TrackingStatus) -> Unit)? = null
    var ackCurrentStatusListener: ((TrackingStatus) -> Unit)? = null
    var psStatusListener: ((TrackingStatus) -> Unit)? = null

    fun parseCommand(command: String) {
        try {
            when {
                command.startsWith(CMD) -> {
                    parseCmd(command)

                }
                command.startsWith(ACK) -> {
                    parseAck(command)

                }
                command.startsWith(PS_STATUS) -> {
                    parsePsStatus(command)
                }
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
            ack.startsWith(CommandSender.ACK_SMS_TRACKING_ON) ->
                parseSmsTrackingOnAck()
            ack.startsWith(CommandSender.ACK_SMS_TRACKING_OFF) ->
                parseSmsTrackingOffAck()
            ack.startsWith(CommandSender.ACK_SMS_TRACKING_REPORT) ->
                parseSmsTrackingReport(ack)
            ack.startsWith(CommandSender.ACK_GET_CURRENT_STATUS) ->
                parseCurrentStatusAck(ack)
        }
    }

    private fun parseDataOnAck(ack: String) {
        ackDataOnListener?.invoke(TrackingStatus(internetAvailability = ack.toTrackingValues()[2].toInt()))
    }

    private fun parseDataOffAck(ack: String) {
        ackDataOffListener?.invoke(TrackingStatus(internetAvailability = ack.toTrackingValues()[2].toInt()))
    }

    private fun parseSmsTrackingOnAck() {
        ackSmsTrackingOnListener?.invoke()
    }

    private fun parseSmsTrackingOffAck() {
        ackSmsTrackingOffListener?.invoke()
    }

    private fun parseSmsTrackingReport(ack: String) {
        parseTrackingStatus(ack) {
            ackSmsTrackingReportListener?.invoke(it)
        }
    }

    private fun parseCurrentStatusAck(ack: String) {
        parseTrackingStatus(ack) {
            ackCurrentStatusListener?.invoke(it)
        }
    }

    private fun parsePsStatus(command: String) {
        psStatusListener?.invoke(TrackingStatus(plugged = command.toTrackingValues()[1].toInt()))
    }

    private fun parseTrackingStatus(ack: String, listener: (TrackingStatus) -> Unit) {
        val internetAvailable = ack.toTrackingValues()[2].toInt()
        val plugged = ack.toTrackingValues()[3].toInt()
        val charging = ack.toTrackingValues()[4].toInt()
        val batteryLevel = ack.toTrackingValues()[5].toInt()
        val location = ack.toTrackingValues()[6].toLocationValues()
        val latitude = location[0].toDoubleOrNull()
        val longitude = location[1].toDoubleOrNull()

        listener.invoke(
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