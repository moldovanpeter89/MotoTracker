package com.pm.mototracker

import android.content.Context
import android.util.TypedValue

const val TRACKING_VALUE_POSITIV = 1
const val TRACKING_VALUE_NEGATIV = 0

fun Boolean.toTrackingValueInt(): Int {
    return if (this) TRACKING_VALUE_POSITIV else TRACKING_VALUE_NEGATIV
}

fun Int.toTrackingValueBoolean(): Boolean {
    return this == TRACKING_VALUE_POSITIV
}

fun Int.powerSupplyTrackingValueToText(context: Context): String {
    return context.getString(
        R.string.tracking_status_ps,
        if (this == 1)
            context.getString(R.string.connected)
        else
            context.getString(R.string.disconnected)
    )
}

fun Int.batteryLevelTrackingValueToText(context: Context, batteryLevel: Int?): String {
    return context.getString(
        R.string.tracking_status_battery,
        batteryLevel,
        if (this == 1)
            context.getString(R.string.charging)
        else
            context.getString(R.string.not_charging)
    )
}

fun Int.dataTrackingValueToText(context: Context): String {
    return context.getString(
        R.string.tracking_status_data,
        if (this == 1)
            context.getString(R.string.on)
        else
            context.getString(R.string.off)
    )
}

fun Int.smsTrackingValueToText(context: Context): String {
    return context.getString(
        R.string.tracking_status_sms_tracking,
        if (this == 1)
            context.getString(R.string.on)
        else
            context.getString(R.string.off)
    )
}

fun Context.getDimension(value: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, this.resources.displayMetrics
    ).toInt()
}