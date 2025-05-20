package com.RDragon.rd_translator

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_ID = 101
        private const val CAPTURE_INTERVAL = 60000L
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var textView: TextView
    private lateinit var ignoreButton: Button
    private lateinit var handler: Handler
    private lateinit var prefs: SharedPreferences

    private var mediaProjection: MediaProjection? = null
    private var lastCaptureTime = 0L
    private var isProcessing = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.w(TAG, "MediaProjection system stop")
            terminateService()
        }
    }

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        prefs = getSharedPreferences("OCR_Prefs", Context.MODE_PRIVATE)
        setupNotification()
        setupOverlay()
    }

    private fun setupNotification() {
        val channel = NotificationChannel(
            "TRANSLATOR_CHANNEL",
            "Translation Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background translation service"
        }

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
            createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "TRANSLATOR_CHANNEL")
            .setContentTitle("Translation Active")
            .setSmallIcon(R.drawable.ic_translation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        textView = overlayView.findViewById(R.id.overlayTextView)
        ignoreButton = overlayView.findViewById(R.id.ignoreButton)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        windowManager.addView(overlayView, params)
        overlayView.visibility = View.GONE

        ignoreButton.setOnClickListener {
            textView.text.toString().substringBefore(":").trim().let { word ->
                if (word.isNotEmpty()) saveIgnoredWord(word)
            }
            hideOverlay()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.hasExtra("code") && it.hasExtra("data")) {
                handleNewProjection(
                    it.getIntExtra("code", Activity.RESULT_CANCELED),
                    it.getParcelableExtra("data")!!
                )
            }
        }
        return START_STICKY
    }

    private fun handleNewProjection(code: Int, data: Intent) {
        cleanupProjection()

        mediaProjection = (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
            .getMediaProjection(code, data)
            .apply {
                registerCallback(projectionCallback, handler)
            }

        startCaptureCycle()
    }

    private fun startCaptureCycle() {
        if (isProcessing) return
        isProcessing = true
        scheduleNextCapture()
    }

    private fun scheduleNextCapture() {
        val delay = calculateDelay()
        handler.postDelayed(::executeCapture, delay)
    }

    private fun calculateDelay(): Long {
        return if (lastCaptureTime == 0L) 0L
        else maxOf(CAPTURE_INTERVAL - (SystemClock.elapsedRealtime() - lastCaptureTime), 0)
    }

    private fun executeCapture() {
        // Вместо проверки isStopped, проверяем наличие mediaProjection
        val proj = mediaProjection
        if (proj == null) {
            terminateService()
            return
        }
        OCRProcessor.capture(proj) { words ->
            processResults(words)
            lastCaptureTime = SystemClock.elapsedRealtime()
            scheduleNextCapture()
        }
    }

    private fun processResults(words: List<String>) {
        val filtered = words.filterNot { it in getIgnoredWords() }
        filtered.randomOrNull()?.let(::showTranslation) ?: hideOverlay()
    }

    private fun showTranslation(word: String) {
        TranslationManager.translate(word) { translation ->
            handler.post {
                textView.text = "$word: $translation"
                overlayView.visibility = View.VISIBLE
                handler.postDelayed(::hideOverlay, 5000)
            }
        }
    }

    private fun hideOverlay() {
        overlayView.visibility = View.GONE
    }

    private fun getIgnoredWords(): Set<String> {
        return prefs.getStringSet("ignored", mutableSetOf()) ?: emptySet()
    }

    private fun saveIgnoredWord(word: String) {
        prefs.edit().apply {
            putStringSet("ignored", getIgnoredWords().toMutableSet().apply { add(word) })
            apply()
        }
    }

    private fun cleanupProjection() {
        mediaProjection?.let {
            it.unregisterCallback(projectionCallback)
            it.stop()
            mediaProjection = null
        }
        OCRProcessor.cleanup()
    }

    private fun terminateService() {
        stopSelf()
    }

    override fun onDestroy() {
        isProcessing = false
        handler.removeCallbacksAndMessages(null)
        cleanupProjection()
        windowManager.removeView(overlayView)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
