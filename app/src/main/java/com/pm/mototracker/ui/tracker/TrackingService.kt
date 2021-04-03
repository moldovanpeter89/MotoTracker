package com.pm.mototracker.ui.tracker

import android.app.*
import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.pm.mototracker.R
import com.pm.mototracker.manager.CommandParser
import com.pm.mototracker.manager.CommandSender
import com.pm.mototracker.manager.TrackingManager
import com.pm.mototracker.util.PreferenceHelper
import com.pm.mototracker.util.PreferenceHelper.defaultPrefs
import com.pm.mototracker.util.PreferenceHelper.get


class TrackingService : Service() {
    companion object {
        private const val CHANNEL_ID = "my_service"
        private const val CHANNEL_NAME = "My Background Service"

        private const val CONTENT_TITLE = "MotoTracker"
        private const val CONTENT_TEXT = "Tracking Service Running..."

        const val ACTION_SMS = "android.provider.Telephony.SMS_RECEIVED"
        private const val ACTION_POWER_CONNECTED = "android.intent.action.ACTION_POWER_CONNECTED"
        private const val ACTION_POWER_DISCONNECTED =
            "android.intent.action.ACTION_POWER_DISCONNECTED"

        private const val ONGOING_NOTIFICATION_ID = 1848 //-ban szulettem
        const val SMS_ACTION_PRIORITY = 1000
        private const val POWER_SUPPLY_CONNECTED = 1
        private const val POWER_SUPPLY_DISCONNECTED = 0
    }

    private val commandParser = CommandParser()
    private val trackingManager = TrackingManager(this)
    private lateinit var prefHelper: SharedPreferences
    private var controlTowerPhoneNumber: String? = null


    private val commandReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var messageBody = ""
            for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                messageBody = smsMessage.messageBody
            }
            handleMessage(messageBody)
        }
    }

    private val powerReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_POWER_CONNECTED -> {
                    CommandSender.sendCommand(
                        phoneNumber = controlTowerPhoneNumber,
                        command = CommandSender.PS_STATUS + POWER_SUPPLY_CONNECTED
                    )
                    //Sending readable message
                    CommandSender.sendCommand(
                        phoneNumber = controlTowerPhoneNumber,
                        command = getString(R.string.ps_connected)
                    )
                }
                ACTION_POWER_DISCONNECTED -> {
                    CommandSender.sendCommand(
                        phoneNumber = controlTowerPhoneNumber,
                        command = CommandSender.PS_STATUS + POWER_SUPPLY_DISCONNECTED
                    )
                    //Sending readable message
                    CommandSender.sendCommand(
                        phoneNumber = controlTowerPhoneNumber,
                        command = getString(R.string.ps_disconnected)
                    )
                }
            }
        }
    }

    override fun onCreate() {
        prefHelper = defaultPrefs(this)
        trackingManager.init()
        controlTowerPhoneNumber = prefHelper[PreferenceHelper.CONTROL_TOWER_PHONE_NUMBER, ""]
        startForeground()
        startCommandReceiver()
        startPowerReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(commandReceiver);
    }

    private fun startForeground() {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(CHANNEL_ID, CHANNEL_NAME)
            } else {
                ""
            }

        Intent(this, TrackerActivity::class.java)
            .also { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0,
                    notificationIntent, 0
                ).also { pendingIntent ->
                    NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle(CONTENT_TITLE)
                        .setContentText(CONTENT_TEXT)
                        .setPriority(PRIORITY_MIN)
                        .setContentIntent(pendingIntent).build()
                        .also { notification ->
                            startForeground(ONGOING_NOTIFICATION_ID, notification)
                        }
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun startCommandReceiver() {
        IntentFilter().apply {
            addAction(ACTION_SMS)
            priority = SMS_ACTION_PRIORITY
        }.also {
            registerReceiver(commandReceiver, it);
        }
    }

    private fun startPowerReceiver() {
        IntentFilter().apply {
            addAction(ACTION_POWER_CONNECTED)
            addAction(ACTION_POWER_DISCONNECTED)
            priority = SMS_ACTION_PRIORITY
        }.also {
            registerReceiver(powerReceiver, it);
        }
    }

    private fun handleMessage(messageBody: String) {
        commandParser.commandDataListener = { dataOn ->
            trackingManager.switchMobileDataAndLocation(dataOn) { internetStatus ->
                CommandSender.sendCommand(
                    phoneNumber = controlTowerPhoneNumber,
                    command = if (dataOn) CommandSender.ACK_DATA_ON + internetStatus else CommandSender.ACK_DATA_OFF + internetStatus
                )
            }
        }
        commandParser.commandSmsTrackingListener = { smsTrackOn ->

        }
        commandParser.commandCurrentStatusListener = {
            trackingManager.currentStatus { internetAvailability, plugged, charging, batteryLevel, latitude, longitude ->
                val command =
                    "${CommandSender.ACK_GET_CURRENT_STATUS}$internetAvailability#$plugged#$charging#$batteryLevel#$latitude@$longitude"
                CommandSender.sendCommand(
                    phoneNumber = controlTowerPhoneNumber,
                    command = command
                )
            }

        }
        commandParser.parseCommand(messageBody)
    }
}