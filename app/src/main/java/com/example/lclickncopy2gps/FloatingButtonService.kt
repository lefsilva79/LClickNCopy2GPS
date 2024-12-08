package com.example.lclickncopy2gps

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.location.Geocoder
import android.net.Uri
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import kotlin.concurrent.thread

class FloatingButtonService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: View
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupFloatingButton()
    }

    private fun setupFloatingButton() {
        floatingButton = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null)

        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        try {
            windowManager.addView(floatingButton, params)
            floatingButton.findViewById<ImageButton>(R.id.floatingButton).setOnClickListener {
                handleButtonClick()
            }
            setupTouchListener(params)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao criar botão flutuante", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTouchListener(params: WindowManager.LayoutParams) {
        var initialClickTime: Long = 0
        var isClick = true

        floatingButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialClickTime = System.currentTimeMillis()
                    isClick = true
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val moved = Math.abs(event.rawX - initialTouchX) > 10 ||
                            Math.abs(event.rawY - initialTouchY) > 10
                    if (moved) {
                        isClick = false
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        try {
                            windowManager.updateViewLayout(floatingButton, params)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val clickDuration = System.currentTimeMillis() - initialClickTime
                    if (isClick && clickDuration < 200) {
                        floatingButton.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun handleButtonClick() {
        val address = LyftAddressAccessibilityService.detectAddressNow()
        if (address.isNotEmpty()) {
            openWaze(address)
        } else {
            Toast.makeText(this, "Nenhum endereço detectado na tela atual", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWaze(address: String) {
        thread {
            val coordinates = getCoordinatesFromAddress(address)
            if (coordinates != null) {
                openWazeWithCoordinates(coordinates, address)
            } else {
                openWazeWithAddress(address)
            }
        }
    }

    private fun getCoordinatesFromAddress(address: String): LatLng? {
        try {
            val geocoder = Geocoder(this)
            // Aumentando a precisão usando o endereço completo
            val results = geocoder.getFromLocationName(address, 1)
            if (!results.isNullOrEmpty()) {
                val location = results[0]
                // Validar se o número do endereço está correto
                val streetNumber = address.split(" ").firstOrNull()?.toIntOrNull()
                val resultNumber = location.getAddressLine(0)?.split(" ")?.firstOrNull()?.toIntOrNull()

                // Se os números são muito diferentes, retorna null para usar o método de endereço direto
                if (streetNumber != null && resultNumber != null) {
                    if (Math.abs(streetNumber - resultNumber) > 2) {
                        return null
                    }
                }
                return LatLng(location.latitude, location.longitude)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun openWazeWithCoordinates(coordinates: LatLng, address: String) {
        try {
            val wazeUri = "waze://?ll=${coordinates.latitude},${coordinates.longitude}" +
                    "&navigate=yes" +
                    "&zoom=17" +
                    "&accuracy=100" +
                    "&q=${Uri.encode(address)}"  // Adiciona o endereço original como referência
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wazeUri)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            openPlayStore()
        }
    }

    private fun openWazeWithAddress(address: String) {
        try {
            val formattedAddress = address.trim().replace(Regex("\\s+"), " ")
            val wazeUri = "waze://?q=${Uri.encode(formattedAddress)}" +
                    "&navigate=yes" +
                    "&zoom=17" +
                    "&accuracy=100"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wazeUri)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            openPlayStore()
        }
    }

    private fun openPlayStore() {
        val marketUri = "market://details?id=com.waze"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(marketUri)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingButton.isInitialized) {
            windowManager.removeView(floatingButton)
        }
    }
}