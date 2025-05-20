package com.RDragon.rd_translator

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    private lateinit var handler: Handler
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var textView: TextView
    private lateinit var ignoreButton: Button
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var lastCaptureTime = 0L
    private var isProcessing = false

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIF_ID = 101
        private const val CHANNEL_ID = "TRANSLATOR_CHANNEL"
        private const val CAPTURE_INTERVAL = 60000L
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() = stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        setupNotification()
        setupOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.extras?.let { extras ->
            val code = extras.getInt("code", Activity.RESULT_CANCELED)
            val data = extras.getParcelable<Intent>("data")
            if (data != null && code == Activity.RESULT_OK) setupProjection(code, data)
        }
        return START_STICKY
    }

    private fun setupNotification() {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Translation Service", NotificationManager.IMPORTANCE_LOW)
        )
        val note = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Translation Active")
            .setSmallIcon(R.drawable.ic_translation)
            .build()
        startForeground(NOTIF_ID, note)
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
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START; x = 0; y = 100
        }
        windowManager.addView(overlayView, params)
        overlayView.visibility = View.GONE
        ignoreButton.setOnClickListener {
            val word = textView.text.split(':')[0].trim().lowercase()
            IgnoreRepository.addIgnored(this, word)
            overlayView.visibility = View.GONE
        }
    }

    private fun setupProjection(code: Int, data: Intent) {
        cleanupProjection()
        mediaProjection = (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
            .getMediaProjection(code, data)
            .apply { registerCallback(projectionCallback, handler) }

        val metrics = Resources.getSystem().displayMetrics
        imageReader = ImageReader.newInstance(
            metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2
        )
        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "OCR_Display",
            metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null
        )
        startCaptureCycle()
    }

    private fun startCaptureCycle() {
        if (isProcessing) return
        isProcessing = true
        scheduleNextCapture()
    }

    private fun scheduleNextCapture() {
        val delay = if (lastCaptureTime == 0L) 0L else maxOf(CAPTURE_INTERVAL - (SystemClock.elapsedRealtime() - lastCaptureTime), 0)
        handler.postDelayed({ captureOnce() }, delay)
    }

    private fun captureOnce() {
        imageReader?.acquireLatestImage()?.let { image ->
            OCRProcessor.processImage(image) { words ->
                val filtered = words.filterNot { it in IgnoreRepository.getIgnored(this) }
                filtered.shuffled().firstOrNull()?.let { word ->
                    TranslationManager.translate(word) { translation ->
                        if (translation == "[error]" || translation.isBlank()) {
                            IgnoreRepository.addIgnored(this, word)
                            captureOnce()
                        } else {
                            handler.post {
                                textView.text = "$word: $translation"
                                overlayView.visibility = View.VISIBLE
                                handler.postDelayed({ overlayView.visibility = View.GONE }, 5000)
                            }
                            lastCaptureTime = SystemClock.elapsedRealtime()
                            scheduleNextCapture()
                        }
                    }
                } ?: scheduleNextCapture()
            }
        } ?: scheduleNextCapture()
    }

    private fun cleanupProjection() {
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
        virtualDisplay?.release()
        imageReader?.close()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        cleanupProjection()
        windowManager.removeView(overlayView)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}