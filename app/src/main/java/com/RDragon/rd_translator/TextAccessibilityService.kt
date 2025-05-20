package com.RDragon.rd_translator

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import android.os.Build

class TextAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "TextAccessibilitySvc"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val knownWords = mutableSetOf("you", "i", "hello") // храните в SharedPrefs

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) return

        val rootNode = rootInActiveWindow ?: return
        val textList = mutableListOf<String>()
        collectText(rootNode, textList)
        val fullText = textList.joinToString(" ").trim()

        if (fullText.isBlank()) return

        // фильтруем уже известные слова
        val filtered = fullText.split(Regex("\\W+"))
            .map { it.lowercase() }
            .filter { it.isNotBlank() && !knownWords.contains(it) }
            .joinToString(" ")

        if (filtered.isNotBlank()) {
            Log.d(TAG, "Filtered text to translate: \"$filtered\"")
            translateAndShow(filtered)
        }
    }

    private fun collectText(node: AccessibilityNodeInfo, texts: MutableList<String>) {
        node.text?.let { texts.add(it.toString()) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectText(it, texts) }
        }
    }

    private fun translateAndShow(text: String) {
        coroutineScope.launch {
            try {
                val translated = translateText(text)
                if (translated != null) {
                    Log.d(TAG, "Translation ready, launching OverlayService: \"$translated\"")
                    val intent = Intent(this@TextAccessibilityService, OverlayService::class.java).apply {
                        putExtra("text", translated)
                    }
                    // На Android O+ надо стартовать foreground-сервис
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Log.d(TAG, "Calling startForegroundService()")
                        startForegroundService(intent)
                    } else {
                        Log.d(TAG, "Calling startService()")
                        startService(intent)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Translation failed", e)
            }
        }
    }

    private suspend fun translateText(text: String): String? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=ru&dt=t&q=$encoded"
            Log.d(TAG, "Fetching URL: $urlStr")
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()
            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "Got response: $resp")
            val arr = JSONArray(resp).getJSONArray(0)
            val result = StringBuilder()
            for (i in 0 until arr.length()) {
                result.append(arr.getJSONArray(i).getString(0))
            }
            result.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error in translateText()", e)
            null
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }
}
