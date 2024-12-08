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
        // Inflatar o layout
        floatingButton = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null)

        // Configurar os parâmetros da janela
        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = 100  // posição inicial X
            y = 200  // posição inicial Y
        }

        try {
            // Adicionar o botão à janela
            windowManager.addView(floatingButton, params)

            // Configurar o clique do botão
            floatingButton.findViewById<ImageButton>(R.id.floatingButton).setOnClickListener {
                handleButtonClick()
            }

            // Configurar o touch listener para arrastar
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