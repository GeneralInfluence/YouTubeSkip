package com.adskipper

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import com.adskipper.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val enabled = isServiceEnabled()
        binding.statusIndicator.setBackgroundResource(
            if (enabled) R.drawable.circle_green else R.drawable.circle_red
        )
        binding.tvStatus.text = if (enabled) {
            getString(R.string.status_active)
        } else {
            getString(R.string.status_inactive)
        }
        binding.tvInstructions.text = if (enabled) {
            getString(R.string.instructions_active)
        } else {
            getString(R.string.instructions_inactive)
        }
        binding.btnOpenSettings.text = if (enabled) {
            getString(R.string.btn_manage)
        } else {
            getString(R.string.btn_enable)
        }
    }

    private fun isServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName &&
            it.resolveInfo.serviceInfo.name == AdSkipperService::class.java.name
        }
    }
}
