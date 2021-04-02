package com.pm.mototracker.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.pm.mototracker.R
import com.pm.mototracker.common.BaseActivity
import com.pm.mototracker.databinding.ActivityDashboardBinding
import com.pm.mototracker.ui.control.ControlTowerActivity
import com.pm.mototracker.ui.tracker.TrackerActivity
import com.pm.mototracker.util.PreferenceHelper
import com.pm.mototracker.util.PreferenceHelper.get
import com.pm.mototracker.util.PreferenceHelper.set
import kotlin.reflect.KClass

class DashboardActivity : BaseActivity<ActivityDashboardBinding, DashboardViewModel>() {

    private lateinit var prefHelper: SharedPreferences

    override fun viewModelClass(): KClass<DashboardViewModel> = DashboardViewModel::class

    override fun provideBinding(): ActivityDashboardBinding =
        ActivityDashboardBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefHelper = PreferenceHelper.defaultPrefs(this)
        checkPermissions()
    }

    private fun initView() {
        checkPhoneNumberFromStorage()
        with(binding) {
            openControlTower.setOnClickListener {
                navigateToControlTower()
            }
            openTracker.setOnClickListener {
                navigateToTracker()
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                initView()
            }
        }

    private fun checkPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED -> {
                initView()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS) -> {
                Toast.makeText(this, getString(R.string.permission_rational_msg), Toast.LENGTH_LONG)
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            }
        }
    }

    private fun checkPhoneNumberFromStorage() {
        val ctPhoneNumber = prefHelper[PreferenceHelper.CONTROL_TOWER_PHONE_NUMBER, ""]
        val tPhoneNumber = prefHelper[PreferenceHelper.TRACKING_PHONE_NUMBER, ""]

        if (ctPhoneNumber?.isBlank() == false) {
            binding.trackerControlTowerPhoneTi.setText(ctPhoneNumber)
        }

        if (tPhoneNumber?.isBlank() == false) {
            binding.trackerPhoneTi.setText(tPhoneNumber)
        }
    }

    private fun validatePhoneNumber(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) {
            return false
        }
        return true
    }

    private fun navigateToControlTower() {
        val trackerPhoneNumber = binding.trackerPhoneTi.text.toString().trim()

        if (validatePhoneNumber(trackerPhoneNumber)) {
            startActivity(Intent(this, ControlTowerActivity::class.java))
            prefHelper[PreferenceHelper.TRACKING_PHONE_NUMBER] = trackerPhoneNumber

        } else {
            binding.trackerPhoneTl.error = getString(R.string.phone_number_missing)
        }
    }

    private fun navigateToTracker() {
        val controlTowerPhoneNumber = binding.trackerControlTowerPhoneTi.text.toString().trim()

        if (validatePhoneNumber(controlTowerPhoneNumber)) {
            startActivity(Intent(this, TrackerActivity::class.java))
            prefHelper[PreferenceHelper.CONTROL_TOWER_PHONE_NUMBER] = controlTowerPhoneNumber

        } else {
            binding.trackerControlTowerPhoneTl.error = getString(R.string.phone_number_missing)
        }
    }
}