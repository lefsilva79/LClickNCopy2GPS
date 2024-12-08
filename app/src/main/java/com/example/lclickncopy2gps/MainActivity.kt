package com.example.lclickncopy2gps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lclickncopy2gps.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Se todas as permissões já estiverem concedidas, inicia o serviço e fecha o app
        if (isAccessibilityServiceEnabled() && Settings.canDrawOverlays(this)) {
            startFloatingService()
            finish()
            return
        }

        // Se não, mostra a tela de configuração
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updatePermissionStatus()

        binding.accessibilityButton.setOnClickListener {
            openAccessibilitySettings()
        }

        binding.locationButton.setOnClickListener {
            requestOverlayPermission()
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        val serviceName = "$packageName/.LyftAddressAccessibilityService"
        return enabledServices.any { it.id.contains(serviceName) }
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this,
                "Por favor, ative o serviço 'LClickNCopy2GPS' nas configurações de acessibilidade",
                Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao abrir configurações de acessibilidade", Toast.LENGTH_LONG).show()
        }
    }

    private fun startFloatingService() {
        try {
            val serviceIntent = Intent(this, FloatingButtonService::class.java)
            startService(serviceIntent)
            Toast.makeText(this, "Serviço iniciado", Toast.LENGTH_SHORT).show()
            // Fecha o app após iniciar o serviço com sucesso
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao iniciar o serviço: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
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

    private fun updatePermissionStatus() {
        binding.accessibilityCheck.isChecked = isAccessibilityServiceEnabled()
        binding.locationCheck.isChecked = Settings.canDrawOverlays(this)

        // Se todas as permissões estiverem ok, inicia o serviço e fecha o app
        if (isAccessibilityServiceEnabled() && Settings.canDrawOverlays(this)) {
            startFloatingService()
        }
    }

    override fun onResume() {
        super.onResume()
        // Verifica se pode fechar o app assim que voltar para ele
        if (isAccessibilityServiceEnabled() && Settings.canDrawOverlays(this)) {
            startFloatingService()
        } else {
            updatePermissionStatus()
        }
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
    }
}