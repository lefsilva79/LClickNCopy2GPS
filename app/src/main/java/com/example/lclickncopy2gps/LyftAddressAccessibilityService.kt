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

        // Novo método para detectar endereço na parte superior
        fun detectAddressTopHalf(): String {
            return instance?.findAddressInTopHalf() ?: ""
        }
    }

    private var screenHeight: Int = 0
    private var screenWidth: Int = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Serviço de acessibilidade conectado")

        val displayMetrics = resources.displayMetrics
        screenHeight = displayMetrics.heightPixels
        screenWidth = displayMetrics.widthPixels
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

    // Novo método para encontrar endereço na parte superior
    private fun findAddressInTopHalf(): String {
        lastDetectedAddress = ""  // Reset do último endereço
        try {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                findAddressNodesInTopHalf(rootNode)
                rootNode.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar endereço na parte superior", e)
        }
        return lastDetectedAddress
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Agora vazio pois a detecção é feita apenas no clique do botão
    }

    private fun findAddressNodes(node: AccessibilityNodeInfo) {
        try {
            // Obtém as coordenadas na tela do nó atual
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)

            // Só processa se o nó estiver na metade inferior da tela
            if (rect.top >= screenHeight / 2) {
                node.text?.toString()?.let { text ->
                    if (isAddress(text)) {
                        val formattedAddress = formatAddress(text)
                        if (formattedAddress.isNotEmpty()) {
                            lastDetectedAddress = formattedAddress
                            Log.d(TAG, "Endereço detectado e formatado: $lastDetectedAddress")
                        }
                    }
                }
            }

            // Continue procurando nos nós filhos
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

    // Novo método para procurar endereços na parte superior
    private fun findAddressNodesInTopHalf(node: AccessibilityNodeInfo) {
        try {
            // Obtém as coordenadas na tela do nó atual
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)

            // Só processa se o nó estiver na metade superior da tela
            if (rect.top < screenHeight / 2) {
                node.text?.toString()?.let { text ->
                    if (isAddress(text)) {
                        val formattedAddress = formatAddress(text)
                        if (formattedAddress.isNotEmpty()) {
                            lastDetectedAddress = formattedAddress
                            Log.d(TAG, "Endereço detectado na parte superior: $lastDetectedAddress")
                        }
                    }
                }
            }

            // Continue procurando nos nós filhos
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    findAddressNodesInTopHalf(child)
                    child.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao procurar nós de endereço na parte superior", e)
        }
    }

    private fun isAddress(text: String): Boolean {
        // Primeiro, formata o texto para juntar possíveis múltiplas linhas
        val formattedText = formatAddress(text)

        // Verifica se começa com número
        if (!formattedText.matches(Regex("^\\d+.*"))) {
            return false
        }

        // Verifica comprimento mínimo
        if (formattedText.length < 5) {
            return false
        }

        // Ignora textos muito longos (provavelmente são descrições)
        if (formattedText.length > 100) {
            return false
        }

        // Verifica se contém palavras-chave típicas de endereços dos EUA
        val hasAddressKeywords = formattedText.contains(Regex("\\b(St|Ave|Rd|Blvd|Dr|Ln|Way|IL|Chicago)\\b", RegexOption.IGNORE_CASE))

        // Ignora se contém palavras-chave típicas de conteúdo não-endereço
        val hasNonAddressKeywords = formattedText.contains(Regex("\\b(view|hotel|family|reviews|bubbles)\\b", RegexOption.IGNORE_CASE))

        return hasAddressKeywords && !hasNonAddressKeywords
    }

    private fun formatAddress(text: String): String {
        return text
            .trim()
            .replace(Regex("\\s+"), " ")  // Remove espaços extras
            .replace("\t", " ")           // Remove tabulações
            .split("\n")                  // Divide por quebras de linha
            .map { it.trim() }           // Remove espaços no início e fim de cada linha
            .filter { it.isNotEmpty() }  // Remove linhas vazias
            .joinToString(", ")          // Junta as linhas com vírgula
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