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
import java.net.URLEncoder

class FloatingButtonService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: ImageButton
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingButton = (LayoutInflater.from(this)
            .inflate(R.layout.floating_button, null) as ImageButton).apply {
            setOnTouchListener(getTouchListener())
            setOnClickListener {
                val address = LyftAddressAccessibilityService.lastDetectedAddress
                if (address.isNotEmpty()) {
                    openAddressInWaze(address)
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        try {
            windowManager.addView(floatingButton, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getTouchListener(): View.OnTouchListener = View.OnTouchListener { view, event ->
        val params = view.layoutParams as WindowManager.LayoutParams

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
                windowManager.updateViewLayout(view, params)
                true
            }
            MotionEvent.ACTION_UP -> {
                val moved = Math.abs(event.rawX - initialTouchX) > 10 ||
                        Math.abs(event.rawY - initialTouchY) > 10
                !moved // Se não moveu, permite o click
            }
            else -> false
        }
    }

    private fun openAddressInWaze(address: String) {
        try {
            val encodedAddress = URLEncoder.encode(address, "UTF-8")
            val wazeUri = "https://waze.com/ul?q=$encodedAddress"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wazeUri)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingButton.isInitialized && ::windowManager.isInitialized) {
            try {
                windowManager.removeView(floatingButton)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}