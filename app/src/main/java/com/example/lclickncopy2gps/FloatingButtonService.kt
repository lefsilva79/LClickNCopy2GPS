package com.example.lclickncopy2gps

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
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
    private lateinit var closeButton: View
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var isCloseBtnShowing = false

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupFloatingButton()
        setupCloseButton()
    }

    private fun setupFloatingButton() {
        // Infla o layout
        floatingButton = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null)

        // Configura os parâmetros da janela
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        // Adiciona a view e configura o listener
        windowManager.addView(floatingButton, params)
        setupTouchListener(params)
    }

    private fun setupCloseButton() {
        closeButton = LayoutInflater.from(this).inflate(R.layout.layout_close_button, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100 // Distância do fundo da tela
        }

        closeButton.visibility = View.GONE
        windowManager.addView(closeButton, params)
    }

    private fun setupTouchListener(params: WindowManager.LayoutParams) {
        val button = floatingButton.findViewById<ImageButton>(R.id.floatingButton)

        button.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    showCloseButton()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(floatingButton, params)

                        if (isNearCloseButton()) {
                            closeButton.alpha = 0.7f
                        } else {
                            closeButton.alpha = 1.0f
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    hideCloseButton()
                    val moved = Math.abs(event.rawX - initialTouchX) > 5 ||
                            Math.abs(event.rawY - initialTouchY) > 5

                    if (isNearCloseButton()) {
                        stopSelf()
                    } else if (!moved) {
                        view.performClick()
                        handleButtonClick()
                    }
                    true
                }
                else -> false
            }
        }

        button.setOnClickListener {
            handleButtonClick()
        }
    }

    private fun isNearCloseButton(): Boolean {
        if (!::closeButton.isInitialized) return false

        val closeLocation = IntArray(2)
        closeButton.getLocationOnScreen(closeLocation)

        val buttonLocation = IntArray(2)
        floatingButton.getLocationOnScreen(buttonLocation)

        val closeRect = Rect(
            closeLocation[0],
            closeLocation[1],
            closeLocation[0] + closeButton.width,
            closeLocation[1] + closeButton.height
        )

        return closeRect.contains(
            buttonLocation[0] + floatingButton.width/2,
            buttonLocation[1] + floatingButton.height/2
        )
    }

    private fun showCloseButton() {
        if (!isCloseBtnShowing && ::closeButton.isInitialized) {
            closeButton.visibility = View.VISIBLE
            isCloseBtnShowing = true
        }
    }

    private fun hideCloseButton() {
        if (isCloseBtnShowing && ::closeButton.isInitialized) {
            closeButton.visibility = View.GONE
            isCloseBtnShowing = false
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
        if (::closeButton.isInitialized) {
            windowManager.removeView(closeButton)
        }
    }
}