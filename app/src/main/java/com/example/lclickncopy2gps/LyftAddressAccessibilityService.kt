package com.example.lclickncopy2gps

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class LyftAddressAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "LyftAccessibilityService"
        var lastDetectedAddress: String = ""
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            packageNames = arrayOf("me.lyft.android") // Package correto do Lyft
            notificationTimeout = 100
        }
        setServiceInfo(info)
        Log.d(TAG, "Serviço de acessibilidade conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    findAddressNodes(rootNode)
                    rootNode.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar evento de acessibilidade", e)
        }
    }

    private fun findAddressNodes(node: AccessibilityNodeInfo) {
        try {
            node.text?.toString()?.let { text ->
                if (isAddress(text)) {
                    lastDetectedAddress = text
                    Log.d(TAG, "Endereço detectado: $lastDetectedAddress")
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    findAddressNodes(child)
                    child.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao procurar nós de endereço", e)
        }
    }

    private fun isAddress(text: String): Boolean {
        return text.length > 10 && (
                text.contains("Rua", ignoreCase = true) ||
                        text.contains("Avenida", ignoreCase = true) ||
                        text.contains("R.", ignoreCase = true) ||
                        text.contains("Av.", ignoreCase = true)
                )
    }

    override fun onInterrupt() {
        Log.d(TAG, "Serviço de acessibilidade interrompido")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Serviço de acessibilidade destruído")
    }
}