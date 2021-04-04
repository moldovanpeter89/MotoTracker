package com.pm.mototracker.model

data class TrackingStatus(
    val internetAvailability: Int? = null,
    val plugged: Int? = null,
    val charging: Int? = null,
    val batteryLevel: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)