package com.pm.mototracker.manager

class CommandParser {

    var commandDataListener: ((Boolean) -> Unit)? = null
    var commandSmsTrackingListener: ((Boolean) -> Unit)? = null
    var commandCurrentStatusListener: (() -> Unit)? = null

    fun parseCommand(command: String) {
        if (command.startsWith("CMD")) {
            parseCmd(command)

        } else if (command.startsWith("ACK")) {
            parseAck(command)
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

    private fun parseAck(command: String) {

    }
}