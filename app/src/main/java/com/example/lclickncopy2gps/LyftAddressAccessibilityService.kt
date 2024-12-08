package com.example.lclickncopy2gps

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class LyftAddressAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "LyftAccessibilityService"
        private var instance: LyftAddressAccessibilityService? = null
        var lastDetectedAddress: String = ""
            private set

        fun detectAddressNow(): String {
            return instance?.findCurrentAddress() ?: ""
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Serviço de acessibilidade conectado")
    }

    private fun findCurrentAddress(): String {
        lastDetectedAddress = ""  // Reset do último endereço
        try {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                findAddressNodes(rootNode)
                rootNode.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar endereço", e)
        }
        return lastDetectedAddress
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Agora vazio pois a detecção é feita apenas no clique do botão
    }

    private fun findAddressNodes(node: AccessibilityNodeInfo) {
        try {
            node.text?.toString()?.let { text ->
                if (isAddress(text)) {
                    val formattedAddress = formatAddress(text)
                    if (formattedAddress.isNotEmpty()) {
                        lastDetectedAddress = formattedAddress
                        Log.d(TAG, "Endereço detectado e formatado: $lastDetectedAddress")
                    }
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
        val cleanText = text.trim().replace(Regex("\\s+"), " ")
        return cleanText.matches(Regex("^\\d+.*,.*")) &&
                cleanText.length > 5 &&
                cleanText.contains(",")
    }

    private fun formatAddress(text: String): String {
        return text
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace("\n", " ")
            .replace("\t", " ")
    }

    override fun onInterrupt() {
        Log.d(TAG, "Serviço de acessibilidade interrompido")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "Serviço de acessibilidade destruído")
    }
}