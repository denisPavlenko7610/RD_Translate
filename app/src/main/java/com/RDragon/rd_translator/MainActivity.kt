package com.RDragon.rd_translator

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_OVERLAY = 1001
        private const val REQUEST_CAPTURE = 1002
    }

    private lateinit var adapter: WordsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupRecyclerView()
        setupButtons()
        // Initialize default ignored words
        IgnoreRepository.initDefaults(this)
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerWords)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = WordsAdapter().apply {
            submitList(IgnoreRepository.getIgnored(this@MainActivity).toList())
        }
        recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnStart).setOnClickListener {
            checkOverlayPermission()
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
        }

        findViewById<Button>(R.id.btnAddWord).setOnClickListener {
            showAddWordDialog()
        }
    }

    private fun showAddWordDialog() {
        val input = android.widget.EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Add Word to Ignore")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                input.text.toString().trim().takeIf { it.isNotBlank() }?.let { word ->
                    IgnoreRepository.addIgnored(this, word)
                    adapter.submitList(IgnoreRepository.getIgnored(this).toList())
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_OVERLAY -> if (resultCode == Activity.RESULT_OK && Settings.canDrawOverlays(this)) startScreenCapture()
            REQUEST_CAPTURE -> if (resultCode == Activity.RESULT_OK && data != null) startService(
                Intent(this, OverlayService::class.java).apply {
                    putExtra("code", resultCode)
                    putExtra("data", data)
                }
            )
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                REQUEST_OVERLAY
            )
        } else startScreenCapture()
    }

    private fun startScreenCapture() {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }
}
