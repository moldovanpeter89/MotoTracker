package com.pm.mototracker.ui.control

import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.provider.Telephony
import android.view.View
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.pm.mototracker.*
import com.pm.mototracker.common.BaseActivity
import com.pm.mototracker.databinding.ActivityControlTowerBinding
import com.pm.mototracker.manager.CommandParser
import com.pm.mototracker.manager.CommandSender
import com.pm.mototracker.model.TrackingStatus
import com.pm.mototracker.ui.tracker.TrackingService
import com.pm.mototracker.util.PreferenceHelper
import com.pm.mototracker.util.PreferenceHelper.defaultPrefs
import com.pm.mototracker.util.PreferenceHelper.get
import kotlin.reflect.KClass


class ControlTowerActivity : BaseActivity<ActivityControlTowerBinding, ControlTowerViewModel>() {

    companion object {
        private const val IC_EXPANDED_ROTATION_DEG = 0F
        private const val IC_COLLAPSED_ROTATION_DEG = 180F
        private const val MAP_ZOOM = 14.6F
    }

    private lateinit var gMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var prefHelper: SharedPreferences
    private var trackingPhoneNumber: String? = null
    private val commandParser = CommandParser()
    private var canSendCommand = true

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
        initTrackingDeviceStatus()
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
                        addMarker(
                            MarkerOptions()
                                .position(currentPosition)
                                .title(getString(R.string.you_are_here))
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        )
                        moveCamera(CameraUpdateFactory.newLatLng(currentPosition))
                        animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, MAP_ZOOM))
                    }
                }
            }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTrackingClickListeners() {
        binding.trackingOptionsDialog.dataSwitch.setOnTouchListener { v, _ ->
            canSendCommand = true
            false
        }
        binding.trackingOptionsDialog.smsSwitch.setOnTouchListener { _, _ ->
            canSendCommand = true
            false
        }
        binding.trackingOptionsDialog.dataSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (canSendCommand) {
                CommandSender.sendCommand(
                    phoneNumber = trackingPhoneNumber,
                    command = if (isChecked) CommandSender.CMD_DATA_ON else CommandSender.CMD_DATA_OFF
                )
                binding.trackingStatusProgressbar.visibility = View.VISIBLE
            }
        }
        binding.trackingOptionsDialog.smsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (canSendCommand) {
                CommandSender.sendCommand(
                    phoneNumber = trackingPhoneNumber,
                    command = if (isChecked) CommandSender.CMD_SMS_TRACKING_ON else CommandSender.CMD_SMS_TRACKING_OFF
                )
                binding.trackingStatusProgressbar.visibility = View.VISIBLE
            }
        }
        binding.trackingOptionsDialog.currentStatus.setOnClickListener {
            CommandSender.sendCommand(
                phoneNumber = trackingPhoneNumber,
                command = CommandSender.CMD_GET_CURRENT_STATUS
            )
            canSendCommand = false
            binding.trackingStatusProgressbar.visibility = View.VISIBLE
        }
    }

    private fun handleMessage(messageBody: String) {
        commandParser.ackDataOnListener = { trackingStatus ->
            canSendCommand = false
            setTrackingDeviceStatus(trackingStatus)
        }
        commandParser.ackDataOffListener = { trackingStatus ->
            canSendCommand = false
            setTrackingDeviceStatus(trackingStatus)
        }
        commandParser.ackSmsTrackingOnListener = {
            canSendCommand = false
            setSmsTrackingStatus(smsTrackOn = true)
        }
        commandParser.ackSmsTrackingOffListener = {
            canSendCommand = false
            setSmsTrackingStatus(smsTrackOn = false)
        }
        commandParser.ackSmsTrackingReportListener = { trackingStatus ->
            canSendCommand = false
            setSmsTrackingStatus(smsTrackOn = true)
            setTrackingDeviceStatus(trackingStatus)
        }
        commandParser.ackCurrentStatusListener = { trackingStatus ->
            canSendCommand = false
            setTrackingDeviceStatus(trackingStatus)
        }
        commandParser.psStatusListener = { trackingStatus ->
            setTrackingDeviceStatus(trackingStatus)
        }
        commandParser.parseCommand(messageBody)
    }

    private fun initTrackingDeviceStatus() {
        binding.trackingStatusPowerSupply.text =
            getString(R.string.tracking_status_ps, getString(R.string.na))
        binding.trackingStatusBattery.text = getString(R.string.tracking_status_battery_na)
        binding.trackingStatusData.text =
            getString(R.string.tracking_status_data, getString(R.string.na))
        binding.trackingStatusSmsTracking.text =
            getString(R.string.tracking_status_sms_tracking, getString(R.string.na))
    }

    private fun setSmsTrackingStatus(smsTrackOn: Boolean) {
        binding.trackingStatusProgressbar.visibility = View.GONE
        binding.trackingOptionsDialog.smsSwitch.isChecked = smsTrackOn
        binding.trackingStatusSmsTracking.text = smsTrackOn.smsTrackingValueToText(this)
    }

    private fun setTrackingDeviceStatus(trackingStatus: TrackingStatus) {
        binding.trackingStatusProgressbar.visibility = View.GONE

        addTrackerMarker(trackingStatus)

        binding.trackingOptionsDialog.dataSwitch.isChecked =
            trackingStatus.internetAvailability?.toTrackingValueBoolean() == true

        trackingStatus.plugged?.let {
            binding.trackingStatusPowerSupply.text =
                trackingStatus.plugged.powerSupplyTrackingValueToText(this)
        }
        trackingStatus.charging?.let {
            binding.trackingStatusBattery.text =
                trackingStatus.charging.batteryLevelTrackingValueToText(
                    this,
                    trackingStatus.batteryLevel
                )
        }
        trackingStatus.internetAvailability?.let {
            binding.trackingStatusData.text =
                trackingStatus.internetAvailability.dataTrackingValueToText(this)
        }

        canSendCommand = true
    }

    private fun addTrackerMarker(trackingStatus: TrackingStatus) {
        if (trackingStatus.latitude != null && trackingStatus.longitude != null) {
            val location = LatLng(trackingStatus.latitude, trackingStatus.longitude)
            with(gMap) {
                clear()
                val currentPosition = LatLng(location.latitude, location.longitude)
                addMarker(
                    MarkerOptions()
                        .position(currentPosition)
                        .title(getString(R.string.moto))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
                moveCamera(CameraUpdateFactory.newLatLng(currentPosition))
                animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, MAP_ZOOM))
            }
        }
    }
}
