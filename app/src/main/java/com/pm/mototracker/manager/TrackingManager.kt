package com.pm.mototracker.manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.pm.mototracker.toTrackingValueInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.DataOutputStream


class TrackingManager(private val context: Context) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    fun init() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun switchMobileDataAndLocation(switchOn: Boolean, listener: (Int) -> Unit) {
        switchMobileData(switchOn)
        switchLocation(switchOn)
        GlobalScope.launch(Dispatchers.IO) {
            delay(3000L)
            listener.invoke((checkInternet().toTrackingValueInt()))
        }
    }

    private fun switchMobileData(switchOn: Boolean) {
        val dataSwitch: String = if (switchOn) {
            "enable"
        } else {
            "disable"
        }
        val suProcess = Runtime.getRuntime().exec("su")
        val outputStream = DataOutputStream(suProcess.outputStream)
        outputStream.writeBytes("svc data $dataSwitch\n")
        outputStream.writeBytes("exit\n")
        outputStream.flush()
    }

    private fun switchLocation(turnOn: Boolean) {
        val gpsSwitch: String = if (turnOn) {
            "+"
        } else {
            "-"
        }
        val commandsArray =
            arrayOf("settings put secure location_providers_allowed " + gpsSwitch + "gps")
        val suProcess = Runtime.getRuntime().exec("su")
        val outputStream = DataOutputStream(suProcess.outputStream)
        for (command in commandsArray) {
            outputStream.writeBytes(
                """
                $command
                
                """.trimIndent()
            )
        }
        outputStream.writeBytes("exit\n")
        outputStream.flush()
    }

    private fun checkInternet(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }

        val networks = connectivityManager.allNetworks
        var hasInternet = false

        if (networks.isNotEmpty()) {
            for (network in networks) {
                val nc = connectivityManager.getNetworkCapabilities(network)
                if (nc!!.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                    hasInternet = true
            }
        }

        return hasInternet
    }

    @SuppressLint("MissingPermission")
    fun currentStatus(listener: (Int, Int, Int, Int?, Double?, Double?) -> Unit) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }

        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val isPlugged: Boolean =
            plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean =
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                listener.invoke(
                    checkInternet().toTrackingValueInt(),
                    isPlugged.toTrackingValueInt(),
                    isCharging.toTrackingValueInt(),
                    batteryPct?.toInt(),
                    location?.latitude,
                    location?.longitude
                )
            }
    }
}