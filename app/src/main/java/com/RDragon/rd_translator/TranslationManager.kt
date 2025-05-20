package com.RDragon.rd_translator

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
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
        val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=ru&dt=t&q=$q"

        return URL(urlStr).openConnection().run {
            this as HttpURLConnection
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("User-Agent", "Mozilla/5.0")
            connect()

            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    inputStream.bufferedReader().use { reader ->
                        parseTranslationResult(reader.readText())
                    }
                }
                else -> {
                    Log.e(TAG, "Non-OK response code: $responseCode")
                    "[error]"
                }
            }
        }
    }

    private fun parseTranslationResult(json: String): String {
        return try {
            JSONArray(json).getJSONArray(0).let { arr ->
                StringBuilder().apply {
                    for (i in 0 until arr.length()) {
                        append(arr.getJSONArray(i).getString(0))
                    }
                }.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing failed", e)
            "[error]"
        }
    }
}