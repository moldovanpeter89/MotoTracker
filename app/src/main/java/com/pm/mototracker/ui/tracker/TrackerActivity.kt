package com.pm.mototracker.ui.tracker

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import com.pm.mototracker.common.BaseActivity
import com.pm.mototracker.databinding.ActivityTrackerBinding
import com.pm.mototracker.util.PreferenceHelper
import com.pm.mototracker.util.PreferenceHelper.defaultPrefs
import com.pm.mototracker.util.PreferenceHelper.get
import com.pm.mototracker.util.PreferenceHelper.set
import kotlin.reflect.KClass


class TrackerActivity : BaseActivity<ActivityTrackerBinding, TrackerViewModel>() {

    private lateinit var prefHelper: SharedPreferences

    override fun viewModelClass(): KClass<TrackerViewModel> = TrackerViewModel::class

    override fun provideBinding(): ActivityTrackerBinding =
        ActivityTrackerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefHelper = defaultPrefs(this)
        initView()
    }

    private fun initView() {
        initTrackingSwitch()
    }

    private fun initTrackingSwitch() {
        binding.trackerSwich.isChecked =
            prefHelper[PreferenceHelper.SERVICE_STARTED, false] == true
        binding.trackerSwich.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startTrackingService()

            } else {
                stopTrackingService()
            }
        }
    }

    private fun startTrackingService() {
        prefHelper[PreferenceHelper.SERVICE_STARTED] = true
        Intent(this, TrackingService::class.java).also { intent ->
            startService(intent)
        }
    }

    private fun stopTrackingService() {
        prefHelper[PreferenceHelper.SERVICE_STARTED] = false
        Intent(this, TrackingService::class.java).also { intent ->
            stopService(intent)
        }
    }
}