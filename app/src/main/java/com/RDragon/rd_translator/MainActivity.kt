package com.RDragon.rd_translator

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_OVERLAY = 1001
        private const val REQUEST_CAPTURE = 1002
        private const val TAG = "MainActivity"
    }

    private lateinit var adapter: WordsAdapter
    private var projectionData: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        setupButtons()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerWords)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = WordsAdapter().apply {
            submitList(loadIgnoredWords())
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

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                },
                REQUEST_OVERLAY
            )
        } else {
            startScreenCapture()
        }
    }

    private fun startScreenCapture() {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    private fun showAddWordDialog() {
        val input = android.widget.EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Add Word to Ignore")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                input.text.toString().trim().takeIf { it.isNotBlank() }?.let {
                    saveIgnoredWord(it)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveIgnoredWord(word: String) {
        val words = loadIgnoredWords().toMutableSet().apply { add(word) }
        saveToPreferences(words)
        adapter.submitList(words.toList())
    }

    private fun loadIgnoredWords(): List<String> {
        return getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .getStringSet("ignored", mutableSetOf())?.toList() ?: emptyList()
    }

    private fun saveToPreferences(words: Set<String>) {
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().apply {
            putStringSet("ignored", words)
            apply()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_OVERLAY -> handleOverlayResult(resultCode)
            REQUEST_CAPTURE -> handleCaptureResult(resultCode, data)
        }
    }

    private fun handleOverlayResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK && Settings.canDrawOverlays(this)) {
            startScreenCapture()
        }
    }

    private fun handleCaptureResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            projectionData = data
            startService(Intent(this, OverlayService::class.java).apply {
                putExtra("code", resultCode)
                putExtra("data", data)
            })
        }
    }
}