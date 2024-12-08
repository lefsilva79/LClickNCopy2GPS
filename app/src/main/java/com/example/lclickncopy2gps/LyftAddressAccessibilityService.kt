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
                    val formattedAddress = formatAddress(text)
                    if (formattedAddress != lastDetectedAddress) {
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
        // Verifica se começa com números (padrão americano)
        val startsWithNumber = text.trim().matches(Regex("^\\d+.*"))

        // Verifica se tem um comprimento mínimo razoável e contém mais números
        return startsWithNumber && text.length > 5 && text.contains(Regex("\\d+"))
    }

    private fun formatAddress(text: String): String {
        return text
            .trim() // Remove espaços no início e fim
            .replace(Regex("\\s+"), " ") // Substitui múltiplos espaços por um único espaço
            .replace("\n", " ") // Substitui quebras de linha por espaço
            .replace("\t", " ") // Substitui tabs por espaço
    }

    override fun onInterrupt() {
        Log.d(TAG, "Serviço de acessibilidade interrompido")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Serviço de acessibilidade destruído")
    }
}