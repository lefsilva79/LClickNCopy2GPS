package com.example.lclickncopy2gps

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Color
import android.location.Geocoder
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
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
    private var startTime: Long = 0

    // Novas constantes para os efeitos
    private val ATTRACTION_THRESHOLD = 300
    private val ATTRACTION_FORCE = 0.6f
    private val LONG_PRESS_DURATION = 2000 // 2 segundos em milissegundos

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupFloatingButton()
        setupCloseButton()
    }

    private fun setupFloatingButton() {
        floatingButton = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null)

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
            y = 100
        }

        closeButton.visibility = View.GONE
        windowManager.addView(closeButton, params)

        // Configura a cor inicial do botão de fechar
        closeButton.findViewById<ImageView>(R.id.closeButton)?.let { closeImageView ->
            closeImageView.setColorFilter(Color.GRAY)
        }
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
                    startTime = System.currentTimeMillis()
                    showCloseButton()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(floatingButton, params)

                        if (isNearCloseButton()) {
                            // Aplica efeitos quando próximo
                            applyAttractionForce(params)
                            closeButton.findViewById<ImageView>(R.id.closeButton)?.let { closeImageView ->
                                closeImageView.setColorFilter(Color.RED)
                                closeImageView.scaleX = 1.3f
                                closeImageView.scaleY = 1.3f
                            }
                            closeButton.alpha = 0.7f
                        } else {
                            // Retorna ao normal
                            closeButton.findViewById<ImageView>(R.id.closeButton)?.let { closeImageView ->
                                closeImageView.setColorFilter(Color.GRAY)
                                closeImageView.scaleX = 1.0f
                                closeImageView.scaleY = 1.0f
                            }
                            closeButton.alpha = 1.0f
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    hideCloseButton()
                    val endTime = System.currentTimeMillis()
                    val pressDuration = endTime - startTime

                    val moved = Math.abs(event.rawX - initialTouchX) > 5 ||
                            Math.abs(event.rawY - initialTouchY) > 5

                    if (isNearCloseButton()) {
                        stopSelf()
                    } else if (!moved) {
                        if (pressDuration >= LONG_PRESS_DURATION) {
                            handleLongPress()
                        } else {
                            view.performClick()
                            handleButtonClick()
                        }
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

    private fun handleLongPress() {
        val address = LyftAddressAccessibilityService.detectAddressTopHalf()
        if (address.isNotEmpty()) {
            // Copia o endereço para a área de transferência (mantém essa funcionalidade)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("address", address)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Endereço copiado: $address", Toast.LENGTH_SHORT).show()

            // Executa a mesma ação do clique normal do botão
            openWaze(address)
        } else {
            Toast.makeText(this, "Nenhum endereço detectado na parte superior da tela", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isNearCloseButton(): Boolean {
        if (!::closeButton.isInitialized) return false

        val closeLocation = IntArray(2)
        closeButton.getLocationOnScreen(closeLocation)

        val buttonLocation = IntArray(2)
        floatingButton.getLocationOnScreen(buttonLocation)

        val distance = Math.sqrt(
            Math.pow((closeLocation[0] - buttonLocation[0]).toDouble(), 2.0) +
                    Math.pow((closeLocation[1] - buttonLocation[1]).toDouble(), 2.0)
        ).toFloat()

        return distance < ATTRACTION_THRESHOLD
    }

    private fun applyAttractionForce(params: WindowManager.LayoutParams) {
        val closeLocation = IntArray(2)
        val buttonLocation = IntArray(2)

        closeButton.getLocationOnScreen(closeLocation)
        floatingButton.getLocationOnScreen(buttonLocation)

        val deltaX = closeLocation[0] - buttonLocation[0]
        val deltaY = closeLocation[1] - buttonLocation[1]

        params.x += (deltaX * ATTRACTION_FORCE).toInt()
        params.y += (deltaY * ATTRACTION_FORCE).toInt()

        try {
            windowManager.updateViewLayout(floatingButton, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

            // Reseta o estado do botão de fechar
            closeButton.findViewById<ImageView>(R.id.closeButton)?.let { closeImageView ->
                closeImageView.setColorFilter(Color.GRAY)
                closeImageView.scaleX = 1.0f
                closeImageView.scaleY = 1.0f
            }
        }
    }

    private fun handleButtonClick() {
        val address = LyftAddressAccessibilityService.detectAddressNow()
        if (address.isNotEmpty()) {
            // Copia o endereço para a área de transferência
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("address", address)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Endereço copiado: $address", Toast.LENGTH_SHORT).show()

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
            val results = geocoder.getFromLocationName(address, 1)
            if (!results.isNullOrEmpty()) {
                val location = results[0]
                val streetNumber = address.split(" ").firstOrNull()?.toIntOrNull()
                val resultNumber = location.getAddressLine(0)?.split(" ")?.firstOrNull()?.toIntOrNull()

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
                    "&q=${Uri.encode(address)}"
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