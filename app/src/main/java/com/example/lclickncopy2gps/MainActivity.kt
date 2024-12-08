package com.example.lclickncopy2gps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.accessibilityservice.AccessibilityServiceInfoCompat // Use este import
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lclickncopy2gps.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startServiceButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
            } else if (!isAccessibilityServiceEnabled()) {
                openAccessibilitySettings()
                Toast.makeText(this, "Por favor, habilite o serviço de acessibilidade", Toast.LENGTH_LONG).show()
            } else {
                startFloatingService()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(1) // 1 é o valor de FEEDBACK_GENERIC
        return enabledServices.any { it.id.contains(packageName) }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
    }

    private fun startFloatingService() {
        val serviceIntent = Intent(this, FloatingButtonService::class.java)
        startService(serviceIntent)
        Toast.makeText(this, "Serviço iniciado", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                if (isAccessibilityServiceEnabled()) {
                    startFloatingService()
                } else {
                    openAccessibilitySettings()
                    Toast.makeText(this, "Por favor, habilite o serviço de acessibilidade", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Permissão necessária para o botão flutuante", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
    }
}