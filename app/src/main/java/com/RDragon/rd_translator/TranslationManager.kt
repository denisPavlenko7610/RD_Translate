package com.RDragon.rd_translator

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TranslationManager {
    private const val TAG = "TranslationManager"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun translate(word: String, callback: (String) -> Unit) {
        Log.d(TAG, "Translating word: \"$word\"")
        Thread {
            try {
                val result = performTranslation(word)
                mainHandler.post { callback(result) }
            } catch (e: Exception) {
                Log.e(TAG, "Translation failed", e)
                mainHandler.post { callback("[error]") }
            }
        }.start()
    }

    private fun performTranslation(word: String): String {
        val q = URLEncoder.encode(word, "UTF-8")
        val urlStr = "https://api.mymemory.translated.net/get?q=$q&langpair=en|ru"

        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        conn.connect()

        return if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            parseTranslationResult(response)
        } else {
            Log.e(TAG, "Non-OK response code: ${conn.responseCode}")
            "[error]"
        }
    }

    private fun parseTranslationResult(json: String): String {
        return try {
            val obj = JSONObject(json)
            obj.getJSONObject("responseData").getString("translatedText")
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing failed", e)
            "[error]"
        }
    }
}
