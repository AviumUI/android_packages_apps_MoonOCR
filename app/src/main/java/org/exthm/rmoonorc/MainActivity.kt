package org.exthm.rmoonorc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import org.exthm.rmoonorc.service.OcrService
import org.exthm.rmoonorc.utils.Constants

class MainActivity : ComponentActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissionsAndStart()
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val serviceIntent = Intent(this, OcrService::class.java).apply {
                action = Constants.ACTION_START_SERVICE
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            startForegroundService(serviceIntent)
        }
        finish()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestScreenCapture()
        } else {
            requestScreenCapture()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
            return
        }

        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun requestScreenCapture() {
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}