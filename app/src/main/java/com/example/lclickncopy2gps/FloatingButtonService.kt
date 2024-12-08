package com.example.lclickncopy2gps

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast

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
        val inflater = LayoutInflater.from(this)
        floatingButton = inflater.inflate(R.layout.layout_floating_button, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        val button = floatingButton.findViewById<ImageButton>(R.id.floatingButton)
        button.setOnClickListener {
            handleButtonClick()
        }

        setupTouchListener(params)
        windowManager.addView(floatingButton, params)
    }

    private fun setupTouchListener(params: WindowManager.LayoutParams) {
        floatingButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingButton, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun handleButtonClick() {
        val address = LyftAddressAccessibilityService.lastDetectedAddress
        if (address.isNotEmpty()) {
            openWaze(address)
        } else {
            Toast.makeText(this, "Nenhum endereço detectado ainda", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWaze(address: String) {
        try {
            val formattedAddress = address.trim().replace(Regex("\\s+"), " ")
            val wazeUri = "waze://?q=${Uri.encode(formattedAddress)}&navigate=yes"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wazeUri)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            val marketUri = "market://details?id=com.waze"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(marketUri)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingButton.isInitialized) {
            windowManager.removeView(floatingButton)
        }
    }
}