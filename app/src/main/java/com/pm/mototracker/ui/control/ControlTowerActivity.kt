package com.pm.mototracker.ui.control

import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.util.TypedValue
import android.view.View
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.pm.mototracker.R
import com.pm.mototracker.common.BaseActivity
import com.pm.mototracker.databinding.ActivityControlTowerBinding
import com.pm.mototracker.manager.CommandParser
import com.pm.mototracker.manager.CommandSender
import com.pm.mototracker.ui.tracker.TrackingService
import com.pm.mototracker.util.PreferenceHelper
import com.pm.mototracker.util.PreferenceHelper.defaultPrefs
import com.pm.mototracker.util.PreferenceHelper.get
import kotlin.reflect.KClass


class ControlTowerActivity : BaseActivity<ActivityControlTowerBinding, ControlTowerViewModel>() {

    companion object {
        private const val IC_EXPANDED_ROTATION_DEG = 0F
        private const val IC_COLLAPSED_ROTATION_DEG = 180F
    }

    private lateinit var gMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var prefHelper: SharedPreferences
    private var trackingPhoneNumber: String? = null
    private val commandParser = CommandParser()

    private val ackReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var messageBody = ""
            for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                messageBody = smsMessage.messageBody
            }
            handleMessage(messageBody)
        }
    }

    override fun viewModelClass(): KClass<ControlTowerViewModel> = ControlTowerViewModel::class

    override fun provideBinding(): ActivityControlTowerBinding =
        ActivityControlTowerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.map.onCreate(savedInstanceState)
        init()
        initView()
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        binding.map.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.map.onDestroy()
        unregisterReceiver(ackReceiver)
        super.onDestroy()
    }

    private fun init() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        prefHelper = defaultPrefs(this)
        trackingPhoneNumber = prefHelper[PreferenceHelper.TRACKING_PHONE_NUMBER, ""]
        startCommandReceiver()
    }

    private fun startCommandReceiver() {
        IntentFilter().apply {
            addAction(TrackingService.ACTION_SMS)
            priority = TrackingService.SMS_ACTION_PRIORITY
        }.also {
            registerReceiver(ackReceiver, it);
        }
    }

    private fun initView() {
        initMap()
        initTrackingOptionsDialog()
        setTrackingClickListeners()
    }

    private fun initMap() {
        binding.map.getMapAsync {
            gMap = it
            displayCurrentLocation()
        }
    }

    private fun initTrackingOptionsDialog() {
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.trackingOptionsDialog.container)
        binding.trackingOptionsDialog.container.setBackgroundResource(R.drawable.bg_top_radius)
        binding.trackingOptionsDialog.dataSwitch.isChecked = false
        binding.trackingOptionsDialog.smsSwitch.isChecked = false

        bottomSheetBehavior.peekHeight = getDimension(54f)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        binding.trackingOptionsDialog.expandedImage.rotation =
            IC_EXPANDED_ROTATION_DEG

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState != BottomSheetBehavior.STATE_EXPANDED) {
                    binding.trackingOptionsDialog.expandedImage.rotation =
                        IC_EXPANDED_ROTATION_DEG

                } else {
                    binding.trackingOptionsDialog.expandedImage.rotation =
                        IC_COLLAPSED_ROTATION_DEG
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        binding.trackingOptionsDialog.container.setOnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun displayCurrentLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                with(gMap) {
                    if (location != null) {
                        val currentPosition = LatLng(location.latitude, location.longitude)
                        addMarker(MarkerOptions().position(currentPosition).title("I am here!"))
                        moveCamera(CameraUpdateFactory.newLatLng(currentPosition))
                        animateCamera(CameraUpdateFactory.zoomTo(10f))
                    }
                }
            }
    }

    private fun setTrackingClickListeners() {
        binding.trackingOptionsDialog.dataSwitch.setOnCheckedChangeListener { _, isChecked ->
            CommandSender.sendCommand(
                phoneNumber = trackingPhoneNumber,
                command = if (isChecked) CommandSender.CMD_DATA_ON else CommandSender.CMD_DATA_OFF
            )
        }
        binding.trackingOptionsDialog.smsSwitch.setOnCheckedChangeListener { _, isChecked ->
            CommandSender.sendCommand(
                phoneNumber = trackingPhoneNumber,
                command = if (isChecked) CommandSender.CMD_SMS_TRACKING_ON else CommandSender.CMD_SMS_TRACKING_OFF
            )
        }
        binding.trackingOptionsDialog.currentStatus.setOnClickListener {
            CommandSender.sendCommand(
                phoneNumber = trackingPhoneNumber,
                command = CommandSender.CMD_GET_CURRENT_STATUS
            )
        }
    }

    private fun handleMessage(messageBody: String) {
        commandParser.ackDataOnListener = { internetAvailability ->

        }
        commandParser.ackDataOffListener = { internetAvailability ->

        }
        commandParser.ackCurrentStatusListener =
            { internetAvailability, plugged, charging, batteryLevel, latitude, longitude ->
                Log.d(
                    "TAG",
                    "##lis: $internetAvailability, $plugged, $charging, $batteryLevel, $latitude,  $longitude"
                )
            }

        commandParser.parseCommand(messageBody)
    }
}

fun Context.getDimension(value: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, this.resources.displayMetrics
    ).toInt()
}