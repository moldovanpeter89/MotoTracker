package com.pm.mototracker.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.DataOutputStream


class TrackingManager(private val context: Context) {
    companion object {
        const val INTERNET_AVAILABLE = 1
        const val INTERNET_NOT_AVAILABLE = 0
    }

    fun switchMobileDataAndLocation(switchOn: Boolean, listener: (Int) -> Unit) {
        switchMobileData(switchOn)
        switchLocation(switchOn)
        GlobalScope.launch(Dispatchers.IO) {
            delay(3000L)
            listener.invoke(if (checkInternet()) INTERNET_AVAILABLE else INTERNET_NOT_AVAILABLE)
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
                if (nc!!.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) hasInternet =
                    true
            }
        }
        return hasInternet
    }
}